package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.NeighborNotification
import com.example.model.NeighborhoodCatalog
import com.example.model.NeighborhoodPost
import com.example.model.PostCategory
import com.example.model.PostComment
import com.example.model.PostScope
import com.example.model.UiState
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeighborhoodSwitcherSheet(
    currentNeighborhoodId: String,
    currentRadiusMiles: Long,
    onDismiss: () -> Unit,
    onApply: (String, Long) -> Unit
) {
    var selectedId by remember(currentNeighborhoodId) { mutableStateOf(currentNeighborhoodId) }
    var radiusFloat by remember(currentRadiusMiles) { mutableFloatStateOf(currentRadiusMiles.toFloat()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Your Neighborhood & Radius",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Switch your primary block or expand how far you see posts and deals.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            NeighborhoodCatalog.areas.forEach { area ->
                val isSelected = area.id == selectedId
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("neighborhood_option_${area.id}")
                        .clickable { selectedId = area.id }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(area.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${area.cityLabel} • ${area.landmarks}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Discovery Radius: ${radiusFloat.roundToLong()} miles",
                style = MaterialTheme.typography.titleSmall
            )
            Slider(
                value = radiusFloat,
                onValueChange = { radiusFloat = it },
                valueRange = 1f..15f,
                steps = 13,
                modifier = Modifier.testTag("radius_slider")
            )

            Button(
                onClick = {
                    onApply(selectedId, radiusFloat.roundToLong())
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("apply_neighborhood_button")
            ) {
                Text("Update Neighborhood")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    notificationsState: UiState<List<NeighborNotification>>,
    onMarkRead: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Neighborhood Activity", style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close notifications")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            when (notificationsState) {
                is UiState.Loading -> Text("Loading notifications...", style = MaterialTheme.typography.bodyMedium)
                is UiState.Error -> Text(notificationsState.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    if (notificationsState.data.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("You're all caught up!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(notificationsState.data, key = { it.id }) { notif ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (notif.readFlag) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onMarkRead(notif.id) }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(notif.title, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                relativeTimeLabel(notif.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            notif.body,
                                            style = MaterialTheme.typography.bodySmall,
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostSheet(
    neighborhoodName: String,
    initialCategory: PostCategory = PostCategory.GENERAL,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        category: PostCategory,
        scope: PostScope,
        title: String,
        body: String,
        locationTag: String,
        photoUri: String?,
        pollOptions: List<String>,
        pinnedUrgent: Boolean
    ) -> Unit
) {
    var category by remember { mutableStateOf(initialCategory) }
    var scope by remember { mutableStateOf(PostScope.LOCAL) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var locationTag by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var pollOpt1 by remember { mutableStateOf("") }
    var pollOpt2 by remember { mutableStateOf("") }
    var pollOpt3 by remember { mutableStateOf("") }
    var pinnedUrgent by remember { mutableStateOf(initialCategory == PostCategory.SAFETY) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) photoUri = uri }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("New Post", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Posting to $neighborhoodName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close composer")
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PostCategory.entries.forEach { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text(item.label) },
                        modifier = Modifier.testTag("compose_cat_${item.name.lowercase()}")
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PostScope.entries.forEach { s ->
                    FilterChip(
                        selected = scope == s,
                        onClick = { scope = s },
                        label = { Text(s.label) }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 140) title = it },
                label = { Text("Headline") },
                placeholder = { Text("What's happening in the neighborhood?") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_title_input")
            )

            OutlinedTextField(
                value = body,
                onValueChange = { if (it.length <= 2000) body = it },
                label = { Text("Details") },
                placeholder = { Text("Share helpful details, context, or questions...") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_body_input")
            )

            OutlinedTextField(
                value = locationTag,
                onValueChange = { if (it.length <= 100) locationTag = it },
                label = { Text("Cross street or landmark (optional)") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_location_input")
            )

            if (category == PostCategory.SAFETY) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mark as Urgent Safety Alert", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Highlights this alert at the top of the Safety Hub",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = pinnedUrgent,
                        onCheckedChange = { pinnedUrgent = it }
                    )
                }
            }

            if (category == PostCategory.POLL) {
                Text("Poll Options (up to 3)", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = pollOpt1,
                    onValueChange = { pollOpt1 = it },
                    label = { Text("Option 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pollOpt2,
                    onValueChange = { pollOpt2 = it },
                    label = { Text("Option 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pollOpt3,
                    onValueChange = { pollOpt3 = it },
                    label = { Text("Option 3 (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedButton(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("pick_post_photo_button")
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (photoUri == null) "Attach Photo from Library" else "Change Selected Photo")
            }

            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Selected post photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }

            val canSubmit = title.isNotBlank() && body.isNotBlank() && !isBusy &&
                (category != PostCategory.POLL || (pollOpt1.isNotBlank() && pollOpt2.isNotBlank()))

            Button(
                onClick = {
                    onSubmit(
                        category,
                        scope,
                        title,
                        body,
                        locationTag,
                        photoUri?.toString(),
                        listOf(pollOpt1, pollOpt2, pollOpt3),
                        pinnedUrgent
                    )
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("publish_post_button")
            ) {
                Text(if (isBusy) "Publishing..." else "Post to Neighborhood")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailSheet(
    post: NeighborhoodPost,
    currentUserId: String,
    commentsState: UiState<List<PostComment>>,
    thankedState: UiState<Boolean>,
    onDismiss: () -> Unit,
    onToggleThank: () -> Unit,
    onAddComment: (String, () -> Unit) -> Unit,
    onDeletePost: () -> Unit,
    onMessageAuthor: () -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    val hasThanked = (thankedState as? UiState.Success)?.data ?: false

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeighborAvatar(post.authorInitials, post.authorAvatarColor)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.authorName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${post.neighborhoodName} • ${relativeTimeLabel(post.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (post.authorId == currentUserId) {
                    IconButton(
                        onClick = onDeletePost,
                        modifier = Modifier.testTag("delete_post_button")
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete post", tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    TextButton(
                        onClick = onMessageAuthor,
                        modifier = Modifier.testTag("message_author_button")
                    ) {
                        Text("Message")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            CategoryBadge(category = PostCategory.fromRaw(post.category))
            Spacer(modifier = Modifier.height(8.dp))
            Text(post.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(6.dp))
            Text(post.body, style = MaterialTheme.typography.bodyLarge)

            if (!post.photoUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = post.photoUri,
                    contentDescription = post.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleThank,
                    modifier = Modifier.testTag("detail_thank_button")
                ) {
                    Text(if (hasThanked) "Thanked (${post.thanksCount})" else "Thank Neighbor (${post.thanksCount})")
                }
                Text(
                    "${post.commentsCount} replies",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Text("Neighbor Replies", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            when (commentsState) {
                is UiState.Loading -> Text("Loading replies...", style = MaterialTheme.typography.bodySmall)
                is UiState.Error -> Text(commentsState.message, color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    if (commentsState.data.isEmpty()) {
                        Text(
                            "No replies yet. Be the first neighbor to respond!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(commentsState.data, key = { it.id }) { comment ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                        .padding(10.dp)
                                ) {
                                    NeighborAvatar(comment.authorInitials, comment.authorAvatarColor, size = 32.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                comment.authorName,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                relativeTimeLabel(comment.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(comment.body, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { if (it.length <= 1000) commentText = it },
                    placeholder = { Text("Write a helpful reply...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input")
                )
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onAddComment(commentText) { commentText = "" }
                        }
                    },
                    enabled = commentText.isNotBlank(),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("send_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Post reply",
                        tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
