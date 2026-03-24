package com.example.marketofthehood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.marketofthehood.ui.theme.MarketofthehoodTheme
import java.util.Locale

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
    SELLER("Seller")
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

private val demoCredentials = mapOf(
    "aksha" to "1234",
    "sam" to "password",
    "ria" to "market"
)

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
        price = "$210",
        sellerId = "sam"
    ),
    Listing(
        id = 2,
        title = "Study Table",
        description = "Wooden study table with drawer.",
        category = "Furniture",
        price = "$85",
        sellerId = "ria"
    ),
    Listing(
        id = 3,
        title = "Laptop 16GB RAM",
        description = "Lightly used for one year, charger included.",
        category = "Electronics",
        price = "$520",
        sellerId = "aksha"
    ),
    Listing(
        id = 4,
        title = "Guitar",
        description = "Acoustic guitar for beginners.",
        category = "Music",
        price = "$95",
        sellerId = "sam"
    )
)

@Composable
fun BuySellApp() {
    var loggedInUserId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedListingId by rememberSaveable { mutableStateOf<Int?>(null) }
    val allListings = remember { mutableStateListOf<Listing>().apply { addAll(starterListings) } }
    var nextId by rememberSaveable { mutableStateOf(starterListings.size + 1) }

    if (loggedInUserId == null) {
        LoginScreen(onLoginSuccess = { loggedInUserId = it })
        return
    }

    val selectedListing = allListings.firstOrNull { it.id == selectedListingId }

    if (selectedListing != null) {
        ListingDetailScreen(
            listing = selectedListing,
            sellerRating = sellerRatings[selectedListing.sellerId] ?: 0f,
            onBack = { selectedListingId = null }
        )
    } else {
        MarketplaceScreen(
            userId = loggedInUserId!!,
            listings = allListings,
            sellerRatings = sellerRatings,
            onListingClick = { selectedListingId = it.id },
            onAddListing = { title, description, category, price, imageUri ->
                allListings.add(
                    Listing(
                        id = nextId,
                        title = title,
                        description = description,
                        category = category,
                        price = price,
                        sellerId = loggedInUserId!!,
                        imageUri = imageUri
                    )
                )
                nextId += 1
            },
            onLogout = {
                selectedListingId = null
                loggedInUserId = null
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
    sellerRatings: Map<String, Float>,
    onListingClick: (Listing) -> Unit,
    onAddListing: (String, String, String, String, String?) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.MARKET) }

    // On the Seller tab, system back returns to the main market tab first.
    BackHandler(enabled = selectedTab == HomeTab.SELLER) {
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
                    TextButton(onClick = onLogout) {
                        Text("Logout")
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

        items(items = listings, key = { it.id }) { listing ->
            ListingCard(
                listing = listing,
                sellerRating = sellerRatings[listing.sellerId] ?: 0f,
                onClick = { onListingClick(listing) }
            )
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
                prefix = { Text("$") },
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
                            "$${price.trim()}",
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
    onBack: () -> Unit
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
            sellerRatings = sellerRatings,
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
            onBack = {}
        )
    }
}
