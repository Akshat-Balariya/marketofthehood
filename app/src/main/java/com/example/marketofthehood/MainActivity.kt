package com.example.marketofthehood

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.marketofthehood.ui.theme.MarketofthehoodTheme
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarketofthehoodTheme {
                BuySellApp()
            }
        }
    }
}

enum class HomeTab(val label: String) {
    MARKET("On Sale"),
    SELLER("Seller"),
    PURCHASES("Purchases")
}

data class Listing(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val price: String,
    val sellerId: String,
    val imageUri: String? = null
)

data class ChatMessage(
    val senderId: String,
    val text: String
)

enum class ListingSortOption(val label: String) {
    DEFAULT("Default"),
    PRICE_LOW_TO_HIGH("Price: Low to High"),
    PRICE_HIGH_TO_LOW("Price: High to Low")
}

private val demoCredentials = mapOf(
    "aksha" to "1234",
    "sam" to "password",
    "ria" to "market"
)

private const val SESSION_PREFS = "market_session"
private const val SESSION_USER_ID_KEY = "logged_in_user_id"
private const val SESSION_LISTINGS_KEY = "saved_listings"

private fun parseListingPrice(price: String): Double? {
    val normalized = price.replace(Regex("[^0-9.]"), "")
    return normalized.toDoubleOrNull()
}

private fun persistImageUriForListing(context: Context, sourceUriString: String?): String? {
    if (sourceUriString.isNullOrBlank()) return null

    return try {
        val sourceUri = Uri.parse(sourceUriString)
        val imagesDir = File(context.filesDir, "listing_images").apply { mkdirs() }
        val targetFile = File(
            imagesDir,
            "listing_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
        )

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null

        Uri.fromFile(targetFile).toString()
    } catch (_: Exception) {
        null
    }
}

private fun saveListings(sessionPrefs: android.content.SharedPreferences, listings: List<Listing>) {
    val jsonArray = JSONArray()
    listings.forEach { listing ->
        jsonArray.put(
            JSONObject()
                .put("id", listing.id)
                .put("title", listing.title)
                .put("description", listing.description)
                .put("category", listing.category)
                .put("price", listing.price)
                .put("sellerId", listing.sellerId)
                .put("imageUri", listing.imageUri)
        )
    }
    sessionPrefs.edit().putString(SESSION_LISTINGS_KEY, jsonArray.toString()).apply()
}

private fun loadListings(sessionPrefs: android.content.SharedPreferences): List<Listing>? {
    val raw = sessionPrefs.getString(SESSION_LISTINGS_KEY, null) ?: return null
    return try {
        val jsonArray = JSONArray(raw)
        buildList {
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(index) ?: continue
                add(
                    Listing(
                        id = item.optInt("id"),
                        title = item.optString("title"),
                        description = item.optString("description"),
                        category = item.optString("category"),
                        price = item.optString("price"),
                        sellerId = item.optString("sellerId"),
                        imageUri = item.optString("imageUri").ifBlank { null }
                    )
                )
            }
        }
    } catch (_: Exception) {
        null
    }
}

private val sellerRatings = mapOf(
    "aksha" to 4.6f,
    "sam" to 4.2f,
    "ria" to 4.8f
)

