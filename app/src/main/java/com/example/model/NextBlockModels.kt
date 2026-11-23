package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class PostCategory(val label: String) {
    GENERAL("General"),
    SAFETY("Crime & Safety"),
    RECOMMENDATION("Recommendations"),
    LOST_FOUND("Lost & Found"),
    EVENT("Events"),
    POLL("Polls");

    companion object {
        fun fromRaw(raw: String): PostCategory =
            entries.firstOrNull { it.name == raw } ?: GENERAL
    }
}

enum class PostScope(val label: String) {
    LOCAL("My Neighborhood"),
    NEARBY("Nearby Neighborhoods");

    companion object {
        fun fromRaw(raw: String): PostScope =
            entries.firstOrNull { it.name == raw } ?: LOCAL
    }
}

enum class FeedSort(val label: String) {
    RECENT("Recent"),
    TOP("Top"),
    URGENT("Urgent First")
}

enum class ListingCategory(val label: String) {
    FREE("Free"),
    FURNITURE("Furniture"),
    ELECTRONICS("Electronics"),
    GARDEN("Garden & Patio"),
    KIDS("Kids & Baby"),
    TOOLS("Tools"),
    OTHER("Other");

    companion object {
        fun fromRaw(raw: String): ListingCategory =
            entries.firstOrNull { it.name == raw } ?: OTHER
    }
}

enum class ListingCondition(val label: String) {
    NEW("New"),
    LIKE_NEW("Like New"),
    GOOD("Good"),
    FAIR("Fair");

    companion object {
        fun fromRaw(raw: String): ListingCondition =
            entries.firstOrNull { it.name == raw } ?: GOOD
    }
}

enum class ListingStatus(val label: String) {
    AVAILABLE("Available"),
    PENDING("Pending Pickup"),
    SOLD("Sold");

    companion object {
        fun fromRaw(raw: String): ListingStatus =
            entries.firstOrNull { it.name == raw } ?: AVAILABLE
    }
}

enum class EventCategory(val label: String) {
    BLOCK_PARTY("Block Party"),
    GARAGE_SALE("Garage Sale"),
    CLEANUP("Park Cleanup"),
    MARKET("Farmers Market"),
    WORKSHOP("Workshop");

    companion object {
        fun fromRaw(raw: String): EventCategory =
            entries.firstOrNull { it.name == raw } ?: BLOCK_PARTY
    }
}

enum class GroupCategory(val label: String) {
    GARDENING("Gardening"),
    PETS("Pet Parents"),
    SAFETY("Safety Watch"),
    PARENTS("Parents"),
    BUY_NOTHING("Buy Nothing"),
    FITNESS("Walking & Fitness");

    companion object {
        fun fromRaw(raw: String): GroupCategory =
            entries.firstOrNull { it.name == raw } ?: GARDENING
    }
}

enum class BusinessCategory(val label: String) {
    HOME_SERVICES("Home Services"),
    CAFE_FOOD("Cafes & Food"),
    PETS("Pet Care & Vets"),
    HEALTH("Health & Wellness"),
    AUTO("Auto Repair"),
    LANDSCAPING("Landscaping");

    companion object {
        fun fromRaw(raw: String): BusinessCategory =
            entries.firstOrNull { it.name == raw } ?: HOME_SERVICES
    }
}

data class NeighborhoodArea(
    val id: String,
    val name: String,
    val cityLabel: String,
    val landmarks: String,
    val mapXFraction: Float,
    val mapYFraction: Float
)

object NeighborhoodCatalog {
    val areas = listOf(
        NeighborhoodArea(
            id = "maple_heights",
            name = "Maple Heights",
            cityLabel = "Northside",
            landmarks = "Oak Park Trail • Maple Ave Market",
            mapXFraction = 0.46f,
            mapYFraction = 0.42f
        ),
        NeighborhoodArea(
            id = "oakwood_district",
            name = "Oakwood District",
            cityLabel = "West End",
            landmarks = "Oakwood Library • Elm St Commons",
            mapXFraction = 0.25f,
            mapYFraction = 0.56f
        ),
        NeighborhoodArea(
            id = "cedar_park",
            name = "Cedar Park",
            cityLabel = "Eastside",
            landmarks = "Cedar Creek • Botanical Gardens",
            mapXFraction = 0.72f,
            mapYFraction = 0.35f
        ),
        NeighborhoodArea(
            id = "riverside_commons",
            name = "Riverside Commons",
            cityLabel = "Waterfront",
            landmarks = "Riverwalk Pier • Farmers Plaza",
            mapXFraction = 0.62f,
            mapYFraction = 0.72f
        ),
        NeighborhoodArea(
            id = "willow_creek",
            name = "Willow Creek",
            cityLabel = "South Hills",
            landmarks = "Willow Pond • Dog Run Park",
            mapXFraction = 0.34f,
            mapYFraction = 0.78f
        ),
        NeighborhoodArea(
            id = "historic_midtown",
            name = "Historic Midtown",
            cityLabel = "Central",
            landmarks = "Town Square • Clocktower Cafe",
            mapXFraction = 0.50f,
            mapYFraction = 0.20f
        )
    )

