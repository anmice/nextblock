package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.MarketplaceListing
import com.example.model.NeighborPrivateSettings
import com.example.model.NeighborProfile
import com.example.model.NeighborhoodCatalog
import com.example.model.NeighborhoodPost
import com.example.model.UiState
import com.example.ui.components.NeighborAvatar
import com.example.ui.components.VerifiedNeighborPill
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: NeighborProfile,
    settings: NeighborPrivateSettings?,
    postsState: UiState<List<NeighborhoodPost>>,
    listingsState: UiState<List<MarketplaceListing>>,
    isBusy: Boolean,
    onOpenNeighborhoodSwitcher: () -> Unit,
    onUpdateProfile: (String, String, String, List<String>) -> Unit,
    onUpdatePreferences: (Boolean, Boolean, Long) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditSheet by remember { mutableStateOf(false) }

    val allPosts = (postsState as? UiState.Success)?.data.orEmpty()
    val myPostsCount = allPosts.count { it.authorId == profile.userId }
    val savedPosts = allPosts.filter { it.id in (settings?.savedPostIds ?: emptyList()) }
    val allListings = (listingsState as? UiState.Success)?.data.orEmpty()
    val myListingsCount = allListings.count { it.sellerId == profile.userId }

    var alertsEnabled by remember(settings?.alertsEnabled) { mutableStateOf(settings?.alertsEnabled ?: true) }
    var digestEnabled by remember(settings?.digestEnabled) { mutableStateOf(settings?.digestEnabled ?: true) }
    var radiusFloat by remember(settings?.radiusMiles) { mutableFloatStateOf((settings?.radiusMiles ?: 3L).toFloat()) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NeighborAvatar(profile.avatarInitials, profile.avatarColorHex, size = 60.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(profile.displayName, style = MaterialTheme.typography.titleLarge)
                                    VerifiedNeighborPill()
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        "${profile.neighborhoodName}${settings?.streetName?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (profile.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(profile.bio, style = MaterialTheme.typography.bodyMedium)
                        }

                        if (profile.interests.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profile.interests.forEach { tag ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text(
                                            tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStat("$myPostsCount", "Posts")
                            ProfileStat("$myListingsCount", "Listings")
                            ProfileStat("${savedPosts.size}", "Bookmarks")
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showEditSheet = true },
                                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("edit_profile_button")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile")
                            }
                            Button(
                                onClick = onOpenNeighborhoodSwitcher,
                                modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("switch_neighborhood_profile_button")
                            ) {
                                Text("Switch Block")
                            }
                        }
                    }
                }
            }

            // Preferences Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Neighborhood Settings", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Urgent Safety Alerts", style = MaterialTheme.typography.titleSmall)
                                Text("Receive immediate safety and lost pet notices", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = alertsEnabled,
                                onCheckedChange = {
                                    alertsEnabled = it
                                    onUpdatePreferences(it, digestEnabled, radiusFloat.roundToLong())
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily Neighborhood Digest", style = MaterialTheme.typography.titleSmall)
                                Text("Summary of top recommendations & events", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = digestEnabled,
                                onCheckedChange = {
                                    digestEnabled = it
                                    onUpdatePreferences(alertsEnabled, it, radiusFloat.roundToLong())
                                }
                            )
                        }
                        Column {
                            Text("Feed Radius: ${radiusFloat.roundToLong()} miles", style = MaterialTheme.typography.titleSmall)
                            Slider(
                                value = radiusFloat,
                                onValueChange = { radiusFloat = it },
                                onValueChangeFinished = {
                                    onUpdatePreferences(alertsEnabled, digestEnabled, radiusFloat.roundToLong())
                                },
                                valueRange = 1f..15f,
                                steps = 13
                            )
                        }
                    }
                }
            }

            if (savedPosts.isNotEmpty()) {
                item {
                    Text("Bookmarked Posts", style = MaterialTheme.typography.titleMedium)
                }
                items(savedPosts, key = { it.id }) { post ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(post.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${post.authorName} • ${post.neighborhoodName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showEditSheet) {
        var editName by remember { mutableStateOf(profile.displayName) }
        var editBio by remember { mutableStateOf(profile.bio) }
        var editStreet by remember { mutableStateOf(settings?.streetName.orEmpty()) }
        var editInterests by remember { mutableStateOf(profile.interests.toSet()) }

        ModalBottomSheet(onDismissRequest = { showEditSheet = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Edit Neighbor Profile", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = editName, onValueChange = { if (it.length <= 60) editName = it }, label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = editStreet, onValueChange = { if (it.length <= 100) editStreet = it }, label = { Text("Street (private)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = editBio, onValueChange = { if (it.length <= 240) editBio = it }, label = { Text("Bio") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeighborhoodCatalog.interestOptions.forEach { tag ->
                        FilterChip(
                            selected = tag in editInterests,
                            onClick = { editInterests = if (tag in editInterests) editInterests - tag else editInterests + tag },
                            label = { Text(tag) }
                        )
                    }
                }
                Button(
                    onClick = {
                        onUpdateProfile(editName, editBio, editStreet, editInterests.toList())
                        showEditSheet = false
                    },
                    enabled = editName.isNotBlank() && !isBusy,
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
