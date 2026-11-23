package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.CommunityFeedRepository
import com.example.data.ProfileAndMessagingRepository
import com.example.data.authStateFlow
import com.example.data.signOut
import com.example.model.NeighborProfile
import com.example.model.PostCategory
import com.example.model.UiState
import com.example.ui.components.CreatePostSheet
import com.example.ui.components.NeighborhoodSwitcherSheet
import com.example.ui.components.NotificationsSheet
import com.example.ui.components.PostDetailSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.NeighborhoodSetupScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SafetyHubScreen
import com.example.ui.theme.NextBlockTheme
import com.example.viewmodel.NextBlockViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_HOME = "home"
const val ROUTE_DISCOVER = "discover"
const val ROUTE_MARKET = "market"
const val ROUTE_SAFETY = "safety"
const val ROUTE_MESSAGES = "messages"
const val ROUTE_PROFILE = "profile"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val primaryNavItems = listOf(
    NavDestinationItem(ROUTE_HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavDestinationItem(ROUTE_DISCOVER, "Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
    NavDestinationItem(ROUTE_MARKET, "For Sale", Icons.Filled.Storefront, Icons.Outlined.Storefront),
    NavDestinationItem(ROUTE_SAFETY, "Safety", Icons.Filled.Shield, Icons.Outlined.Shield),
    NavDestinationItem(ROUTE_MESSAGES, "Messages", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NextBlockTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            val viewModel: NextBlockViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        NextBlockViewModel(
                            profileRepo = ProfileAndMessagingRepository(db),
                            feedRepo = CommunityFeedRepository(db),
                            currentUserId = user.uid
                        )
                    }
                }
            )
            AuthenticatedNextBlockApp(viewModel = viewModel)
        }
    }
}

@Composable
private fun AuthenticatedNextBlockApp(viewModel: NextBlockViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    when (val state = profileState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { signOut(credentialManager, scope) }) {
                        Text("Sign Out & Retry")
                    }
                }
            }
        }

        is UiState.Success -> {
            val profile = state.data
            if (profile == null) {
                NeighborhoodSetupScreen(
                    isBusy = isBusy,
                    onCompleteSetup = { name, nId, street, bio, interests ->
                        viewModel.completeOnboarding(name, nId, street, bio, interests)
                    }
                )
            } else {
                NextBlockMainScaffold(
                    profile = profile,
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onSignOut = { signOut(credentialManager, scope) }
                )
            }
        }
    }
}