    val interestOptions = listOf(
        "Gardening",
        "Pet Parents",
        "Safety Watch",
        "Local Deals",
        "Events & Markets",
        "Home DIY",
        "Walking & Trails",
        "Buy Nothing"
    )

    val avatarColors = listOf(
        0xFF0A6847L,
        0xFF157A8CL,
        0xFFD97724L,
        0xFF3E569EL,
        0xFF7B4B94L,
        0xFF2B6CB0L
    )

    fun byId(id: String): NeighborhoodArea =
        areas.firstOrNull { it.id == id } ?: areas.first()
}

@Keep
@IgnoreExtraProperties
data class NeighborProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val neighborhoodId: String = "maple_heights",
    val neighborhoodName: String = "Maple Heights",
    val bio: String = "",
    val avatarInitials: String = "NB",
    val avatarColorHex: Long = 0xFF0A6847L,
    val interests: List<String> = emptyList(),
    val helpfulCount: Long = 0L,
    val publicProfile: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NeighborPrivateSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val streetName: String = "",
    val radiusMiles: Long = 3L,
    val savedPostIds: List<String> = emptyList(),
    val savedListingIds: List<String> = emptyList(),
    val alertsEnabled: Boolean = true,
    val digestEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NeighborNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "COMMENT",
    val targetId: String = "",
    val readFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NeighborhoodPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorInitials: String = "NB",
    val authorAvatarColor: Long = 0xFF0A6847L,
    val neighborhoodId: String = "maple_heights",
    val neighborhoodName: String = "Maple Heights",
    val visibility: String = "NEIGHBORHOOD",
    val scope: String = "LOCAL",
    val category: String = "GENERAL",
    val title: String = "",
    val body: String = "",
    val locationTag: String = "",
    val photoUri: String? = null,
    val pollOptions: List<String> = emptyList(),
    val pollVotes: List<Long> = emptyList(),
    val thanksCount: Long = 0L,
    val commentsCount: Long = 0L,
    val pinnedUrgent: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorInitials: String = "NB",
    val authorAvatarColor: Long = 0xFF0A6847L,
    val neighborhoodName: String = "Maple Heights",
    val body: String = "",
    val visibility: String = "NEIGHBORHOOD",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MarketplaceListing(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerInitials: String = "NB",
    val sellerAvatarColor: Long = 0xFF0A6847L,
    val neighborhoodId: String = "maple_heights",
    val neighborhoodName: String = "Maple Heights",
    val visibility: String = "NEIGHBORHOOD",
    val title: String = "",
    val description: String = "",
    val priceDollars: Long = 0L,
    val category: String = "OTHER",
    val condition: String = "GOOD",
    val status: String = "AVAILABLE",
    val photoUri: String? = null,
    val pickupArea: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NeighborhoodEvent(
    @DocumentId val id: String = "",
    val organizerId: String = "",
    val organizerName: String = "",
    val neighborhoodId: String = "maple_heights",
    val neighborhoodName: String = "Maple Heights",
    val visibility: String = "NEIGHBORHOOD",
    val title: String = "",
    val description: String = "",
    val category: String = "BLOCK_PARTY",
    val dateLabel: String = "",
    val timeLabel: String = "",
    val locationLabel: String = "",
    val rsvpCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NeighborhoodGroup(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val neighborhoodId: String = "maple_heights",
    val neighborhoodName: String = "Maple Heights",
    val visibility: String = "NEIGHBORHOOD",
    val name: String = "",
    val description: String = "",
    val category: String = "GARDENING",
    val memberCount: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LocalBusiness(
    @DocumentId val id: String = "",
    val recommenderId: String = "",
    val recommenderName: String = "",
    val neighborhoodId: String = "maple_heights",
    val neighborhoodName: String = "Maple Heights",
    val visibility: String = "NEIGHBORHOOD",
    val name: String = "",
    val category: String = "HOME_SERVICES",
    val summary: String = "",
    val addressLabel: String = "",
    val phoneLabel: String = "",
    val favesCount: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectConversation(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
    val subject: String = "",
    val lastMessageText: String = "",
    val lastSenderId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val participantUids: List<String> = emptyList(),
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

fun initialsFor(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "NB"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}
