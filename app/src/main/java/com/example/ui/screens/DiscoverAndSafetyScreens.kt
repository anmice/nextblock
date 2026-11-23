package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BusinessCategory
import com.example.model.EventCategory
import com.example.model.GroupCategory
import com.example.model.LocalBusiness
import com.example.model.NeighborProfile
import com.example.model.NeighborhoodArea
import com.example.model.NeighborhoodEvent
import com.example.model.NeighborhoodGroup
import com.example.model.NeighborhoodPost
import com.example.model.PostCategory
import com.example.model.UiState
import com.example.ui.components.EmptyNeighborhoodState
import com.example.ui.components.NeighborAvatar
import com.example.ui.components.NeighborhoodMapCanvas
import com.example.ui.components.PostCard
import com.example.ui.components.VerifiedNeighborPill
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonTint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    profile: NeighborProfile,
    radiusMiles: Long,
    businessesState: UiState<List<LocalBusiness>>,
    eventsState: UiState<List<NeighborhoodEvent>>,
    groupsState: UiState<List<NeighborhoodGroup>>,
    neighborsState: UiState<List<NeighborProfile>>,
    postsState: UiState<List<NeighborhoodPost>>,
    favedBusinessIds: Set<String>,
    rsvpedEventIds: Set<String>,
    joinedGroupIds: Set<String>,
    isBusy: Boolean,
    onToggleBusinessFave: (LocalBusiness) -> Unit,
    onCreateBusiness: (String, BusinessCategory, String, String, String, () -> Unit) -> Unit,
    onToggleEventRsvp: (NeighborhoodEvent) -> Unit,
    onCreateEvent: (String, String, EventCategory, String, String, String, () -> Unit) -> Unit,
    onToggleGroupJoin: (NeighborhoodGroup) -> Unit,
    onCreateGroup: (String, String, GroupCategory, () -> Unit) -> Unit,
    onSelectNeighborhoodOnMap: (NeighborhoodArea) -> Unit,
    onMessageNeighbor: (NeighborProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateBusiness by remember { mutableStateOf(false) }
    var showCreateEvent by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }
    val tabs = listOf("Businesses", "Events", "Groups", "Neighbors")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedTab < 3) {
                ExtendedFloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            0 -> showCreateBusiness = true
                            1 -> showCreateEvent = true
                            2 -> showCreateGroup = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = {
                        Text(
                            when (selectedTab) {
                                0 -> "Recommend"
                                1 -> "New Event"
                                else -> "New Group"
                            }
                        )
                    },
                    modifier = Modifier.testTag("discover_fab")
                )
            }
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
                    .widthIn(max = 760.dp)
            ) {
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                    Column {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text("Discover ${profile.neighborhoodName}", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Local favorites, block events, groups, and neighbors",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SecondaryTabRow(selectedTabIndex = selectedTab) {
                            tabs.forEachIndexed { idx, label ->
                                Tab(
                                    selected = selectedTab == idx,
                                    onClick = { selectedTab = idx },
                                    text = { Text(label) },
                                    modifier = Modifier.testTag("discover_tab_${label.lowercase()}")
                                )
                            }
                        }
                    }
                }

                when (selectedTab) {
                    0 -> BusinessesTab(
                        neighborhoodName = profile.neighborhoodName,
                        state = businessesState,
                        favedIds = favedBusinessIds,
                        onToggleFave = onToggleBusinessFave,
                        onOpenCreate = { showCreateBusiness = true }
                    )
                    1 -> EventsTab(
                        neighborhoodName = profile.neighborhoodName,
                        state = eventsState,
                        rsvpedIds = rsvpedEventIds,
                        onToggleRsvp = onToggleEventRsvp,
                        onOpenCreate = { showCreateEvent = true }
                    )
                    2 -> GroupsTab(
                        neighborhoodName = profile.neighborhoodName,
                        state = groupsState,
                        joinedIds = joinedGroupIds,
                        onToggleJoin = onToggleGroupJoin,
                        onOpenCreate = { showCreateGroup = true }
                    )
                    3 -> NeighborsAndMapTab(
                        profile = profile,
                        radiusMiles = radiusMiles,
                        neighborsState = neighborsState,
                        safetyCount = (postsState as? UiState.Success)?.data?.count { it.category == PostCategory.SAFETY.name } ?: 0,
                        eventsCount = (eventsState as? UiState.Success)?.data?.size ?: 0,
                        onSelectNeighborhood = onSelectNeighborhoodOnMap,
                        onMessageNeighbor = onMessageNeighbor
                    )
                }
            }
        }
    }

    if (showCreateBusiness) {
        CreateBusinessSheet(
            isBusy = isBusy,
            onDismiss = { showCreateBusiness = false },
            onSubmit = { name, cat, summary, addr, phone ->
                onCreateBusiness(name, cat, summary, addr, phone) { showCreateBusiness = false }
            }
        )
    }
    if (showCreateEvent) {
        CreateEventSheet(
            isBusy = isBusy,
            onDismiss = { showCreateEvent = false },
            onSubmit = { title, desc, cat, date, time, loc ->
                onCreateEvent(title, desc, cat, date, time, loc) { showCreateEvent = false }
            }
        )
    }
    if (showCreateGroup) {
        CreateGroupSheet(
            isBusy = isBusy,
            onDismiss = { showCreateGroup = false },
            onSubmit = { name, desc, cat ->
                onCreateGroup(name, desc, cat) { showCreateGroup = false }
            }
        )
    }
}

