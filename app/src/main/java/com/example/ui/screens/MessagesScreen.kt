package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.model.DirectConversation
import com.example.model.DirectMessage
import com.example.model.NeighborProfile
import com.example.model.UiState
import com.example.model.initialsFor
import com.example.ui.components.NeighborAvatar
import com.example.ui.components.relativeTimeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    currentProfile: NeighborProfile,
    conversationsState: UiState<List<DirectConversation>>,
    selectedConversationId: String?,
    activeMessagesState: UiState<List<DirectMessage>>,
    neighborsState: UiState<List<NeighborProfile>>,
    onSelectConversation: (String?) -> Unit,
    onSendMessage: (otherUserId: String, otherUserName: String, subject: String, text: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewChatPicker by remember { mutableStateOf(false) }

    val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val activeConv = conversations.firstOrNull { it.id == selectedConversationId }

    if (activeConv != null) {
        BackHandler { onSelectConversation(null) }
        val otherIdx = activeConv.participantUids.indexOfFirst { it != currentProfile.userId }.takeIf { it >= 0 } ?: 0
        val otherUid = activeConv.participantUids.getOrElse(otherIdx) { "" }
        val otherName = activeConv.participantNames.getOrElse(otherIdx) { "Neighbor" }

        ChatDetailScreen(
            currentUserId = currentProfile.userId,
            otherUserName = otherName,
            subject = activeConv.subject,
            messagesState = activeMessagesState,
            onBack = { onSelectConversation(null) },
            onSend = { text ->
                if (otherUid.isNotBlank()) {
                    onSendMessage(otherUid, otherName, activeConv.subject.ifBlank { "Neighbor Chat" }, text)
                }
            },
            modifier = modifier
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewChatPicker = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New Chat") },
                modifier = Modifier.testTag("fab_new_message")
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
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("Neighbor Messages", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Direct 1-on-1 conversations with verified neighbors",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                when (conversationsState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is UiState.Error -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) { Text(conversationsState.message, color = MaterialTheme.colorScheme.error) }
                    is UiState.Success -> {
                        if (conversations.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("No Messages Yet", style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Tap New Chat to message a neighbor, or tap Message Seller on any Marketplace item.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(conversations, key = { it.id }) { conv ->
                                    val otherIdx = conv.participantUids.indexOfFirst { it != currentProfile.userId }.takeIf { it >= 0 } ?: 0
                                    val otherName = conv.participantNames.getOrElse(otherIdx) { "Neighbor" }
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("conversation_card_${conv.id}")
                                            .clickable { onSelectConversation(conv.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            NeighborAvatar(initialsFor(otherName), 0xFF157A8CL, size = 44.dp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(otherName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        relativeTimeLabel(conv.updatedAt ?: conv.createdAt),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (conv.subject.isNotBlank()) {
                                                    Text(conv.subject, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Text(
                                                    conv.lastMessageText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
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
    }

    if (showNewChatPicker) {
        val neighbors = (neighborsState as? UiState.Success)?.data.orEmpty().filter { it.userId != currentProfile.userId }
        ModalBottomSheet(onDismissRequest = { showNewChatPicker = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Start a Conversation", style = MaterialTheme.typography.headlineSmall)
                if (neighbors.isEmpty()) {
                    Text(
                        "No other verified neighbors in ${currentProfile.neighborhoodName} yet. When another neighbor signs in, they will appear here!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(neighbors, key = { it.userId }) { neighbor ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showNewChatPicker = false
                                        onSendMessage(
                                            neighbor.userId,
                                            neighbor.displayName,
                                            "Neighbor Chat",
                                            "Hi ${neighbor.displayName}! Neighbor from ${currentProfile.neighborhoodName} here."
                                        )
                                    }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    NeighborAvatar(neighbor.avatarInitials, neighbor.avatarColorHex)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(neighbor.displayName, style = MaterialTheme.typography.titleSmall)
                                        Text(neighbor.neighborhoodName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun ChatDetailScreen(
    currentUserId: String,
    otherUserName: String,
    subject: String,
    messagesState: UiState<List<DirectMessage>>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to messages")
                }
                NeighborAvatar(initialsFor(otherUserName), 0xFF157A8CL, size = 38.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(otherUserName, style = MaterialTheme.typography.titleMedium)
                    if (subject.isNotBlank()) {
                        Text(subject, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (messagesState) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Error -> Text(messagesState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                is UiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(messagesState.data, key = { it.id }) { msg ->
                            val isMine = msg.senderId == currentUserId
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = if (isMine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.widthIn(max = 290.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                        Text(
                                            msg.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            relativeTimeLabel(msg.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 1000) draft = it },
                    placeholder = { Text("Message $otherUserName...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("chat_message_input")
                )
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onSend(draft)
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("send_chat_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (draft.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
