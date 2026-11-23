package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.ListingCategory
import com.example.model.ListingCondition
import com.example.model.ListingStatus
import com.example.model.MarketplaceListing
import com.example.model.NeighborProfile
import com.example.model.UiState
import com.example.ui.components.EmptyNeighborhoodState
import com.example.ui.components.NeighborAvatar
import com.example.ui.components.relativeTimeLabel
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.SageTint
import com.example.ui.theme.WarmSand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    profile: NeighborProfile,
    listingsState: UiState<List<MarketplaceListing>>,
    savedListingIds: List<String>,
    selectedCategory: ListingCategory?,
    searchQuery: String,
    isBusy: Boolean,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (ListingCategory?) -> Unit,
    onToggleSaveListing: (String) -> Unit,
    onCreateListing: (String, String, Long, ListingCategory, ListingCondition, String, String?, () -> Unit) -> Unit,
    onUpdateListingStatus: (String, ListingStatus, () -> Unit) -> Unit,
    onDeleteListing: (String, () -> Unit) -> Unit,
    onMessageSeller: (MarketplaceListing) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedListing by remember { mutableStateOf<MarketplaceListing?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateSheet = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Sell / Give") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_create_listing")
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
                    .widthIn(max = 840.dp)
            ) {
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text("For Sale & Free", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Local items in ${profile.neighborhoodName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            placeholder = { Text("Search furniture, bikes, plants...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("marketplace_search_input")
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text("All") }
                    )
                    ListingCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onSelectCategory(if (selectedCategory == cat) null else cat) },
                            label = { Text(cat.label) },
                            modifier = Modifier.testTag("market_cat_${cat.name.lowercase()}")
                        )
                    }
                }

                when (listingsState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    is UiState.Error -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) { Text(listingsState.message, color = MaterialTheme.colorScheme.error) }
                    is UiState.Success -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 172.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 92.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(132.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_marketplace_banner),
                                        contentDescription = "Neighborhood porch sale",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.horizontalGradient(listOf(Color(0xD90A4A33), Color(0x330A4A33))))
                                    )
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            "Porch Pickup Marketplace",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White
                                        )
                                        Text(
                                            "Buy, sell, or give away items right down the street.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }

                            if (listingsState.data.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyNeighborhoodState(
                                        imageRes = R.drawable.img_marketplace_banner,
                                        title = "Nothing Listed Here Yet",
                                        subtitle = "Clear out your garage or offer something free to a neighbor in ${profile.neighborhoodName}.",
                                        ctaLabel = "List First Item",
                                        testTag = "empty_create_listing_button",
                                        onCtaClick = { showCreateSheet = true }
                                    )
                                }
                            } else {
                                items(listingsState.data, key = { it.id }) { listing ->
                                    ListingGridCard(
                                        listing = listing,
                                        isSaved = listing.id in savedListingIds,
                                        onClick = { selectedListing = listing },
                                        onToggleSave = { onToggleSaveListing(listing.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateListingSheet(
            isBusy = isBusy,
            onDismiss = { showCreateSheet = false },
            onSubmit = { title, desc, price, cat, cond, pickup, uri ->
                onCreateListing(title, desc, price, cat, cond, pickup, uri) {
                    showCreateSheet = false
                }
            }
        )
    }

    val activeListing = selectedListing
    if (activeListing != null) {
        ListingDetailSheet(
            listing = activeListing,
            currentUserId = profile.userId,
            onDismiss = { selectedListing = null },
            onMessageSeller = {
                selectedListing = null
                onMessageSeller(activeListing)
            },
            onMarkStatus = { newStatus ->
                onUpdateListingStatus(activeListing.id, newStatus) {
                    selectedListing = null
                }
            },
            onDelete = {
                onDeleteListing(activeListing.id) {
                    selectedListing = null
                }
            }
        )
    }
}

@Composable
private fun ListingGridCard(
    listing: MarketplaceListing,
    isSaved: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit
) {
    val status = ListingStatus.fromRaw(listing.status)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("listing_card_${listing.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!listing.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = listing.photoUri,
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sell,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Price badge
                Surface(
                    color = if (listing.priceDollars == 0L) ForestGreen else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (listing.priceDollars == 0L) "FREE" else "$${listing.priceDollars}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (listing.priceDollars == 0L) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Save listing",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${ListingCondition.fromRaw(listing.condition).label} • ${listing.pickupArea.ifBlank { listing.neighborhoodName }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (status != ListingStatus.AVAILABLE) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = if (status == ListingStatus.SOLD) CrimsonAlert.copy(alpha = 0.12f) else WarmSand,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = status.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (status == ListingStatus.SOLD) CrimsonAlert else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateListingSheet(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Long, ListingCategory, ListingCondition, String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("0") }
    var category by remember { mutableStateOf(ListingCategory.FURNITURE) }
    var condition by remember { mutableStateOf(ListingCondition.GOOD) }
    var pickupArea by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoUri = uri
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("List an Item", style = MaterialTheme.typography.headlineSmall)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ListingCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = {
                            category = cat
                            if (cat == ListingCategory.FREE) priceText = "0"
                        },
                        label = { Text(cat.label) }
                    )
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 120) title = it },
                label = { Text("Item Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("listing_title_input")
            )
            OutlinedTextField(
                value = if (category == ListingCategory.FREE) "0" else priceText,
                onValueChange = { raw -> priceText = raw.filter { it.isDigit() }.take(6) },
                enabled = category != ListingCategory.FREE,
                label = { Text("Price in USD (0 = Free)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("listing_price_input")
            )
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ListingCondition.entries.forEach { cond ->
                    FilterChip(selected = condition == cond, onClick = { condition = cond }, label = { Text(cond.label) })
                }
            }
            OutlinedTextField(
                value = pickupArea,
                onValueChange = { if (it.length <= 100) pickupArea = it },
                label = { Text("Pickup Area (e.g. Porch near Maple & 3rd)") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 1500) description = it },
                label = { Text("Description & Dimensions") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("listing_desc_input")
            )
            OutlinedButton(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (photoUri == null) "Add Item Photo" else "Change Photo")
            }
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Selected item photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(14.dp))
                )
            }
            val parsedPrice = priceText.toLongOrNull() ?: 0L
            Button(
                onClick = { onSubmit(title, description, parsedPrice, category, condition, pickupArea, photoUri?.toString()) },
                enabled = title.isNotBlank() && description.isNotBlank() && !isBusy,
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("publish_listing_button")
            ) {
                Text(if (isBusy) "Listing..." else "Publish Listing")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListingDetailSheet(
    listing: MarketplaceListing,
    currentUserId: String,
    onDismiss: () -> Unit,
    onMessageSeller: () -> Unit,
    onMarkStatus: (ListingStatus) -> Unit,
    onDelete: () -> Unit
) {
    val isOwner = listing.sellerId == currentUserId
    val status = ListingStatus.fromRaw(listing.status)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!listing.photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = listing.photoUri,
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp))
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = SageTint, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = if (listing.priceDollars == 0L) "FREE" else "$${listing.priceDollars}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ForestGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Text(
                    "${ListingCondition.fromRaw(listing.condition).label} • ${status.label}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Text(listing.title, style = MaterialTheme.typography.headlineSmall)
            Text(listing.description, style = MaterialTheme.typography.bodyLarge)

            Row(verticalAlignment = Alignment.CenterVertically) {
                NeighborAvatar(listing.sellerInitials, listing.sellerAvatarColor, size = 36.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(listing.sellerName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${listing.neighborhoodName} • ${relativeTimeLabel(listing.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isOwner) {
                Button(
                    onClick = onMessageSeller,
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("message_seller_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Message ${listing.sellerName}")
                }
            } else {
                if (status != ListingStatus.SOLD) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (status != ListingStatus.PENDING) {
                            OutlinedButton(
                                onClick = { onMarkStatus(ListingStatus.PENDING) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Mark Pending") }
                        }
                        Button(
                            onClick = { onMarkStatus(ListingStatus.SOLD) },
                            modifier = Modifier.weight(1f).testTag("mark_sold_button")
                        ) { Text("Mark Sold") }
                    }
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth().testTag("delete_listing_button")
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Listing", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
