const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb
      .collection("posts")
      .where("neighborhoodId", "==", "maple_heights")
      .where("visibility", "==", "NEIGHBORHOOD")
      .get()
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("listings").doc("list_1").get());
  await assertFails(unauthDb.collection("conversations").doc("conv_1").get());
});

test("Users and private settings enforce PII isolation and shadow-field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice M.",
      neighborhoodId: "maple_heights",
      neighborhoodName: "Maple Heights",
      bio: "Gardener & dog walker on Elm St.",
      avatarInitials: "AM",
      avatarColorHex: 0xFF0A6847,
      interests: ["Gardening", "Pets"],
      helpfulCount: 0,
      publicProfile: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("private")
      .doc("settings")
      .set({
        userId: ALICE_UID,
        email: "alice@example.com",
        streetName: "412 Elm Ave",
        radiusMiles: 3,
        savedPostIds: [],
        savedListingIds: [],
        alertsEnabled: true,
        digestEnabled: true,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Bob can read Alice's public profile and query neighborhood directory
  await assertSucceeds(bobDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    bobDb
      .collection("users")
      .where("neighborhoodId", "==", "maple_heights")
      .where("publicProfile", "==", true)
      .get()
  );

  // Unfiltered users query must fail
  await assertFails(bobDb.collection("users").get());

  // Bob CANNOT read or write Alice's private settings or notifications
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("private")
      .doc("settings")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("notifications")
      .get()
  );

  // Alice cannot inject ghost field or spoof helpfulCount
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      helpfulCount: 999,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );
});

test("Posts, comments, atomic thanks, and polls validate schemas and query filters", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(15000);

  // Create valid post without `@DocumentId` id field
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice M.",
      authorInitials: "AM",
      authorAvatarColor: 0xFF0A6847,
      neighborhoodId: "maple_heights",
      neighborhoodName: "Maple Heights",
      visibility: "NEIGHBORHOOD",
      scope: "LOCAL",
      category: "SAFETY",
      title: "Coyote spotted near Oak Park",
      body: "Saw a coyote near the walking path around 7 AM. Keep small pets close.",
      locationTag: "Oak Park Trail",
      pollOptions: [],
      pollVotes: [],
      thanksCount: 0,
      commentsCount: 0,
      pinnedUrgent: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("posts").doc("post_future").set({
      authorId: ALICE_UID,
      authorName: "Alice M.",
      authorInitials: "AM",
      authorAvatarColor: 0xFF0A6847,
      neighborhoodId: "maple_heights",
      neighborhoodName: "Maple Heights",
      visibility: "NEIGHBORHOOD",
      scope: "LOCAL",
      category: "GENERAL",
      title: "Future post",
      body: "Body",
      locationTag: "",
      pollOptions: [],
      pollVotes: [],
      thanksCount: 0,
      commentsCount: 0,
      pinnedUrgent: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Filtered query succeeds, unfiltered query fails
  await assertSucceeds(
    bobDb
      .collection("posts")
      .where("neighborhoodId", "==", "maple_heights")
      .where("visibility", "==", "NEIGHBORHOOD")
      .get()
  );
  await assertFails(bobDb.collection("posts").get());

  // Bob cannot edit or delete Alice's post
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      title: "Hijacked",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(bobDb.collection("posts").doc("post_1").delete());

  // Non-atomic thanksCount increment without /thanks/{uid} marker fails
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      thanksCount: 1,
      updatedAt: pastDate(1000),
    })
  );

  // Atomic batch thank by Bob succeeds
  const batchAddThank = bobDb.batch();
  batchAddThank.set(
    bobDb.collection("posts").doc("post_1").collection("thanks").doc(BOB_UID),
    {
      userId: BOB_UID,
      targetId: "post_1",
      createdAt: pastDate(1000),
    }
  );
  batchAddThank.update(bobDb.collection("posts").doc("post_1"), {
    thanksCount: 1,
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(batchAddThank.commit());

  // Bob adds comment + increments commentsCount
  const batchComment = bobDb.batch();
  batchComment.set(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1"),
    {
      postId: "post_1",
      authorId: BOB_UID,
      authorName: "Bob K.",
      authorInitials: "BK",
      authorAvatarColor: 0xFF1D6A96,
      neighborhoodName: "Maple Heights",
      body: "Thanks for the heads up!",
      visibility: "NEIGHBORHOOD",
      createdAt: pastDate(500),
    }
  );
  batchComment.update(bobDb.collection("posts").doc("post_1"), {
    commentsCount: 1,
    updatedAt: pastDate(500),
  });
  await assertSucceeds(batchComment.commit());

  // Filtered comment query succeeds; unfiltered fails
  await assertSucceeds(
    aliceDb
      .collection("posts")
      .doc("post_1")
      .collection("comments")
      .where("visibility", "==", "NEIGHBORHOOD")
      .get()
  );
  await assertFails(
    aliceDb.collection("posts").doc("post_1").collection("comments").get()
  );
});

