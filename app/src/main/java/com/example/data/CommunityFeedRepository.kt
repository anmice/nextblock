package com.example.data

import com.example.model.BusinessCategory
import com.example.model.EventCategory
import com.example.model.GroupCategory
import com.example.model.ListingCategory
import com.example.model.ListingCondition
import com.example.model.ListingStatus
import com.example.model.LocalBusiness
import com.example.model.MarketplaceListing
import com.example.model.NeighborProfile
import com.example.model.NeighborhoodEvent
import com.example.model.NeighborhoodGroup
import com.example.model.NeighborhoodPost
import com.example.model.PostCategory
import com.example.model.PostComment
import com.example.model.PostScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CommunityFeedRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "User must be signed in with Google." }
        return uid
    }

    // --- Neighborhood Feed Posts ---

    fun observePosts(neighborhoodId: String): Flow<List<NeighborhoodPost>> = flow {
        if (neighborhoodId.isBlank() || auth.currentUser?.uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("posts")
                .whereEqualTo("neighborhoodId", neighborhoodId)
                .whereEqualTo("visibility", "NEIGHBORHOOD")
                .snapshots()
                .map { snap ->
                    snap.toObjects(NeighborhoodPost::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser != null) {
                        handleFirestoreError(toLog, OperationType.LIST, "posts")
                    }
                    throw error
                }
        )
    }

    fun observeComments(postId: String): Flow<List<PostComment>> = flow {
        if (postId.isBlank() || auth.currentUser?.uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("visibility", "NEIGHBORHOOD")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostComment::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser != null) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeUserThankedPost(postId: String, userId: String): Flow<Boolean> = flow {
        if (postId.isBlank() || userId.isBlank()) {
            emit(false)
            return@flow
        }
        val path = "posts/$postId/thanks/$userId"
        emitAll(
            db.collection("posts").document(postId).collection("thanks").document(userId)
                .snapshots()
                .map { it.exists() }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createPost(
        author: NeighborProfile,
        category: PostCategory,
        scope: PostScope,
        title: String,
        body: String,
        locationTag: String,
        photoUri: String?,
        pollOptions: List<String>,
        pinnedUrgent: Boolean
    ): Result<String> {
        var docPath = "posts"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(140)
            val cleanBody = body.trim().take(2000)
            require(cleanTitle.isNotEmpty() && cleanBody.isNotEmpty()) { "Title and message are required." }
            val cleanPoll = if (category == PostCategory.POLL) {
                pollOptions.map { it.trim().take(80) }.filter { it.isNotEmpty() }.take(4)
            } else emptyList()
            val cleanVotes = List(cleanPoll.size) { 0L }

            val docRef = db.collection("posts").document()
            docPath = docRef.path
            val payload = mapOf(
                "authorId" to uid,
                "authorName" to author.displayName.ifBlank { "Neighbor" }.take(60),
                "authorInitials" to author.avatarInitials.ifBlank { "NB" }.take(4),
                "authorAvatarColor" to author.avatarColorHex,
                "neighborhoodId" to author.neighborhoodId,
                "neighborhoodName" to author.neighborhoodName,
                "visibility" to "NEIGHBORHOOD",
                "scope" to scope.name,
                "category" to category.name,
                "title" to cleanTitle,
                "body" to cleanBody,
                "locationTag" to locationTag.trim().take(100),
                "photoUri" to photoUri?.takeIf { it.isNotBlank() }?.take(500),
                "pollOptions" to cleanPoll,
                "pollVotes" to cleanVotes,
                "thanksCount" to 0L,
                "commentsCount" to 0L,
                "pinnedUrgent" to (pinnedUrgent && category == PostCategory.SAFETY),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun togglePostThank(post: NeighborhoodPost): Result<Unit> {
        var docPath = "posts/${post.id}"
        return try {
            val uid = requireUserId()
            val postRef = db.collection("posts").document(post.id)
            val thankRef = postRef.collection("thanks").document(uid)
            docPath = thankRef.path
            val alreadyThanked = thankRef.get().await().exists()
            val batch = db.batch()
            if (alreadyThanked) {
                batch.delete(thankRef)
                batch.update(
                    postRef,
                    mapOf(
                        "thanksCount" to (post.thanksCount - 1L).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    thankRef,
                    mapOf(
                        "userId" to uid,
                        "targetId" to post.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    postRef,
                    mapOf(
                        "thanksCount" to post.thanksCount + 1L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addComment(post: NeighborhoodPost, author: NeighborProfile, body: String): Result<Unit> {
        var docPath = "posts/${post.id}/comments"
        return try {
            val uid = requireUserId()
            val cleanBody = body.trim().take(1000)
            require(cleanBody.isNotEmpty()) { "Comment cannot be empty." }
            val postRef = db.collection("posts").document(post.id)
            val commentRef = postRef.collection("comments").document()
            docPath = commentRef.path
            val batch = db.batch()
            batch.set(
                commentRef,
                mapOf(
                    "postId" to post.id,
                    "authorId" to uid,
                    "authorName" to author.displayName.ifBlank { "Neighbor" }.take(60),
                    "authorInitials" to author.avatarInitials.ifBlank { "NB" }.take(4),
                    "authorAvatarColor" to author.avatarColorHex,
                    "neighborhoodName" to author.neighborhoodName,
                    "body" to cleanBody,
                    "visibility" to "NEIGHBORHOOD",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to post.commentsCount + 1L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun voteOnPoll(post: NeighborhoodPost, optionIndex: Int): Result<Unit> {
        var docPath = "posts/${post.id}"
        return try {
            requireUserId()
            if (optionIndex !in post.pollVotes.indices) return Result.failure(IllegalArgumentException("Invalid poll option"))
            val updatedVotes = post.pollVotes.toMutableList()
            updatedVotes[optionIndex] = updatedVotes[optionIndex] + 1L
            db.collection("posts").document(post.id).update(
                mapOf(
                    "pollVotes" to updatedVotes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val docPath = "posts/$postId"
        return try {
            requireUserId()
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Marketplace Listings ---

    fun observeListings(neighborhoodId: String): Flow<List<MarketplaceListing>> = flow {
        if (neighborhoodId.isBlank() || auth.currentUser?.uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("listings")
                .whereEqualTo("neighborhoodId", neighborhoodId)
                .whereEqualTo("visibility", "NEIGHBORHOOD")
                .snapshots()
                .map { snap ->
                    snap.toObjects(MarketplaceListing::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser != null) {
                        handleFirestoreError(toLog, OperationType.LIST, "listings")
                    }
                    throw error
                }
        )
    }

    suspend fun createListing(
        seller: NeighborProfile,
        title: String,
        description: String,
        priceDollars: Long,
        category: ListingCategory,
        condition: ListingCondition,
        pickupArea: String,
        photoUri: String?
    ): Result<Unit> {
        var docPath = "listings"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanDesc = description.trim().take(1500)
            require(cleanTitle.isNotEmpty() && cleanDesc.isNotEmpty()) { "Title and description are required." }
            val safePrice = if (category == ListingCategory.FREE) 0L else priceDollars.coerceIn(0L, 100000L)
            val docRef = db.collection("listings").document()
            docPath = docRef.path
            val payload = mapOf(
                "sellerId" to uid,
                "sellerName" to seller.displayName.ifBlank { "Neighbor" }.take(60),
                "sellerInitials" to seller.avatarInitials.ifBlank { "NB" }.take(4),
                "sellerAvatarColor" to seller.avatarColorHex,
                "neighborhoodId" to seller.neighborhoodId,
                "neighborhoodName" to seller.neighborhoodName,
                "visibility" to "NEIGHBORHOOD",
                "title" to cleanTitle,
                "description" to cleanDesc,
                "priceDollars" to safePrice,
                "category" to category.name,
                "condition" to condition.name,
                "status" to ListingStatus.AVAILABLE.name,
                "photoUri" to photoUri?.takeIf { it.isNotBlank() }?.take(500),
                "pickupArea" to pickupArea.trim().take(100),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateListingStatus(listingId: String, status: ListingStatus): Result<Unit> {
        val docPath = "listings/$listingId"
        return try {
            requireUserId()
            db.collection("listings").document(listingId).update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteListing(listingId: String): Result<Unit> {
        val docPath = "listings/$listingId"
        return try {
            requireUserId()
            db.collection("listings").document(listingId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Neighborhood Events ---

    fun observeEvents(neighborhoodId: String): Flow<List<NeighborhoodEvent>> = flow {
        if (neighborhoodId.isBlank() || auth.currentUser?.uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("events")
                .whereEqualTo("neighborhoodId", neighborhoodId)
                .whereEqualTo("visibility", "NEIGHBORHOOD")
                .snapshots()
                .map { snap ->
                    snap.toObjects(NeighborhoodEvent::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser != null) {
                        handleFirestoreError(toLog, OperationType.LIST, "events")
                    }
                    throw error
                }
        )
    }

    suspend fun createEvent(
        organizer: NeighborProfile,
        title: String,
        description: String,
        category: EventCategory,
        dateLabel: String,
        timeLabel: String,
        locationLabel: String
    ): Result<Unit> {
        var docPath = "events"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("events").document()
            val rsvpRef = docRef.collection("rsvps").document(uid)
            docPath = docRef.path
            val batch = db.batch()
            batch.set(
                docRef,
                mapOf(
                    "organizerId" to uid,
                    "organizerName" to organizer.displayName.ifBlank { "Neighbor" }.take(60),
                    "neighborhoodId" to organizer.neighborhoodId,
                    "neighborhoodName" to organizer.neighborhoodName,
                    "visibility" to "NEIGHBORHOOD",
                    "title" to title.trim().take(120),
                    "description" to description.trim().take(1200),
                    "category" to category.name,
                    "dateLabel" to dateLabel.trim().take(60),
                    "timeLabel" to timeLabel.trim().take(40),
                    "locationLabel" to locationLabel.trim().take(120),
                    "rsvpCount" to 1L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                rsvpRef,
                mapOf(
                    "userId" to uid,
                    "targetId" to docRef.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleEventRsvp(event: NeighborhoodEvent): Result<Boolean> {
        val docPath = "events/${event.id}"
        return try {
            val uid = requireUserId()
            val eventRef = db.collection("events").document(event.id)
            val rsvpRef = eventRef.collection("rsvps").document(uid)
            val going = rsvpRef.get().await().exists()
            val batch = db.batch()
            if (going) {
                batch.delete(rsvpRef)
                batch.update(
                    eventRef,
                    mapOf(
                        "rsvpCount" to (event.rsvpCount - 1L).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    rsvpRef,
                    mapOf(
                        "userId" to uid,
                        "targetId" to event.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    eventRef,
                    mapOf(
                        "rsvpCount" to event.rsvpCount + 1L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(!going)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- Neighborhood Groups ---

    fun observeGroups(neighborhoodId: String): Flow<List<NeighborhoodGroup>> = flow {
        if (neighborhoodId.isBlank() || auth.currentUser?.uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("groups")
                .whereEqualTo("neighborhoodId", neighborhoodId)
                .whereEqualTo("visibility", "NEIGHBORHOOD")
                .snapshots()
                .map { snap ->
                    snap.toObjects(NeighborhoodGroup::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.memberCount }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser != null) {
                        handleFirestoreError(toLog, OperationType.LIST, "groups")
                    }
                    throw error
                }
        )
    }

    suspend fun createGroup(
        creator: NeighborProfile,
        name: String,
        description: String,
        category: GroupCategory
    ): Result<Unit> {
        var docPath = "groups"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("groups").document()
            val memberRef = docRef.collection("members").document(uid)
            docPath = docRef.path
            val batch = db.batch()
            batch.set(
                docRef,
                mapOf(
                    "creatorId" to uid,
                    "creatorName" to creator.displayName.ifBlank { "Neighbor" }.take(60),
                    "neighborhoodId" to creator.neighborhoodId,
                    "neighborhoodName" to creator.neighborhoodName,
                    "visibility" to "NEIGHBORHOOD",
                    "name" to name.trim().take(80),
                    "description" to description.trim().take(600),
                    "category" to category.name,
                    "memberCount" to 1L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                memberRef,
                mapOf(
                    "userId" to uid,
                    "targetId" to docRef.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleGroupMembership(group: NeighborhoodGroup): Result<Boolean> {
        val docPath = "groups/${group.id}"
        return try {
            val uid = requireUserId()
            val groupRef = db.collection("groups").document(group.id)
            val memberRef = groupRef.collection("members").document(uid)
            val joined = memberRef.get().await().exists()
            val batch = db.batch()
            if (joined) {
                batch.delete(memberRef)
                batch.update(
                    groupRef,
                    mapOf(
                        "memberCount" to (group.memberCount - 1L).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    memberRef,
                    mapOf(
                        "userId" to uid,
                        "targetId" to group.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    groupRef,
                    mapOf(
                        "memberCount" to group.memberCount + 1L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(!joined)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- Local Businesses & Recommendations ---

    fun observeBusinesses(neighborhoodId: String): Flow<List<LocalBusiness>> = flow {
        if (neighborhoodId.isBlank() || auth.currentUser?.uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("businesses")
                .whereEqualTo("neighborhoodId", neighborhoodId)
                .whereEqualTo("visibility", "NEIGHBORHOOD")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LocalBusiness::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.favesCount }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser != null) {
                        handleFirestoreError(toLog, OperationType.LIST, "businesses")
                    }
                    throw error
                }
        )
    }

    suspend fun createBusiness(
        recommender: NeighborProfile,
        name: String,
        category: BusinessCategory,
        summary: String,
        addressLabel: String,
        phoneLabel: String
    ): Result<Unit> {
        var docPath = "businesses"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("businesses").document()
            val faveRef = docRef.collection("faves").document(uid)
            docPath = docRef.path
            val batch = db.batch()
            batch.set(
                docRef,
                mapOf(
                    "recommenderId" to uid,
                    "recommenderName" to recommender.displayName.ifBlank { "Neighbor" }.take(60),
                    "neighborhoodId" to recommender.neighborhoodId,
                    "neighborhoodName" to recommender.neighborhoodName,
                    "visibility" to "NEIGHBORHOOD",
                    "name" to name.trim().take(100),
                    "category" to category.name,
                    "summary" to summary.trim().take(500),
                    "addressLabel" to addressLabel.trim().take(120),
                    "phoneLabel" to phoneLabel.trim().take(40),
                    "favesCount" to 1L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                faveRef,
                mapOf(
                    "userId" to uid,
                    "targetId" to docRef.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleBusinessFave(business: LocalBusiness): Result<Boolean> {
        val docPath = "businesses/${business.id}"
        return try {
            val uid = requireUserId()
            val bizRef = db.collection("businesses").document(business.id)
            val faveRef = bizRef.collection("faves").document(uid)
            val faved = faveRef.get().await().exists()
            val batch = db.batch()
            if (faved) {
                batch.delete(faveRef)
                batch.update(
                    bizRef,
                    mapOf(
                        "favesCount" to (business.favesCount - 1L).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    faveRef,
                    mapOf(
                        "userId" to uid,
                        "targetId" to business.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    bizRef,
                    mapOf(
                        "favesCount" to business.favesCount + 1L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(!faved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }
}
