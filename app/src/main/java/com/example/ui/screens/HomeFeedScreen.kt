package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FeedSort
import com.example.model.NeighborProfile
import com.example.model.NeighborhoodPost
import com.example.model.PostCategory
import com.example.model.UiState
import com.example.ui.components.EmptyNeighborhoodState
import com.example.ui.components.NeighborAvatar
import com.example.ui.components.PostCard

@Composable
fun HomeFeedScreen(
    profile: NeighborProfile,
    radiusMiles: Long,
    postsState: UiState<List<NeighborhoodPost>>,
    savedPostIds: List<String>,
    votedPollPostIds: Set<String>,
    unreadNotificationsCount: Int,
    searchQuery: String,
    selectedCategory: PostCategory?,
    selectedSort: FeedSort,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (PostCategory?) -> Unit,
    onSelectSort: (FeedSort) -> Unit,
    onOpenNeighborhoodSwitcher: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenComposer: () -> Unit,
    onOpenPostDetail: (NeighborhoodPost) -> Unit,
    onToggleThank: (NeighborhoodPost) -> Unit,
    onToggleSavePost: (String) -> Unit,
    onVotePoll: (NeighborhoodPost, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearchBar by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenComposer,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Post") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("fab_create_post")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
            ) {
                // Top Header Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .minimumInteractiveComponentSize()
                                    .testTag("neighborhood_selector_chip")
                                    .clickable(onClick = onOpenNeighborhoodSwitcher)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = "Neighborhood",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${profile.neighborhoodName} • ${radiusMiles}mi",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Switch neighborhood",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            IconButton(
                                onClick = { showSearchBar = !showSearchBar },
                                modifier = Modifier.testTag("toggle_search_button")
                            ) {
                                Icon(
                                    imageVector = if (showSearchBar) Icons.Filled.Close else Icons.Filled.Search,
                                    contentDescription = "Search feed"
                                )
                            }

                            IconButton(
                                onClick = onOpenNotifications,
                                modifier = Modifier.testTag("open_notifications_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotificationsCount > 0) {
                                            Badge { Text("$unreadNotificationsCount") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Notifications,
                                        contentDescription = "Notifications"
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))
                            NeighborAvatar(
                                initials = profile.avatarInitials,
                                colorHex = profile.avatarColorHex,
                                size = 36.dp,
                                modifier = Modifier
                                    .testTag("header_profile_avatar")
                                    .clickable(onClick = onOpenProfile)
                            )
                        }

                        if (showSearchBar) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchChange,
                                placeholder = { Text("Search posts, streets, or neighbors...") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("feed_search_input")
                            )
                        }
                    }
                }

                // Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text("All") },
                        modifier = Modifier.testTag("filter_all")
                    )
                    PostCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onSelectCategory(if (selectedCategory == cat) null else cat) },
                            label = { Text(cat.label) },
                            modifier = Modifier.testTag("filter_${cat.name.lowercase()}")
                        )
                    }
                }

                // Sort Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sort:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FeedSort.entries.forEach { sort ->
                        val active = selectedSort == sort
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .testTag("sort_${sort.name.lowercase()}")
                                .clickable { onSelectSort(sort) }
                        ) {
                            Text(
                                text = sort.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Feed Content
                when (postsState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = postsState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    is UiState.Success -> {
                        if (postsState.data.isEmpty()) {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    EmptyNeighborhoodState(
                                        imageRes = R.drawable.img_neighborhood_hero,
                                        title = "Say hello to ${profile.neighborhoodName}",
                                        subtitle = "No posts match this filter yet. Share a local tip, ask for a recommendation, or start a poll!",
                                        ctaLabel = "Write First Post",
                                        testTag = "empty_create_post_button",
                                        onCtaClick = onOpenComposer
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 92.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(postsState.data, key = { it.id }) { post ->
                                    PostCard(
                                        post = post,
                                        isSaved = post.id in savedPostIds,
                                        hasVoted = post.id in votedPollPostIds,
                                        onCardClick = { onOpenPostDetail(post) },
                                        onThankClick = { onToggleThank(post) },
                                        onSaveClick = { onToggleSavePost(post.id) },
                                        onVoteOption = { idx -> onVotePoll(post, idx) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
