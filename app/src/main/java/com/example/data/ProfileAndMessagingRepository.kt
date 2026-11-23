package com.example.data

import com.example.model.DirectConversation
import com.example.model.DirectMessage
import com.example.model.NeighborNotification
import com.example.model.NeighborPrivateSettings
import com.example.model.NeighborProfile
import com.example.model.NeighborhoodCatalog
import com.example.model.initialsFor
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
import kotlin.math.abs

class ProfileAndMessagingRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "User must be signed in with Google." }
        return uid
    }

    fun observeProfile(userId: String): Flow<NeighborProfile?> = flow {
        if (userId.isBlank()) {
            emit(null)
            return@flow
        }
        val docPath = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(NeighborProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, docPath)
                    }
                    throw error
                }
        )
    }

    fun observePrivateSettings(userId: String): Flow<NeighborPrivateSettings?> = flow {
        if (userId.isBlank()) {
            emit(null)
            return@flow
        }
        val docPath = "users/$userId/private/settings"
        emitAll(
            db.collection("users").document(userId)
                .collection("private").document("settings")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(NeighborPrivateSettings::class.java, ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, docPath)
                    }
                    throw error
                }
        )
    }

    fun observeNeighbors(neighborhoodId: String): Flow<List<NeighborProfile>> = flow {
        if (neighborhoodId.isBlank() || auth.currentUser?.uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("users")
                .whereEqualTo("neighborhoodId", neighborhoodId)
                .whereEqualTo("publicProfile", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(NeighborProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName.lowercase() }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser != null) {
                        handleFirestoreError(toLog, OperationType.LIST, "users")
                    }
                    throw error
                }
        )
    }

    fun observeNotifications(userId: String): Flow<List<NeighborNotification>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(NeighborNotification::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveOnboardingProfile(
        displayName: String,
        neighborhoodId: String,
        streetName: String,
        bio: String,
        interests: List<String>
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().take(60)
            require(cleanName.isNotEmpty()) { "Please enter your display name." }
            val area = NeighborhoodCatalog.byId(neighborhoodId)
            val colorIndex = abs(uid.hashCode()) % NeighborhoodCatalog.avatarColors.size
            val avatarColor = NeighborhoodCatalog.avatarColors[colorIndex]
            val userRef = db.collection("users").document(uid)
            val settingsRef = userRef.collection("private").document("settings")
            val notifRef = userRef.collection("notifications").document("welcome")
            docPath = userRef.path

            val batch = db.batch()
            batch.set(
                userRef,
                mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "neighborhoodId" to area.id,
                    "neighborhoodName" to area.name,
                    "bio" to bio.trim().take(240),
                    "avatarInitials" to initialsFor(cleanName),
                    "avatarColorHex" to avatarColor,
                    "interests" to interests.take(10),
                    "helpfulCount" to 0L,
                    "publicProfile" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                settingsRef,
                mapOf(
                    "userId" to uid,
                    "email" to (auth.currentUser?.email ?: "").take(120),
                    "streetName" to streetName.trim().take(100),
                    "radiusMiles" to 3L,
                    "savedPostIds" to emptyList<String>(),
                    "savedListingIds" to emptyList<String>(),
                    "alertsEnabled" to true,
                    "digestEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                notifRef,
                mapOf(
                    "userId" to uid,
                    "title" to "Welcome to ${area.name}!",
                    "body" to "You're connected with verified neighbors in ${area.name}. Say hello or explore local recommendations.",
                    "category" to "EVENT",
                    "targetId" to area.id,
                    "readFlag" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateNeighborhood(neighborhoodId: String, radiusMiles: Long): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val area = NeighborhoodCatalog.byId(neighborhoodId)
            val safeRadius = radiusMiles.coerceIn(1L, 25L)
            val userRef = db.collection("users").document(uid)
            val settingsRef = userRef.collection("private").document("settings")
            docPath = userRef.path
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "neighborhoodId" to area.id,
                    "neighborhoodName" to area.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                settingsRef,
                mapOf(
                    "radiusMiles" to safeRadius,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProfileDetails(
        displayName: String,
        bio: String,
        streetName: String,
        interests: List<String>
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().take(60)
            require(cleanName.isNotEmpty()) { "Display name cannot be empty." }
            val userRef = db.collection("users").document(uid)
            val settingsRef = userRef.collection("private").document("settings")
            docPath = userRef.path
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "displayName" to cleanName,
                    "avatarInitials" to initialsFor(cleanName),
                    "bio" to bio.trim().take(240),
                    "interests" to interests.take(10),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                settingsRef,
                mapOf(
                    "streetName" to streetName.trim().take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(alertsEnabled: Boolean, digestEnabled: Boolean, radiusMiles: Long): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val settingsRef = db.collection("users").document(uid).collection("private").document("settings")
            docPath = settingsRef.path
            settingsRef.update(
                mapOf(
                    "alertsEnabled" to alertsEnabled,
                    "digestEnabled" to digestEnabled,
                    "radiusMiles" to radiusMiles.coerceIn(1L, 25L),
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

    suspend fun toggleSavedPost(postId: String, currentSaved: List<String>): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val settingsRef = db.collection("users").document(uid).collection("private").document("settings")
            docPath = settingsRef.path
            val updated = if (postId in currentSaved) currentSaved - postId else (currentSaved + postId).takeLast(100)
            settingsRef.update(
                mapOf(
                    "savedPostIds" to updated,
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

    suspend fun toggleSavedListing(listingId: String, currentSaved: List<String>): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val settingsRef = db.collection("users").document(uid).collection("private").document("settings")
            docPath = settingsRef.path
            val updated = if (listingId in currentSaved) currentSaved - listingId else (currentSaved + listingId).takeLast(100)
            settingsRef.update(
                mapOf(
                    "savedListingIds" to updated,
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

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("notifications").document(notificationId)
            docPath = ref.path
            ref.update(mapOf("readFlag" to true)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createLocalNotification(title: String, body: String, category: String, targetId: String): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("notifications").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "userId" to uid,
                    "title" to title.take(120),
                    "body" to body.take(300),
                    "category" to category,
                    "targetId" to targetId.take(128),
                    "readFlag" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    // --- Direct Messaging ---

    fun observeConversations(userId: String): Flow<List<DirectConversation>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("conversations")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DirectConversation::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, "conversations")
                    }
                    throw error
                }
        )
    }

    fun observeMessages(conversationId: String, userId: String): Flow<List<DirectMessage>> = flow {
        if (conversationId.isBlank() || userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "conversations/$conversationId/messages"
        emitAll(
            db.collection("conversations").document(conversationId).collection("messages")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DirectMessage::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val fsError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsError == null && error is CancellationException) throw error
                    val toLog = fsError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun sendMessageToNeighbor(
        otherUserId: String,
        otherUserName: String,
        senderName: String,
        subject: String,
        text: String
    ): Result<String> {
        var docPath = "conversations"
        return try {
            val uid = requireUserId()
            require(otherUserId.isNotBlank() && otherUserId != uid) { "Invalid recipient neighbor." }
            val cleanText = text.trim().take(1000)
            require(cleanText.isNotEmpty()) { "Message cannot be empty." }

            val paired = listOf(uid to senderName.take(60), otherUserId to otherUserName.take(60))
                .sortedBy { it.first }
            val convId = "${paired[0].first}_${paired[1].first}"
            val participantUids = listOf(paired[0].first, paired[1].first)
            val participantNames = listOf(paired[0].second, paired[1].second)

            val convRef = db.collection("conversations").document(convId)
            val msgRef = convRef.collection("messages").document()
            docPath = convRef.path

            val existing = convRef.get().await().exists()
            val batch = db.batch()
            if (!existing) {
                batch.set(
                    convRef,
                    mapOf(
                        "participantUids" to participantUids,
                        "participantNames" to participantNames,
                        "subject" to subject.trim().take(120),
                        "lastMessageText" to cleanText.take(500),
                        "lastSenderId" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    convRef,
                    mapOf(
                        "lastMessageText" to cleanText.take(500),
                        "lastSenderId" to uid,
                        "subject" to subject.trim().take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                msgRef,
                mapOf(
                    "conversationId" to convId,
                    "senderId" to uid,
                    "senderName" to senderName.trim().take(60),
                    "participantUids" to participantUids,
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