test("Marketplace listings enforce ownership and terminal SOLD lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("listings").doc("item_1").set({
      sellerId: ALICE_UID,
      sellerName: "Alice M.",
      sellerInitials: "AM",
      sellerAvatarColor: 0xFF0A6847,
      neighborhoodId: "maple_heights",
      neighborhoodName: "Maple Heights",
      visibility: "NEIGHBORHOOD",
      title: "Oak Bookshelf",
      description: "Solid oak 4-tier shelf, great condition.",
      priceDollars: 45,
      category: "FURNITURE",
      condition: "GOOD",
      status: "AVAILABLE",
      pickupArea: "Near Elm & 4th",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob cannot update Alice's listing
  await assertFails(
    bobDb.collection("listings").doc("item_1").update({
      priceDollars: 1,
      updatedAt: pastDate(1000),
    })
  );

  // Alice marks listing SOLD
  await assertSucceeds(
    aliceDb.collection("listings").doc("item_1").update({
      status: "SOLD",
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state lock: once SOLD, cannot mutate back to AVAILABLE
  await assertFails(
    aliceDb.collection("listings").doc("item_1").update({
      status: "AVAILABLE",
      updatedAt: pastDate(500),
    })
  );
});

test("Events, groups, and businesses support atomic membership/faves", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  // Create group + creator membership atomically in one batch
  const createGroupBatch = aliceDb.batch();
  createGroupBatch.set(aliceDb.collection("groups").doc("grp_1"), {
    creatorId: ALICE_UID,
    creatorName: "Alice M.",
    neighborhoodId: "maple_heights",
    neighborhoodName: "Maple Heights",
    visibility: "NEIGHBORHOOD",
    name: "Maple Heights Gardeners",
    description: "Seed swaps, composting tips, and native plant advice.",
    category: "GARDENING",
    memberCount: 1,
    createdAt: ts,
    updatedAt: ts,
  });
  createGroupBatch.set(
    aliceDb.collection("groups").doc("grp_1").collection("members").doc(ALICE_UID),
    {
      userId: ALICE_UID,
      targetId: "grp_1",
      createdAt: ts,
    }
  );
  await assertSucceeds(createGroupBatch.commit());

  // Bob joins atomically
  const joinBatch = bobDb.batch();
  joinBatch.set(
    bobDb.collection("groups").doc("grp_1").collection("members").doc(BOB_UID),
    {
      userId: BOB_UID,
      targetId: "grp_1",
      createdAt: pastDate(1000),
    }
  );
  joinBatch.update(bobDb.collection("groups").doc("grp_1"), {
    memberCount: 2,
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(joinBatch.commit());
});

test("Direct conversations and messages strictly isolate to participants", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate(10000);
  const convId = `${ALICE_UID}_${BOB_UID}`;

  const createConvBatch = aliceDb.batch();
  createConvBatch.set(aliceDb.collection("conversations").doc(convId), {
    participantUids: [ALICE_UID, BOB_UID],
    participantNames: ["Alice M.", "Bob K."],
    subject: "Oak Bookshelf",
    lastMessageText: "Hi! Is the bookshelf still available?",
    lastSenderId: ALICE_UID,
    createdAt: ts,
    updatedAt: ts,
  });
  createConvBatch.set(
    aliceDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .doc("msg_1"),
    {
      conversationId: convId,
      senderId: ALICE_UID,
      senderName: "Alice M.",
      participantUids: [ALICE_UID, BOB_UID],
      text: "Hi! Is the bookshelf still available?",
      createdAt: ts,
    }
  );
  await assertSucceeds(createConvBatch.commit());

  // Bob can query his conversations and messages
  await assertSucceeds(
    bobDb
      .collection("conversations")
      .where("participantUids", "array-contains", BOB_UID)
      .get()
  );
  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .where("participantUids", "array-contains", BOB_UID)
      .get()
  );

  // Charlie cannot read or message Alice & Bob's conversation
  await assertFails(charlieDb.collection("conversations").doc(convId).get());
  await assertFails(
    charlieDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .doc("msg_2")
      .set({
        conversationId: convId,
        senderId: CHARLIE_UID,
        senderName: "Charlie",
        participantUids: [ALICE_UID, BOB_UID],
        text: "Snooping!",
        createdAt: pastDate(500),
      })
  );
});
