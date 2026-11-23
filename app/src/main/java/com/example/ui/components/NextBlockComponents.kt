package com.example.ui.components

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.NeighborhoodArea
import com.example.model.NeighborhoodCatalog
import com.example.model.NeighborhoodEvent
import com.example.model.NeighborhoodPost
import com.example.model.PostCategory
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonTint
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.IndigoEvent
import com.example.ui.theme.IndigoTint
import com.example.ui.theme.SageTint
import com.example.ui.theme.TealFound
import com.example.ui.theme.TealTint
import com.example.ui.theme.TerracottaAmber
import com.example.ui.theme.WarmSand
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.hypot

fun relativeTimeLabel(timestamp: Timestamp?): String {
    val ts = timestamp ?: return "Just now"
    val diffSec = (System.currentTimeMillis() / 1000L) - ts.seconds
    return when {
        diffSec < 60 -> "Just now"
        diffSec < 3600 -> "${diffSec / 60}m ago"
        diffSec < 86400 -> "${diffSec / 3600}h ago"
        diffSec < 604800 -> "${diffSec / 86400}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(ts.toDate())
    }
}

@Composable
fun NeighborAvatar(
    initials: String,
    colorHex: Long,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(colorHex))
            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.take(2).uppercase(),
            style = if (size >= 48.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun VerifiedNeighborPill(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = "Verified neighbor",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "Verified",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

fun categoryStyle(category: PostCategory): Triple<ImageVector, Color, Color> = when (category) {
    PostCategory.GENERAL -> Triple(Icons.Filled.Campaign, ForestGreen, SageTint)
    PostCategory.SAFETY -> Triple(Icons.Filled.Shield, CrimsonAlert, CrimsonTint)
    PostCategory.RECOMMENDATION -> Triple(Icons.Filled.ThumbUp, TerracottaAmber, WarmSand)
    PostCategory.LOST_FOUND -> Triple(Icons.Filled.Pets, TealFound, TealTint)
    PostCategory.EVENT -> Triple(Icons.Filled.Event, IndigoEvent, IndigoTint)
    PostCategory.POLL -> Triple(Icons.Filled.Poll, ForestGreen, SageTint)
}

@Composable
fun CategoryBadge(category: PostCategory, modifier: Modifier = Modifier) {
    val (icon, fg, bg) = categoryStyle(category)
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = category.label,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = category.label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PostCard(
    post: NeighborhoodPost,
    isSaved: Boolean,
    hasVoted: Boolean,
    onCardClick: () -> Unit,
    onThankClick: () -> Unit,
    onSaveClick: () -> Unit,
    onVoteOption: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val category = PostCategory.fromRaw(post.category)
    val isUrgent = post.pinnedUrgent && category == PostCategory.SAFETY

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}")
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isUrgent) 1.5.dp else 1.dp,
            color = if (isUrgent) CrimsonAlert.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isUrgent) {
                Surface(
                    color = CrimsonTint,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Urgent alert",
                            tint = CrimsonAlert,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Urgent Neighborhood Safety Alert",
                            style = MaterialTheme.typography.labelMedium,
                            color = CrimsonAlert
                        )
                    }
                }
            }

            // Author row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeighborAvatar(
                    initials = post.authorInitials,
                    colorHex = post.authorAvatarColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        VerifiedNeighborPill()
                    }
                    Text(
                        text = "${post.neighborhoodName} • ${relativeTimeLabel(post.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CategoryBadge(category = category)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = post.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            if (post.locationTag.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Location tag",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = post.locationTag,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!post.photoUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = post.photoUri,
                    contentDescription = "Post photo for ${post.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            // Poll options widget
            if (category == PostCategory.POLL && post.pollOptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                val totalVotes = post.pollVotes.sum().coerceAtLeast(1L)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        post.pollOptions.forEachIndexed { idx, option ->
                            val votes = post.pollVotes.getOrElse(idx) { 0L }
                            val fraction = if (post.pollVotes.sum() > 0L) votes.toFloat() / totalVotes.toFloat() else 0f
                            val pct = (fraction * 100).toInt()
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("poll_option_${post.id}_$idx")
                                    .clickable { onVoteOption(idx) }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = option,
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = if (hasVoted || post.pollVotes.sum() > 0L) "$pct% ($votes)" else "Vote",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (post.pollVotes.sum() > 0L) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(4.dp))

            // Reactions bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .minimumInteractiveComponentSize()
                        .testTag("thank_button_${post.id}")
                        .clickable(onClick = onThankClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (post.thanksCount > 0) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Thank neighbor",
                        tint = if (post.thanksCount > 0) CrimsonAlert else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Thank (${post.thanksCount})",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (post.thanksCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .minimumInteractiveComponentSize()
                        .testTag("comment_button_${post.id}")
                        .clickable(onClick = onCardClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Open comments",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.commentsCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSaveClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("save_post_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isSaved) "Remove bookmark" else "Bookmark post",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, post.title)
                                putExtra(Intent.EXTRA_TEXT, "[${post.neighborhoodName}] ${post.title}\n\n${post.body}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share neighborhood post"))
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("share_post_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share post",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NeighborhoodMapCanvas(
    selectedNeighborhoodId: String,
    radiusMiles: Long,
    safetyPostCount: Int,
    eventsCount: Int,
    onSelectNeighborhood: (NeighborhoodArea) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeArea = NeighborhoodCatalog.byId(selectedNeighborhoodId)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val roadColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${activeArea.name} Boundary",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${activeArea.cityLabel} • ${radiusMiles} mi radius • Tap a district pin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "$safetyPostCount alerts • $eventsCount events",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFEAF3EC),
                                Color(0xFFF3EFE4),
                                Color(0xFFE4EEF0)
                            )
                        )
                    )
                    .pointerInput(selectedNeighborhoodId) {
                        detectTapGestures { tap ->
                            val tapped = NeighborhoodCatalog.areas.minByOrNull { area ->
                                val ax = area.mapXFraction * size.width
                                val ay = area.mapYFraction * size.height
                                hypot(tap.x - ax, tap.y - ay)
                            }
                            if (tapped != null) onSelectNeighborhood(tapped)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Parkland & creek shapes
                    drawCircle(
                        color = Color(0xFFCDE5D4),
                        radius = w * 0.14f,
                        center = Offset(w * 0.72f, h * 0.34f)
                    )
                    drawCircle(
                        color = Color(0xFFD2E8EC),
                        radius = w * 0.16f,
                        center = Offset(w * 0.60f, h * 0.80f)
                    )

                    // Street grid lines
                    for (i in 1..4) {
                        val y = h * (i / 5f)
                        drawLine(roadColor, Offset(0f, y), Offset(w, y), strokeWidth = 3f)
                    }
                    for (i in 1..5) {
                        val x = w * (i / 6f)
                        drawLine(roadColor, Offset(x, 0f), Offset(x, h), strokeWidth = 3f)
                    }
                    // Main diagonal avenue
                    drawLine(
                        color = Color(0xFFD6CFC2),
                        start = Offset(0f, h * 0.85f),
                        end = Offset(w, h * 0.15f),
                        strokeWidth = 7f
                    )

                    // Radius ring around active neighborhood
                    val center = Offset(activeArea.mapXFraction * w, activeArea.mapYFraction * h)
                    val radiusPx = (w * 0.12f) + (radiusMiles.coerceIn(1L, 15L) / 15f) * (w * 0.22f)
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.14f),
                        radius = radiusPx,
                        center = center
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = radiusPx,
                        center = center,
                        style = Stroke(
                            width = 3f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                        )
                    )

                    // Neighborhood pins
                    NeighborhoodCatalog.areas.forEach { area ->
                        val pt = Offset(area.mapXFraction * w, area.mapYFraction * h)
                        val isSelected = area.id == selectedNeighborhoodId
                        drawCircle(
                            color = Color.White,
                            radius = if (isSelected) 18f else 12f,
                            center = pt
                        )
                        drawCircle(
                            color = if (isSelected) primaryColor else secondaryColor,
                            radius = if (isSelected) 12f else 7f,
                            center = pt
                        )
                    }
                }

                // Subtle bottom legend pill
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = activeArea.landmarks,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyNeighborhoodState(
    @DrawableRes imageRes: Int,
    title: String,
    subtitle: String,
    ctaLabel: String,
    testTag: String,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCtaClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(testTag)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(ctaLabel)
            }
        }
    }
}