private val starterListings = listOf(
    Listing(
        id = 1,
        title = "Mountain Bicycle",
        description = "Good condition bike, recently serviced.",
        category = "Sports",
        price = "Rs 210",
        sellerId = "sam"
    ),
    Listing(
        id = 2,
        title = "Study Table",
        description = "Wooden study table with drawer.",
        category = "Furniture",
        price = "Rs 85",
        sellerId = "ria"
    ),
    Listing(
        id = 3,
        title = "Laptop 16GB RAM",
        description = "Lightly used for one year, charger included.",
        category = "Electronics",
        price = "Rs 520",
        sellerId = "aksha"
    ),
    Listing(
        id = 4,
        title = "Guitar",
        description = "Acoustic guitar for beginners.",
        category = "Music",
        price = "Rs 95",
        sellerId = "sam"
    ),
    Listing(
        id = 5,
        title = "Office Chair",
        description = "Ergonomic chair with adjustable height.",
        category = "Furniture",
        price = "Rs 140",
        sellerId = "ria"
    ),
    Listing(
        id = 6,
        title = "Bluetooth Speaker",
        description = "Portable speaker with deep bass and 10h battery.",
        category = "Electronics",
        price = "Rs 65",
        sellerId = "aksha"
    ),
    Listing(
        id = 7,
        title = "Cricket Bat",
        description = "English willow practice bat, lightly used.",
        category = "Sports",
        price = "Rs 120",
        sellerId = "sam"
    ),
    Listing(
        id = 8,
        title = "Microwave Oven",
        description = "20L microwave, fully working condition.",
        category = "Home",
        price = "Rs 175",
        sellerId = "ria"
    ),
    Listing(
        id = 9,
        title = "Running Shoes",
        description = "Comfortable running shoes, size 9.",
        category = "Fashion",
        price = "Rs 55",
        sellerId = "aksha"
    ),
    Listing(
        id = 10,
        title = "Ceiling Fan",
        description = "3-speed fan with remote, almost new.",
        category = "Home",
        price = "Rs 80",
        sellerId = "sam"
    ),
    Listing(
        id = 11,
        title = "Gaming Keyboard",
        description = "Mechanical keyboard with RGB backlight.",
        category = "Electronics",
        price = "Rs 110",
        sellerId = "ria"
    ),
    Listing(
        id = 12,
        title = "Bookshelf",
        description = "5-tier bookshelf made of engineered wood.",
        category = "Furniture",
        price = "Rs 160",
        sellerId = "aksha"
    ),
    Listing(
        id = 13,
        title = "Table Lamp",
        description = "LED study lamp with touch controls.",
        category = "Home",
        price = "Rs 35",
        sellerId = "sam"
    ),
    Listing(
        id = 14,
        title = "Backpack",
        description = "Water-resistant laptop backpack.",
        category = "Fashion",
        price = "Rs 45",
        sellerId = "ria"
    ),
    Listing(
        id = 15,
        title = "Coffee Maker",
        description = "Compact drip coffee machine.",
        category = "Kitchen",
        price = "Rs 130",
        sellerId = "aksha"
    ),
    Listing(
        id = 16,
        title = "Power Bank",
        description = "20000mAh fast-charging power bank.",
        category = "Electronics",
        price = "Rs 70",
        sellerId = "sam"
    ),
    Listing(
        id = 17,
        title = "Yoga Mat",
        description = "Non-slip mat with carry strap.",
        category = "Sports",
        price = "Rs 30",
        sellerId = "ria"
    ),
    Listing(
        id = 18,
        title = "Wall Clock",
        description = "Minimal design wall clock, silent movement.",
        category = "Home",
        price = "Rs 25",
        sellerId = "aksha"
    ),
    Listing(
        id = 19,
        title = "Desk Organizer",
        description = "Metal mesh organizer for stationery.",
        category = "Office",
        price = "Rs 20",
        sellerId = "sam"
    )
)

