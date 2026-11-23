package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CommunityFeedRepository
import com.example.data.ProfileAndMessagingRepository
import com.example.model.BusinessCategory
import com.example.model.DirectConversation
import com.example.model.DirectMessage
import com.example.model.EventCategory
import com.example.model.FeedSort
import com.example.model.GroupCategory
import com.example.model.ListingCategory
import com.example.model.ListingCondition
import com.example.model.ListingStatus
import com.example.model.LocalBusiness
import com.example.model.MarketplaceListing
import com.example.model.NeighborNotification
import com.example.model.NeighborPrivateSettings
import com.example.model.NeighborProfile
import com.example.model.NeighborhoodEvent
import com.example.model.NeighborhoodGroup
import com.example.model.NeighborhoodPost
import com.example.model.PostCategory
import com.example.model.PostComment
import com.example.model.PostScope
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NextBlockViewModel(
    private val profileRepo: ProfileAndMessagingRepository,
    private val feedRepo: CommunityFeedRepository,
    val currentUserId: String
) : ViewModel() {

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Filter & Search States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPostCategory = MutableStateFlow<PostCategory?>(null)
    val selectedPostCategory: StateFlow<PostCategory?> = _selectedPostCategory.asStateFlow()

    private val _selectedSort = MutableStateFlow(FeedSort.RECENT)
    val selectedSort: StateFlow<FeedSort> = _selectedSort.asStateFlow()

    private val _selectedListingCategory = MutableStateFlow<ListingCategory?>(null)
    val selectedListingCategory: StateFlow<ListingCategory?> = _selectedListingCategory.asStateFlow()

    private val _selectedPostId = MutableStateFlow<String?>(null)
    val selectedPostId: StateFlow<String?> = _selectedPostId.asStateFlow()

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    // Local optimistic sets for instant button highlight alongside atomic Firestore sync
    private val _votedPollPostIds = MutableStateFlow<Set<String>>(emptySet())
    val votedPollPostIds: StateFlow<Set<String>> = _votedPollPostIds.asStateFlow()

    private val _rsvpedEventIds = MutableStateFlow<Set<String>>(emptySet())
    val rsvpedEventIds: StateFlow<Set<String>> = _rsvpedEventIds.asStateFlow()

    private val _joinedGroupIds = MutableStateFlow<Set<String>>(emptySet())
    val joinedGroupIds: StateFlow<Set<String>> = _joinedGroupIds.asStateFlow()

    private val _favedBusinessIds = MutableStateFlow<Set<String>>(emptySet())
    val favedBusinessIds: StateFlow<Set<String>> = _favedBusinessIds.asStateFlow()

    // --- Two-Tier Error-Unwrapping Stream Helper ---
    private fun <T> Flow<T>.asUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    // --- Profile & Settings Streams ---
    val profileState: StateFlow<UiState<NeighborProfile?>> =
        profileRepo.observeProfile(currentUserId).asUiState("profile")

    val privateSettingsState: StateFlow<UiState<NeighborPrivateSettings?>> =
        profileRepo.observePrivateSettings(currentUserId).asUiState("settings")

    val notificationsState: StateFlow<UiState<List<NeighborNotification>>> =
        profileRepo.observeNotifications(currentUserId).asUiState("notifications")

    private val activeNeighborhoodId: Flow<String> = profileState
        .map { state ->
            when (state) {
                is UiState.Success -> state.data?.neighborhoodId.orEmpty()
                else -> ""
            }
        }
        .distinctUntilChanged()

    // --- Neighborhood-Scoped Community Streams ---
    val postsState: StateFlow<UiState<List<NeighborhoodPost>>> =
        activeNeighborhoodId.flatMapLatest { nId ->
            if (nId.isBlank()) flowOf(emptyList()) else feedRepo.observePosts(nId)
        }.asUiState("posts")

    val filteredPostsState: StateFlow<UiState<List<NeighborhoodPost>>> =
        combine(postsState, _selectedPostCategory, _selectedSort, _searchQuery) { state, cat, sort, query ->
            when (state) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> state
                is UiState.Success -> {
                    val q = query.trim().lowercase()
                    val filtered = state.data.filter { post ->
                        val catMatch = cat == null || post.category == cat.name
                        val queryMatch = q.isEmpty() ||
                            post.title.lowercase().contains(q) ||
                            post.body.lowercase().contains(q) ||
                            post.authorName.lowercase().contains(q) ||
                            post.locationTag.lowercase().contains(q)
                        catMatch && queryMatch
                    }
                    val sorted = when (sort) {
                        FeedSort.RECENT -> filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
                        FeedSort.TOP -> filtered.sortedByDescending { it.thanksCount + it.commentsCount * 2L }
                        FeedSort.URGENT -> filtered.sortedWith(
                            compareByDescending<NeighborhoodPost> { it.pinnedUrgent }
                                .thenByDescending { it.category == PostCategory.SAFETY.name }
                                .thenByDescending { it.createdAt?.seconds ?: 0L }
                        )
                    }
                    UiState.Success(sorted)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val listingsState: StateFlow<UiState<List<MarketplaceListing>>> =
        activeNeighborhoodId.flatMapLatest { nId ->
            if (nId.isBlank()) flowOf(emptyList()) else feedRepo.observeListings(nId)
        }.asUiState("listings")

    val filteredListingsState: StateFlow<UiState<List<MarketplaceListing>>> =
        combine(listingsState, _selectedListingCategory, _searchQuery) { state, cat, query ->
            when (state) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> state
                is UiState.Success -> {
                    val q = query.trim().lowercase()
                    val filtered = state.data.filter { item ->
                        val catMatch = cat == null ||
                            (cat == ListingCategory.FREE && item.priceDollars == 0L) ||
                            item.category == cat.name
                        val queryMatch = q.isEmpty() ||
                            item.title.lowercase().contains(q) ||
                            item.description.lowercase().contains(q)
                        catMatch && queryMatch
                    }
                    UiState.Success(filtered)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val eventsState: StateFlow<UiState<List<NeighborhoodEvent>>> =
        activeNeighborhoodId.flatMapLatest { nId ->
            if (nId.isBlank()) flowOf(emptyList()) else feedRepo.observeEvents(nId)
        }.asUiState("events")

    val groupsState: StateFlow<UiState<List<NeighborhoodGroup>>> =
        activeNeighborhoodId.flatMapLatest { nId ->
            if (nId.isBlank()) flowOf(emptyList()) else feedRepo.observeGroups(nId)
        }.asUiState("groups")

    val businessesState: StateFlow<UiState<List<LocalBusiness>>> =
        activeNeighborhoodId.flatMapLatest { nId ->
            if (nId.isBlank()) flowOf(emptyList()) else feedRepo.observeBusinesses(nId)
        }.asUiState("businesses")

    val neighborsState: StateFlow<UiState<List<NeighborProfile>>> =
        activeNeighborhoodId.flatMapLatest { nId ->
            if (nId.isBlank()) flowOf(emptyList()) else profileRepo.observeNeighbors(nId)
        }.asUiState("neighbors")

    val conversationsState: StateFlow<UiState<List<DirectConversation>>> =
        profileRepo.observeConversations(currentUserId).asUiState("conversations")

    val activePostCommentsState: StateFlow<UiState<List<PostComment>>> =
        _selectedPostId.flatMapLatest { postId ->
            if (postId.isNullOrBlank()) flowOf(emptyList()) else feedRepo.observeComments(postId)
        }.asUiState("comments")

    val activePostThankedState: StateFlow<UiState<Boolean>> =
        _selectedPostId.flatMapLatest { postId ->
            if (postId.isNullOrBlank()) flowOf(false) else feedRepo.observeUserThankedPost(postId, currentUserId)
        }.asUiState("thanked")

    val activeMessagesState: StateFlow<UiState<List<DirectMessage>>> =
        _selectedConversationId.flatMapLatest { convId ->
            if (convId.isNullOrBlank()) flowOf(emptyList()) else profileRepo.observeMessages(convId, currentUserId)
        }.asUiState("messages")

    // --- UI Filter Mutators ---
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setPostCategory(category: PostCategory?) { _selectedPostCategory.value = category }
    fun setFeedSort(sort: FeedSort) { _selectedSort.value = sort }
    fun setListingCategory(category: ListingCategory?) { _selectedListingCategory.value = category }
    fun selectPost(postId: String?) { _selectedPostId.value = postId }
    fun selectConversation(convId: String?) { _selectedConversationId.value = convId }

    private fun currentProfile(): NeighborProfile? =
        (profileState.value as? UiState.Success)?.data

    private fun currentSettings(): NeighborPrivateSettings? =
        (privateSettingsState.value as? UiState.Success)?.data

    // --- Profile & Onboarding Actions ---
    fun completeOnboarding(
        displayName: String,
        neighborhoodId: String,
        streetName: String,
        bio: String,
        interests: List<String>
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = profileRepo.saveOnboardingProfile(displayName, neighborhoodId, streetName, bio, interests)
            _isBusy.value = false
            res.onSuccess { _toastMessage.tryEmit("Welcome to your neighborhood!") }
                .onFailure { _toastMessage.tryEmit(it.message ?: "Could not save profile") }
        }
    }

    fun switchNeighborhood(neighborhoodId: String, radiusMiles: Long) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = profileRepo.updateNeighborhood(neighborhoodId, radiusMiles)
            _isBusy.value = false
            res.onSuccess { _toastMessage.tryEmit("Neighborhood updated") }
                .onFailure { _toastMessage.tryEmit(it.message ?: "Update failed") }
        }
    }

    fun updateProfileDetails(displayName: String, bio: String, streetName: String, interests: List<String>) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = profileRepo.updateProfileDetails(displayName, bio, streetName, interests)
            _isBusy.value = false
            res.onSuccess { _toastMessage.tryEmit("Profile updated") }
                .onFailure { _toastMessage.tryEmit(it.message ?: "Update failed") }
        }
    }

    fun updatePreferences(alertsEnabled: Boolean, digestEnabled: Boolean, radiusMiles: Long) {
        viewModelScope.launch {
            val res = profileRepo.updatePreferences(alertsEnabled, digestEnabled, radiusMiles)
            res.onFailure { _toastMessage.tryEmit(it.message ?: "Could not update preferences") }
        }
    }

    fun toggleSavedPost(postId: String) {
        val current = currentSettings()?.savedPostIds ?: emptyList()
        viewModelScope.launch {
            val res = profileRepo.toggleSavedPost(postId, current)
            res.onSuccess {
                val added = postId !in current
                _toastMessage.tryEmit(if (added) "Saved to bookmarks" else "Removed from bookmarks")
            }.onFailure { _toastMessage.tryEmit(it.message ?: "Action failed") }
        }
    }

    fun toggleSavedListing(listingId: String) {
        val current = currentSettings()?.savedListingIds ?: emptyList()
        viewModelScope.launch {
            val res = profileRepo.toggleSavedListing(listingId, current)
            res.onSuccess {
                val added = listingId !in current
                _toastMessage.tryEmit(if (added) "Listing saved" else "Removed from saved")
            }.onFailure { _toastMessage.tryEmit(it.message ?: "Action failed") }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            profileRepo.markNotificationRead(notificationId)
        }
    }

    // --- Feed Post Actions ---
    fun createPost(
        category: PostCategory,
        scope: PostScope,
        title: String,
        body: String,
        locationTag: String,
        photoUri: String?,
        pollOptions: List<String>,
        pinnedUrgent: Boolean,
        onCreated: () -> Unit = {}
    ) {
        val author = currentProfile() ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = feedRepo.createPost(author, category, scope, title, body, locationTag, photoUri, pollOptions, pinnedUrgent)
            _isBusy.value = false
            res.onSuccess { postId ->
                if (category == PostCategory.SAFETY) {
                    profileRepo.createLocalNotification(
                        title = "Safety Alert Posted",
                        body = title.take(120),
                        category = "SAFETY",
                        targetId = postId
                    )
                }
                _toastMessage.tryEmit("Posted to ${author.neighborhoodName}")
                onCreated()
            }.onFailure { _toastMessage.tryEmit(it.message ?: "Could not publish post") }
        }
    }

    fun togglePostThank(post: NeighborhoodPost) {
        viewModelScope.launch {
            val res = feedRepo.togglePostThank(post)
            res.onFailure { _toastMessage.tryEmit(it.message ?: "Could not update thank") }
        }
    }

    fun addComment(post: NeighborhoodPost, body: String, onDone: () -> Unit = {}) {
        val author = currentProfile() ?: return
        viewModelScope.launch {
            val res = feedRepo.addComment(post, author, body)
            res.onSuccess { onDone() }
                .onFailure { _toastMessage.tryEmit(it.message ?: "Could not add comment") }
        }
    }

    fun voteOnPoll(post: NeighborhoodPost, optionIndex: Int) {
        if (post.id in _votedPollPostIds.value) {
            _toastMessage.tryEmit("You already voted on this poll")
            return
        }
        viewModelScope.launch {
            val res = feedRepo.voteOnPoll(post, optionIndex)
            res.onSuccess {
                _votedPollPostIds.value = _votedPollPostIds.value + post.id
                _toastMessage.tryEmit("Vote recorded")
            }.onFailure { _toastMessage.tryEmit(it.message ?: "Vote failed") }
        }
    }

    fun deletePost(postId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val res = feedRepo.deletePost(postId)
            res.onSuccess {
                _toastMessage.tryEmit("Post deleted")
                onDone()
            }.onFailure { _toastMessage.tryEmit(it.message ?: "Delete failed") }
        }
    }

    // --- Marketplace Actions ---
    fun createListing(
        title: String,
        description: String,
        priceDollars: Long,
        category: ListingCategory,
        condition: ListingCondition,
        pickupArea: String,
        photoUri: String?,
        onCreated: () -> Unit = {}
    ) {
        val seller = currentProfile() ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = feedRepo.createListing(seller, title, description, priceDollars, category, condition, pickupArea, photoUri)
            _isBusy.value = false
            res.onSuccess {
                _toastMessage.tryEmit("Listing posted in ${seller.neighborhoodName}")
                onCreated()
            }.onFailure { _toastMessage.tryEmit(it.message ?: "Could not list item") }
        }
    }

    fun updateListingStatus(listingId: String, status: ListingStatus, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val res = feedRepo.updateListingStatus(listingId, status)
            res.onSuccess {
                _toastMessage.tryEmit("Marked as ${status.label}")
                onDone()
            }.onFailure { _toastMessage.tryEmit(it.message ?: "Update failed") }
        }
    }

    fun deleteListing(listingId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val res = feedRepo.deleteListing(listingId)
            res.onSuccess {
                _toastMessage.tryEmit("Listing removed")
                onDone()
            }.onFailure { _toastMessage.tryEmit(it.message ?: "Delete failed") }
        }
    }

    // --- Events, Groups, Businesses ---
    fun createEvent(
        title: String,
        description: String,
        category: EventCategory,
        dateLabel: String,
        timeLabel: String,
        locationLabel: String,
        onCreated: () -> Unit = {}
    ) {
        val organizer = currentProfile() ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = feedRepo.createEvent(organizer, title, description, category, dateLabel, timeLabel, locationLabel)
            _isBusy.value = false
            res.onSuccess {
                _toastMessage.tryEmit("Event added to ${organizer.neighborhoodName}")
                onCreated()
            }.onFailure { _toastMessage.tryEmit(it.message ?: "Could not create event") }
        }
    }

    fun toggleEventRsvp(event: NeighborhoodEvent) {
        viewModelScope.launch {
            val res = feedRepo.toggleEventRsvp(event)
            res.onSuccess { going ->
                _rsvpedEventIds.value = if (going) _rsvpedEventIds.value + event.id else _rsvpedEventIds.value - event.id
                _toastMessage.tryEmit(if (going) "RSVP confirmed" else "RSVP cancelled")
            }.onFailure { _toastMessage.tryEmit(it.message ?: "RSVP failed") }
        }
    }

    fun createGroup(
        name: String,
        description: String,
        category: GroupCategory,
        onCreated: () -> Unit = {}
    ) {
        val creator = currentProfile() ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = feedRepo.createGroup(creator, name, description, category)
            _isBusy.value = false
            res.onSuccess {
                _toastMessage.tryEmit("Group created")
                onCreated()
            }.onFailure { _toastMessage.tryEmit(it.message ?: "Could not create group") }
        }
    }

    fun toggleGroupMembership(group: NeighborhoodGroup) {
        viewModelScope.launch {
            val res = feedRepo.toggleGroupMembership(group)
            res.onSuccess { joined ->
                _joinedGroupIds.value = if (joined) _joinedGroupIds.value + group.id else _joinedGroupIds.value - group.id
                _toastMessage.tryEmit(if (joined) "Joined ${group.name}" else "Left ${group.name}")
            }.onFailure { _toastMessage.tryEmit(it.message ?: "Action failed") }
        }
    }

    fun createBusiness(
        name: String,
        category: BusinessCategory,
        summary: String,
        addressLabel: String,
        phoneLabel: String,
        onCreated: () -> Unit = {}
    ) {
        val recommender = currentProfile() ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = feedRepo.createBusiness(recommender, name, category, summary, addressLabel, phoneLabel)
            _isBusy.value = false
            res.onSuccess {
                _toastMessage.tryEmit("Recommendation added")
                onCreated()
            }.onFailure { _toastMessage.tryEmit(it.message ?: "Could not add business") }
        }
    }

    fun toggleBusinessFave(business: LocalBusiness) {
        viewModelScope.launch {
            val res = feedRepo.toggleBusinessFave(business)
            res.onSuccess { faved ->
                _favedBusinessIds.value = if (faved) _favedBusinessIds.value + business.id else _favedBusinessIds.value - business.id
            }.onFailure { _toastMessage.tryEmit(it.message ?: "Could not update fave") }
        }
    }

    // --- Direct Messaging ---
    fun sendMessageToNeighbor(
        otherUserId: String,
        otherUserName: String,
        subject: String,
        text: String,
        onSent: (String) -> Unit = {}
    ) {
        val sender = currentProfile() ?: return
        viewModelScope.launch {
            val res = profileRepo.sendMessageToNeighbor(
                otherUserId = otherUserId,
                otherUserName = otherUserName,
                senderName = sender.displayName.ifBlank { "Neighbor" },
                subject = subject,
                text = text
            )
            res.onSuccess { convId ->
                _selectedConversationId.value = convId
                onSent(convId)
            }.onFailure { _toastMessage.tryEmit(it.message ?: "Could not send message") }
        }
    }

    private companion object {
        const val TAG = "NextBlockVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