@Composable
private fun NextBlockMainScaffold(
    profile: NeighborProfile,
    viewModel: NextBlockViewModel,
    snackbarHostState: SnackbarHostState,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    val settingsState by viewModel.privateSettingsState.collectAsStateWithLifecycle()
    val settings = (settingsState as? UiState.Success)?.data
    val radiusMiles = settings?.radiusMiles ?: 3L

    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val filteredPostsState by viewModel.filteredPostsState.collectAsStateWithLifecycle()
    val listingsState by viewModel.listingsState.collectAsStateWithLifecycle()
    val filteredListingsState by viewModel.filteredListingsState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val groupsState by viewModel.groupsState.collectAsStateWithLifecycle()
    val businessesState by viewModel.businessesState.collectAsStateWithLifecycle()
    val neighborsState by viewModel.neighborsState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val activePostCommentsState by viewModel.activePostCommentsState.collectAsStateWithLifecycle()
    val activePostThankedState by viewModel.activePostThankedState.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPostCat by viewModel.selectedPostCategory.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
    val selectedListingCat by viewModel.selectedListingCategory.collectAsStateWithLifecycle()
    val selectedPostId by viewModel.selectedPostId.collectAsStateWithLifecycle()
    val selectedConversationId by viewModel.selectedConversationId.collectAsStateWithLifecycle()

    val votedPollIds by viewModel.votedPollPostIds.collectAsStateWithLifecycle()
    val rsvpedEventIds by viewModel.rsvpedEventIds.collectAsStateWithLifecycle()
    val joinedGroupIds by viewModel.joinedGroupIds.collectAsStateWithLifecycle()
    val favedBizIds by viewModel.favedBusinessIds.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var showNeighborhoodSwitcher by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var composerInitialCategory by remember { mutableStateOf<PostCategory?>(null) }

    val unreadCount = (notificationsState as? UiState.Success)?.data?.count { !it.readFlag } ?: 0

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        primaryNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        primaryNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("rail_${item.route}")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        NavigationRailItem(
                            selected = currentRoute == ROUTE_PROFILE,
                            onClick = { navigateTab(ROUTE_PROFILE) },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == ROUTE_PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                    contentDescription = "Profile"
                                )
                            },
                            label = { Text("Profile") },
                            modifier = Modifier.testTag("rail_profile")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    composable(ROUTE_HOME) {
                        HomeFeedScreen(
                            profile = profile,
                            radiusMiles = radiusMiles,
                            postsState = filteredPostsState,
                            savedPostIds = settings?.savedPostIds ?: emptyList(),
                            votedPollPostIds = votedPollIds,
                            unreadNotificationsCount = unreadCount,
                            searchQuery = searchQuery,
                            selectedCategory = selectedPostCat,
                            selectedSort = selectedSort,
                            onSearchChange = viewModel::setSearchQuery,
                            onSelectCategory = viewModel::setPostCategory,
                            onSelectSort = viewModel::setFeedSort,
                            onOpenNeighborhoodSwitcher = { showNeighborhoodSwitcher = true },
                            onOpenNotifications = { showNotifications = true },
                            onOpenProfile = { navigateTab(ROUTE_PROFILE) },
                            onOpenComposer = { composerInitialCategory = PostCategory.GENERAL },
                            onOpenPostDetail = { post -> viewModel.selectPost(post.id) },
                            onToggleThank = viewModel::togglePostThank,
                            onToggleSavePost = viewModel::toggleSavedPost,
                            onVotePoll = viewModel::voteOnPoll
                        )
                    }

                    composable(ROUTE_DISCOVER) {
                        DiscoverScreen(
                            profile = profile,
                            radiusMiles = radiusMiles,
                            businessesState = businessesState,
                            eventsState = eventsState,
                            groupsState = groupsState,
                            neighborsState = neighborsState,
                            postsState = postsState,
                            favedBusinessIds = favedBizIds,
                            rsvpedEventIds = rsvpedEventIds,
                            joinedGroupIds = joinedGroupIds,
                            isBusy = isBusy,
                            onToggleBusinessFave = viewModel::toggleBusinessFave,
                            onCreateBusiness = viewModel::createBusiness,
                            onToggleEventRsvp = viewModel::toggleEventRsvp,
                            onCreateEvent = viewModel::createEvent,
                            onToggleGroupJoin = viewModel::toggleGroupMembership,
                            onCreateGroup = viewModel::createGroup,
                            onSelectNeighborhoodOnMap = { area ->
                                viewModel.switchNeighborhood(area.id, radiusMiles)
                            },
                            onMessageNeighbor = { neighbor ->
                                viewModel.sendMessageToNeighbor(
                                    otherUserId = neighbor.userId,
                                    otherUserName = neighbor.displayName,
                                    subject = "Neighbor Chat",
                                    text = "Hi ${neighbor.displayName}! Connecting from ${profile.neighborhoodName}."
                                ) {
                                    navigateTab(ROUTE_MESSAGES)
                                }
                            }
                        )
                    }

                    composable(ROUTE_MARKET) {
                        MarketplaceScreen(
                            profile = profile,
                            listingsState = filteredListingsState,
                            savedListingIds = settings?.savedListingIds ?: emptyList(),
                            selectedCategory = selectedListingCat,
                            searchQuery = searchQuery,
                            isBusy = isBusy,
                            onSearchChange = viewModel::setSearchQuery,
                            onSelectCategory = viewModel::setListingCategory,
                            onToggleSaveListing = viewModel::toggleSavedListing,
                            onCreateListing = viewModel::createListing,
                            onUpdateListingStatus = viewModel::updateListingStatus,
                            onDeleteListing = viewModel::deleteListing,
                            onMessageSeller = { listing ->
                                viewModel.sendMessageToNeighbor(
                                    otherUserId = listing.sellerId,
                                    otherUserName = listing.sellerName,
                                    subject = listing.title,
                                    text = "Hi ${listing.sellerName}, is \"${listing.title}\" still available?"
                                ) {
                                    navigateTab(ROUTE_MESSAGES)
                                }
                            }
                        )
                    }

                    composable(ROUTE_SAFETY) {
                        SafetyHubScreen(
                            profile = profile,
                            radiusMiles = radiusMiles,
                            postsState = postsState,
                            savedPostIds = settings?.savedPostIds ?: emptyList(),
                            votedPollPostIds = votedPollIds,
                            onSelectNeighborhoodOnMap = { area ->
                                viewModel.switchNeighborhood(area.id, radiusMiles)
                            },
                            onPostSafetyAlert = { composerInitialCategory = PostCategory.SAFETY },
                            onOpenPostDetail = { post -> viewModel.selectPost(post.id) },
                            onToggleThank = viewModel::togglePostThank,
                            onToggleSavePost = viewModel::toggleSavedPost,
                            onVotePoll = viewModel::voteOnPoll
                        )
                    }

                    composable(ROUTE_MESSAGES) {
                        MessagesScreen(
                            currentProfile = profile,
                            conversationsState = conversationsState,
                            selectedConversationId = selectedConversationId,
                            activeMessagesState = activeMessagesState,
                            neighborsState = neighborsState,
                            onSelectConversation = viewModel::selectConversation,
                            onSendMessage = { uid, name, subject, text ->
                                viewModel.sendMessageToNeighbor(uid, name, subject, text)
                            }
                        )
                    }

                    composable(ROUTE_PROFILE) {
                        ProfileScreen(
                            profile = profile,
                            settings = settings,
                            postsState = postsState,
                            listingsState = listingsState,
                            isBusy = isBusy,
                            onOpenNeighborhoodSwitcher = { showNeighborhoodSwitcher = true },
                            onUpdateProfile = viewModel::updateProfileDetails,
                            onUpdatePreferences = viewModel::updatePreferences,
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }

    if (showNeighborhoodSwitcher) {
        NeighborhoodSwitcherSheet(
            currentNeighborhoodId = profile.neighborhoodId,
            currentRadiusMiles = radiusMiles,
            onDismiss = { showNeighborhoodSwitcher = false },
            onApply = viewModel::switchNeighborhood
        )
    }

    if (showNotifications) {
        NotificationsSheet(
            notificationsState = notificationsState,
            onMarkRead = viewModel::markNotificationRead,
            onDismiss = { showNotifications = false }
        )
    }

    val initialCat = composerInitialCategory
    if (initialCat != null) {
        CreatePostSheet(
            neighborhoodName = profile.neighborhoodName,
            initialCategory = initialCat,
            isBusy = isBusy,
            onDismiss = { composerInitialCategory = null },
            onSubmit = { cat, scope, title, body, loc, photoUri, pollOpts, urgent ->
                viewModel.createPost(
                    category = cat,
                    scope = scope,
                    title = title,
                    body = body,
                    locationTag = loc,
                    photoUri = photoUri,
                    pollOptions = pollOpts,
                    pinnedUrgent = urgent
                ) {
                    composerInitialCategory = null
                }
            }
        )
    }

    val activePostId = selectedPostId
    if (activePostId != null) {
        val activePost = (postsState as? UiState.Success)?.data?.firstOrNull { it.id == activePostId }
        if (activePost != null) {
            PostDetailSheet(
                post = activePost,
                currentUserId = profile.userId,
                commentsState = activePostCommentsState,
                thankedState = activePostThankedState,
                onDismiss = { viewModel.selectPost(null) },
                onToggleThank = { viewModel.togglePostThank(activePost) },
                onAddComment = { text, onDone -> viewModel.addComment(activePost, text, onDone) },
                onDeletePost = {
                    viewModel.deletePost(activePost.id) { viewModel.selectPost(null) }
                },
                onMessageAuthor = {
                    viewModel.selectPost(null)
                    viewModel.sendMessageToNeighbor(
                        otherUserId = activePost.authorId,
                        otherUserName = activePost.authorName,
                        subject = "Re: ${activePost.title}",
                        text = "Hi ${activePost.authorName}, reaching out regarding your post \"${activePost.title}\"."
                    ) {
                        navigateTab(ROUTE_MESSAGES)
                    }
                }
            )
        }
    }
}