@Composable
fun BuySellApp() {
    val context = LocalContext.current
    val sessionPrefs = remember(context) {
        context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
    }
    val initialListings = remember(sessionPrefs) {
        loadListings(sessionPrefs)?.takeIf { it.isNotEmpty() } ?: starterListings
    }

    var loggedInUserId by rememberSaveable {
        mutableStateOf(sessionPrefs.getString(SESSION_USER_ID_KEY, null))
    }
    var selectedListingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var activeChatSellerId by rememberSaveable { mutableStateOf<String?>(null) }
    var isChatInboxOpen by rememberSaveable { mutableStateOf(false) }
    var pendingPurchaseListingId by rememberSaveable { mutableStateOf<Int?>(null) }
    val allListings = remember { mutableStateListOf<Listing>().apply { addAll(initialListings) } }
    val chatThreads = remember { mutableStateMapOf<String, List<ChatMessage>>() }
    val purchasesByUser = remember { mutableStateMapOf<String, List<Listing>>() }
    var nextId by rememberSaveable {
        mutableStateOf((initialListings.maxOfOrNull { it.id } ?: 0) + 1)
    }

    if (loggedInUserId == null) {
        LoginScreen(
            onLoginSuccess = { userId ->
                loggedInUserId = userId
                sessionPrefs.edit().putString(SESSION_USER_ID_KEY, userId).apply()
            }
        )
        return
    }

    val selectedListing = allListings.firstOrNull { it.id == selectedListingId }
    val pendingPurchaseListing = allListings.firstOrNull { it.id == pendingPurchaseListingId }
    val userPurchases = purchasesByUser[loggedInUserId!!].orEmpty()

    if (activeChatSellerId != null) {
        ChatScreen(
            currentUserId = loggedInUserId!!,
            sellerId = activeChatSellerId!!,
            messages = chatThreads[activeChatSellerId!!].orEmpty(),
            onSendMessage = { messageText ->
                val existing = chatThreads[activeChatSellerId!!].orEmpty()
                chatThreads[activeChatSellerId!!] = existing + ChatMessage(
                    senderId = loggedInUserId!!,
                    text = messageText
                )
            },
            onBack = { activeChatSellerId = null }
        )
    } else if (isChatInboxOpen) {
        ChatInboxScreen(
            chatThreads = chatThreads,
            onOpenChat = { sellerId ->
                isChatInboxOpen = false
                activeChatSellerId = sellerId
            },
            onBack = { isChatInboxOpen = false }
        )
    } else if (selectedListing != null) {
        ListingDetailScreen(
            listing = selectedListing,
            sellerRating = sellerRatings[selectedListing.sellerId] ?: 0f,
            onBack = { selectedListingId = null },
            onChatClick = {
                isChatInboxOpen = false
                activeChatSellerId = selectedListing.sellerId
                if (chatThreads[selectedListing.sellerId].isNullOrEmpty()) {
                    chatThreads[selectedListing.sellerId] = listOf(
                        ChatMessage(
                            senderId = selectedListing.sellerId,
                            text = "Hi! Thanks for your interest in ${selectedListing.title}."
                        )
                    )
                }
            },
            onBuyClick = { pendingPurchaseListingId = selectedListing.id }
        )
    } else {
        MarketplaceScreen(
            userId = loggedInUserId!!,
            listings = allListings,
            purchases = userPurchases,
            sellerRatings = sellerRatings,
            chatCount = chatThreads.size,
            onOpenChatInbox = { isChatInboxOpen = true },
            onListingClick = { selectedListingId = it.id },
            onAddListing = { title, description, category, price, imageUri ->
                val persistedImageUri = persistImageUriForListing(context, imageUri)
                allListings.add(
                    Listing(
                        id = nextId,
                        title = title,
                        description = description,
                        category = category,
                        price = price,
                        sellerId = loggedInUserId!!,
                        imageUri = persistedImageUri
                    )
                )
                nextId += 1
                saveListings(sessionPrefs, allListings)
            },
            onLogout = {
                activeChatSellerId = null
                isChatInboxOpen = false
                selectedListingId = null
                pendingPurchaseListingId = null
                loggedInUserId = null
                sessionPrefs.edit().remove(SESSION_USER_ID_KEY).apply()
            }
        )
    }

    if (pendingPurchaseListing != null) {
        PurchaseConfirmationDialog(
            listing = pendingPurchaseListing,
            onDismiss = { pendingPurchaseListingId = null },
            onConfirmBuy = {
                val existingPurchases = purchasesByUser[loggedInUserId!!].orEmpty()
                purchasesByUser[loggedInUserId!!] = existingPurchases + pendingPurchaseListing
                allListings.removeAll { it.id == pendingPurchaseListing.id }
                saveListings(sessionPrefs, allListings)
                pendingPurchaseListingId = null
                selectedListingId = null
            }
        )
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit
) {
    var userId by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Market of the Hood",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Sign in with your ID and password",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = userId,
                        onValueChange = {
                            userId = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("User ID") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            val trimmedUser = userId.trim()
                            if (demoCredentials[trimmedUser] == password) {
                                onLoginSuccess(trimmedUser)
                            } else {
                                errorMessage = "Invalid ID or password"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = userId.isNotBlank() && password.isNotBlank()
                    ) {
                        Text("Login")
                    }

                    Text(
                        text = "Try demo users: aksha/1234, sam/password, ria/market",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MarketplaceScreen(
    userId: String,
    listings: List<Listing>,
    purchases: List<Listing>,
    sellerRatings: Map<String, Float>,
    chatCount: Int,
    onOpenChatInbox: () -> Unit,
    onListingClick: (Listing) -> Unit,
    onAddListing: (String, String, String, String, String?) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.MARKET) }

    // On the Seller tab, system back returns to the main market tab first.
    BackHandler(enabled = selectedTab != HomeTab.MARKET) {
        selectedTab = HomeTab.MARKET
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Market of the Hood",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Logged in as $userId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenChatInbox) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Open chats"
                            )
                        }
                        if (chatCount > 0) {
                            Text(
                                text = chatCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(onClick = onLogout) {
                            Text("Logout")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(selectedTabIndex = selectedTab.ordinal) {
                    HomeTab.entries.forEach { tab ->
                        Tab(
                            selected = tab == selectedTab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            HomeTab.MARKET -> SaleFeed(
                listings = listings,
                sellerRatings = sellerRatings,
                paddingValues = innerPadding,
                onListingClick = onListingClick
            )

            HomeTab.SELLER -> SellerSection(
                userId = userId,
                listings = listings,
                sellerRatings = sellerRatings,
                paddingValues = innerPadding,
                onAddListing = onAddListing,
                onListingClick = onListingClick
            )

            HomeTab.PURCHASES -> PurchasesSection(
                purchases = purchases,
                sellerRatings = sellerRatings,
                paddingValues = innerPadding
            )
        }
    }
}

@Composable
fun SaleFeed(
    listings: List<Listing>,
    sellerRatings: Map<String, Float>,
    paddingValues: PaddingValues,
    onListingClick: (Listing) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var minPriceInput by rememberSaveable { mutableStateOf("") }
    var maxPriceInput by rememberSaveable { mutableStateOf("") }
    var selectedSort by rememberSaveable { mutableStateOf(ListingSortOption.DEFAULT) }

    val minPrice = minPriceInput.toDoubleOrNull()
    val maxPrice = maxPriceInput.toDoubleOrNull()

    val normalizedQuery = searchQuery.trim()
    val searchedListings = if (normalizedQuery.isBlank()) {
        listings
    } else {
        listings.filter { listing ->
            listing.title.contains(normalizedQuery, ignoreCase = true) ||
                listing.description.contains(normalizedQuery, ignoreCase = true) ||
                listing.category.contains(normalizedQuery, ignoreCase = true) ||
                listing.sellerId.contains(normalizedQuery, ignoreCase = true)
        }
    }

    val filteredListings = searchedListings.filter { listing ->
        val priceValue = parseListingPrice(listing.price)
        when {
            priceValue == null -> minPrice == null && maxPrice == null
            minPrice != null && priceValue < minPrice -> false
            maxPrice != null && priceValue > maxPrice -> false
            else -> true
        }
    }

    val visibleListings = when (selectedSort) {
        ListingSortOption.DEFAULT -> filteredListings
        ListingSortOption.PRICE_LOW_TO_HIGH -> {
            filteredListings.sortedBy { parseListingPrice(it.price) ?: Double.MAX_VALUE }
        }
        ListingSortOption.PRICE_HIGH_TO_LOW -> {
            filteredListings.sortedByDescending { parseListingPrice(it.price) ?: Double.MIN_VALUE }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Items on sale",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Search") },
                    singleLine = true,
                    placeholder = { Text("Item, category, seller") }
                )

                Button(
                    onClick = { showFilters = !showFilters }
                ) {
                    Text(if (showFilters) "Hide" else "Filters")
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = showFilters,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = minPriceInput,
                                onValueChange = { minPriceInput = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Min price") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            OutlinedTextField(
                                value = maxPriceInput,
                                onValueChange = { maxPriceInput = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Max price") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ListingSortOption.entries.forEach { option ->
                                FilterChip(
                                    selected = selectedSort == option,
                                    onClick = { selectedSort = option },
                                    label = { Text(option.label) }
                                )
                            }
                            TextButton(
                                onClick = {
                                    minPriceInput = ""
                                    maxPriceInput = ""
                                    selectedSort = ListingSortOption.DEFAULT
                                }
                            ) {
                                Text("Reset")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Showing ${visibleListings.size} item(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (visibleListings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "No listings found for your search/filter.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(items = visibleListings, key = { it.id }) { listing ->
                ListingCard(
                    listing = listing,
                    sellerRating = sellerRatings[listing.sellerId] ?: 0f,
                    onClick = { onListingClick(listing) }
                )
            }
        }
    }
}

@Composable
fun SellerSection(
    userId: String,
    listings: List<Listing>,
    sellerRatings: Map<String, Float>,
    paddingValues: PaddingValues,
    onAddListing: (String, String, String, String, String?) -> Unit,
    onListingClick: (Listing) -> Unit
) {
    val myListings = listings.filter { it.sellerId == userId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AddListingCard(onAddListing = onAddListing)
        }

        item {
            Text(
                text = "Your listings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (myListings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "No listings yet. Add your first item above.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(items = myListings, key = { it.id }) { listing ->
                ListingCard(
                    listing = listing,
                    sellerRating = sellerRatings[listing.sellerId] ?: 0f,
                    onClick = { onListingClick(listing) }
                )
            }
        }
    }
}

@Composable
fun PurchasesSection(
    purchases: List<Listing>,
    sellerRatings: Map<String, Float>,
    paddingValues: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Your purchases",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (purchases.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "No purchases yet. Buy an item to track it here.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(items = purchases, key = { it.id }) { listing ->
                ListingCard(
                    listing = listing,
                    sellerRating = sellerRatings[listing.sellerId] ?: 0f,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
fun AddListingCard(
    onAddListing: (String, String, String, String, String?) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var imageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        imageUri = uri?.toString()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Add new listing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    formError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Item title") },
                singleLine = true
            )

            OutlinedTextField(
                value = category,
                onValueChange = {
                    category = it
                    formError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Category") },
                singleLine = true
            )

            OutlinedTextField(
                value = price,
                onValueChange = {
                    price = it
                    formError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Price") },
                prefix = { Text("Rs ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    formError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description") },
                minLines = 2,
                maxLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ListingImage(
                    imageUri = imageUri,
                    category = category,
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(
                                 ActivityResultContracts.PickVisualMedia.ImageOnly
                             )
                        )
                    }
                ) {
                    Text(if (imageUri == null) "Add Image" else "Change Image")
                }
            }

            if (formError != null) {
                Text(
                    text = formError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    if (title.isBlank() || category.isBlank() || price.isBlank() || description.isBlank()) {
                        formError = "Please fill all fields"
                    } else {
                        onAddListing(
                            title.trim(),
                            description.trim(),
                            category.trim(),
                            "Rs ${price.trim()}",
                            imageUri
                        )
                        title = ""
                        description = ""
                        category = ""
                        price = ""
                        imageUri = null
                        formError = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Post Listing")
            }
        }
    }
}

@Composable
fun ListingCard(
    listing: Listing,
    sellerRating: Float,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ListingImage(
                imageUri = listing.imageUri,
                category = listing.category,
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Seller: ${listing.sellerId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SellerRatingBar(
                    rating = sellerRating,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Text(
                text = listing.price,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ListingDetailScreen(
    listing: Listing,
    sellerRating: Float,
    onBack: () -> Unit,
    onChatClick: () -> Unit,
    onBuyClick: () -> Unit
) {
    // System back from details should return to the previous screen in-app.
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                Text(
                    text = "Item details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ListingImage(
                    imageUri = listing.imageUri,
                    category = listing.category,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(18.dp)
                )
            }

            item {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Text(
                    text = listing.price,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = listing.category,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            item {
                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Seller details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = listing.sellerId,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        SellerRatingBar(rating = sellerRating)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onChatClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Chat")
                    }

                    Button(
                        onClick = onBuyClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Buy")
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseConfirmationDialog(
    listing: Listing,
    onDismiss: () -> Unit,
    onConfirmBuy: () -> Unit
) {
    val highlightStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Confirm Purchase") },
        text = {
            Text(
                text = buildAnnotatedString {
                    append("Are you sure you want to buy ")
                    withStyle(style = highlightStyle) {
                        append(listing.title)
                    }
                    append(" for ")
                    withStyle(style = highlightStyle) {
                        append(listing.price)
                    }
                    append("?")
                }
            )
        },
        confirmButton = {
            Button(onClick = onConfirmBuy) {
                Text("Buy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ChatScreen(
    currentUserId: String,
    sellerId: String,
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    var inputText by rememberSaveable { mutableStateOf("") }

    BackHandler(onBack = onBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                Column {
                    Text(
                        text = "Chat with $sellerId",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ask item details before confirming",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isMine = message.senderId == currentUserId

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (isMine) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = message.text,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = if (isMine) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message") },
                    maxLines = 3
                )

                Button(
                    onClick = {
                        val message = inputText.trim()
                        if (message.isNotEmpty()) {
                            onSendMessage(message)
                            inputText = ""
                        }
                    }
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun ChatInboxScreen(
    chatThreads: Map<String, List<ChatMessage>>,
    onOpenChat: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val threadEntries = chatThreads.entries
        .filter { it.value.isNotEmpty() }
        .sortedBy { it.key }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                Text(
                    text = "Your chats",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) { innerPadding ->
        if (threadEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No previous chats yet. Open any item and tap Chat to start.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(threadEntries, key = { it.key }) { thread ->
                    val sellerId = thread.key
                    val lastMessage = thread.value.lastOrNull()?.text.orEmpty()

                    Card(
                        onClick = { onOpenChat(sellerId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = sellerId,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = lastMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListingImage(
    imageUri: String?,
    category: String,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    if (imageUri.isNullOrBlank()) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = category.take(1).ifBlank { "?" }.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        AsyncImage(
            model = imageUri,
            contentDescription = "Listing image",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun SellerRatingBar(
    rating: Float,
    modifier: Modifier = Modifier
) {
    val clamped = rating.coerceIn(0f, 5f)
    val filledStars = clamped.toInt()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            Text(
                text = if (index < filledStars) "★" else "☆",
                color = if (index < filledStars) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = String.format(Locale.US, "%.1f/5", clamped),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MarketofthehoodTheme {
        LoginScreen(onLoginSuccess = {})
    }
}

@Preview(showBackground = true)
@Composable
fun MarketplaceScreenPreview() {
    MarketofthehoodTheme {
        MarketplaceScreen(
            userId = "aksha",
            listings = starterListings,
            purchases = starterListings.take(1),
            sellerRatings = sellerRatings,
            chatCount = 2,
            onOpenChatInbox = {},
            onListingClick = {},
            onAddListing = { _, _, _, _, _ -> },
            onLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ListingDetailPreview() {
    MarketofthehoodTheme {
        ListingDetailScreen(
            listing = starterListings.first(),
            sellerRating = sellerRatings["sam"] ?: 0f,
            onBack = {},
            onChatClick = {},
            onBuyClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    MarketofthehoodTheme {
        ChatScreen(
            currentUserId = "aksha",
            sellerId = "sam",
            messages = listOf(
                ChatMessage(senderId = "sam", text = "Hi! Is this item available?"),
                ChatMessage(senderId = "aksha", text = "Yes, it is available.")
            ),
            onSendMessage = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatInboxPreview() {
    MarketofthehoodTheme {
        ChatInboxScreen(
            chatThreads = mapOf(
                "sam" to listOf(
                    ChatMessage("sam", "Hi! Is this item still available?"),
                    ChatMessage("aksha", "Yes, available")
                ),
                "ria" to listOf(
                    ChatMessage("ria", "Can you pick up tomorrow?")
                )
            ),
            onOpenChat = {},
            onBack = {}
        )
    }
}