@Composable
private fun BusinessesTab(
    neighborhoodName: String,
    state: UiState<List<LocalBusiness>>,
    favedIds: Set<String>,
    onToggleFave: (LocalBusiness) -> Unit,
    onOpenCreate: () -> Unit
) {
    when (state) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.error) }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    item {
                        EmptyNeighborhoodState(
                            imageRes = R.drawable.img_neighborhood_hero,
                            title = "Recommend a Local Favorite",
                            subtitle = "Know a great plumber, vet, bakery, or handyperson near $neighborhoodName? Share them with neighbors!",
                            ctaLabel = "Recommend Business",
                            testTag = "empty_create_business_button",
                            onCtaClick = onOpenCreate
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.data, key = { it.id }) { biz ->
                        val cat = BusinessCategory.fromRaw(biz.category)
                        val faved = biz.id in favedIds
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Filled.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(biz.name, style = MaterialTheme.typography.titleMedium)
                                            Text(cat.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { onToggleFave(biz) },
                                        modifier = Modifier.testTag("fave_biz_${biz.id}")
                                    ) {
                                        Icon(
                                            Icons.Filled.Favorite,
                                            contentDescription = "Fave business",
                                            tint = if (faved) CrimsonAlert else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${biz.favesCount} Faves")
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(biz.summary, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(biz.addressLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (biz.phoneLabel.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(biz.phoneLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Recommended by ${biz.recommenderName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventsTab(
    neighborhoodName: String,
    state: UiState<List<NeighborhoodEvent>>,
    rsvpedIds: Set<String>,
    onToggleRsvp: (NeighborhoodEvent) -> Unit,
    onOpenCreate: () -> Unit
) {
    when (state) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.error) }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    item {
                        EmptyNeighborhoodState(
                            imageRes = R.drawable.img_neighborhood_hero,
                            title = "No Upcoming Events in $neighborhoodName",
                            subtitle = "Organize a block cleanup, garage sale, book swap, or weekend meetup.",
                            ctaLabel = "Host an Event",
                            testTag = "empty_create_event_button",
                            onCtaClick = onOpenCreate
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.data, key = { it.id }) { event ->
                        val cat = EventCategory.fromRaw(event.category)
                        val going = event.id in rsvpedIds
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(999.dp)) {
                                        Text(
                                            cat.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text("${event.rsvpCount} going", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(event.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${event.dateLabel} • ${event.timeLabel}", style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(event.locationLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(event.description, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { onToggleRsvp(event) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("rsvp_event_${event.id}")
                                ) {
                                    Text(if (going) "Going (Tap to Cancel)" else "RSVP Going")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupsTab(
    neighborhoodName: String,
    state: UiState<List<NeighborhoodGroup>>,
    joinedIds: Set<String>,
    onToggleJoin: (NeighborhoodGroup) -> Unit,
    onOpenCreate: () -> Unit
) {
    when (state) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.error) }
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    item {
                        EmptyNeighborhoodState(
                            imageRes = R.drawable.img_neighborhood_hero,
                            title = "Start a $neighborhoodName Group",
                            subtitle = "Bring neighbors together around gardening, pet parents, running clubs, or Buy Nothing swaps.",
                            ctaLabel = "Create Group",
                            testTag = "empty_create_group_button",
                            onCtaClick = onOpenCreate
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.data, key = { it.id }) { group ->
                        val cat = GroupCategory.fromRaw(group.category)
                        val joined = group.id in joinedIds
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(group.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${cat.label} • ${group.memberCount} neighbors",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(group.description, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                OutlinedButton(
                                    onClick = { onToggleJoin(group) },
                                    modifier = Modifier.testTag("join_group_${group.id}")
                                ) {
                                    Text(if (joined) "Joined" else "Join")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NeighborsAndMapTab(
    profile: NeighborProfile,
    radiusMiles: Long,
    neighborsState: UiState<List<NeighborProfile>>,
    safetyCount: Int,
    eventsCount: Int,
    onSelectNeighborhood: (NeighborhoodArea) -> Unit,
    onMessageNeighbor: (NeighborProfile) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NeighborhoodMapCanvas(
                selectedNeighborhoodId = profile.neighborhoodId,
                radiusMiles = radiusMiles,
                safetyPostCount = safetyCount,
                eventsCount = eventsCount,
                onSelectNeighborhood = onSelectNeighborhood
            )
        }
        item {
            Text(
                "Verified Neighbors in ${profile.neighborhoodName}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        when (neighborsState) {
            is UiState.Loading -> item { CircularProgressIndicator() }
            is UiState.Error -> item { Text(neighborsState.message, color = MaterialTheme.colorScheme.error) }
            is UiState.Success -> {
                items(neighborsState.data, key = { it.userId }) { neighbor ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NeighborAvatar(neighbor.avatarInitials, neighbor.avatarColorHex)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(neighbor.displayName, style = MaterialTheme.typography.titleSmall)
                                    VerifiedNeighborPill()
                                }
                                if (neighbor.bio.isNotBlank()) {
                                    Text(neighbor.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (neighbor.userId != profile.userId) {
                                OutlinedButton(
                                    onClick = { onMessageNeighbor(neighbor) },
                                    modifier = Modifier.testTag("message_neighbor_${neighbor.userId}")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Chat")
                                }
                            } else {
                                Text("You", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SafetyHubScreen(
    profile: NeighborProfile,
    radiusMiles: Long,
    postsState: UiState<List<NeighborhoodPost>>,
    savedPostIds: List<String>,
    votedPollPostIds: Set<String>,
    onSelectNeighborhoodOnMap: (NeighborhoodArea) -> Unit,
    onPostSafetyAlert: () -> Unit,
    onOpenPostDetail: (NeighborhoodPost) -> Unit,
    onToggleThank: (NeighborhoodPost) -> Unit,
    onToggleSavePost: (String) -> Unit,
    onVotePoll: (NeighborhoodPost, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safetyPosts = when (postsState) {
        is UiState.Success -> postsState.data.filter {
            it.category == PostCategory.SAFETY.name || it.category == PostCategory.LOST_FOUND.name
        }
        else -> emptyList()
    }
    val urgentCount = safetyPosts.count { it.pinnedUrgent }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPostSafetyAlert,
                icon = { Icon(Icons.Filled.Shield, contentDescription = null) },
                text = { Text("Safety Alert") },
                containerColor = CrimsonAlert,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_safety_alert")
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_safety_banner),
                            contentDescription = "Neighborhood Safety Watch",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xCC1B1F23))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Surface(color = CrimsonTint, shape = RoundedCornerShape(999.dp)) {
                                Text(
                                    "$urgentCount Urgent • ${safetyPosts.size} Total Alerts",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrimsonAlert,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "${profile.neighborhoodName} Safety & Lost/Found",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                    }
                }

                item {
                    NeighborhoodMapCanvas(
                        selectedNeighborhoodId = profile.neighborhoodId,
                        radiusMiles = radiusMiles,
                        safetyPostCount = safetyPosts.size,
                        eventsCount = 0,
                        onSelectNeighborhood = onSelectNeighborhoodOnMap
                    )
                }

                if (postsState is UiState.Loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (safetyPosts.isEmpty()) {
                    item {
                        EmptyNeighborhoodState(
                            imageRes = R.drawable.img_safety_banner,
                            title = "All Quiet on the Block",
                            subtitle = "No active crime, safety, or lost pet alerts in ${profile.neighborhoodName}.",
                            ctaLabel = "Post Safety Update",
                            testTag = "empty_safety_post_button",
                            onCtaClick = onPostSafetyAlert
                        )
                    }
                } else {
                    items(safetyPosts, key = { it.id }) { post ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBusinessSheet(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, BusinessCategory, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(BusinessCategory.HOME_SERVICES) }
    var summary by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Recommend a Local Business", style = MaterialTheme.typography.headlineSmall)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BusinessCategory.entries.forEach { cat ->
                    FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.label) })
                }
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Business Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Why neighbors love them") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Street / Area") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone or Website (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { onSubmit(name, category, summary, address, phone) },
                enabled = name.isNotBlank() && summary.isNotBlank() && address.isNotBlank() && !isBusy,
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
            ) {
                Text(if (isBusy) "Saving..." else "Add Recommendation")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateEventSheet(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, EventCategory, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(EventCategory.BLOCK_PARTY) }
    var dateLabel by remember { mutableStateOf("Sat, Nov 29") }
    var timeLabel by remember { mutableStateOf("10:00 AM") }
    var locationLabel by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Host a Neighborhood Event", style = MaterialTheme.typography.headlineSmall)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventCategory.entries.forEach { cat ->
                    FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.label) })
                }
            }
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = dateLabel, onValueChange = { dateLabel = it }, label = { Text("Date") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = timeLabel, onValueChange = { timeLabel = it }, label = { Text("Time") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = locationLabel, onValueChange = { locationLabel = it }, label = { Text("Meeting Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("What to expect") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { onSubmit(title, description, category, dateLabel, timeLabel, locationLabel) },
                enabled = title.isNotBlank() && description.isNotBlank() && locationLabel.isNotBlank() && !isBusy,
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
            ) {
                Text(if (isBusy) "Creating..." else "Publish Event")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupSheet(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, GroupCategory) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(GroupCategory.GARDENING) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Start a Neighborhood Group", style = MaterialTheme.typography.headlineSmall)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupCategory.entries.forEach { cat ->
                    FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.label) })
                }
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description & Charter") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { onSubmit(name, description, category) },
                enabled = name.isNotBlank() && description.isNotBlank() && !isBusy,
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
            ) {
                Text(if (isBusy) "Creating..." else "Create Group")
            }
        }
    }
}
