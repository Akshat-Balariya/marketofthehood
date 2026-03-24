package com.example.marketofthehood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.marketofthehood.ui.theme.MarketofthehoodTheme

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
    val sellerId: String
)

private val demoCredentials = mapOf(
    "aksha" to "1234",
    "sam" to "password",
    "ria" to "market"
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

    if (loggedInUserId == null) {
        LoginScreen(
            onLoginSuccess = { loggedInUserId = it }
        )
    } else {
        val allListings = remember { mutableStateListOf<Listing>().apply { addAll(starterListings) } }
        var nextId by rememberSaveable { mutableStateOf(starterListings.size + 1) }

        MarketplaceScreen(
            userId = loggedInUserId!!,
            listings = allListings,
            onAddListing = { title, description, category, price ->
                allListings.add(
                    Listing(
                        id = nextId,
                        title = title,
                        description = description,
                        category = category,
                        price = price,
                        sellerId = loggedInUserId!!
                    )
                )
                nextId += 1
            },
            onLogout = { loggedInUserId = null }
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
    onAddListing: (String, String, String, String) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.MARKET) }

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
                paddingValues = innerPadding
            )

            HomeTab.SELLER -> SellerSection(
                userId = userId,
                listings = listings,
                paddingValues = innerPadding,
                onAddListing = onAddListing
            )
        }
    }
}

@Composable
fun SaleFeed(
    listings: List<Listing>,
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
                text = "Items on sale",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(items = listings, key = { it.id }) { listing ->
            ListingCard(listing = listing)
        }
    }
}

@Composable
fun SellerSection(
    userId: String,
    listings: List<Listing>,
    paddingValues: PaddingValues,
    onAddListing: (String, String, String, String) -> Unit
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
                ListingCard(listing = listing)
            }
        }
    }
}

@Composable
fun AddListingCard(
    onAddListing: (String, String, String, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }

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
                            "$${price.trim()}"
                        )
                        title = ""
                        description = ""
                        category = ""
                        price = ""
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
fun ListingCard(listing: Listing) {
    Card(
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
            Surface(
                modifier = Modifier
                    .size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = listing.category.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
            onAddListing = { _, _, _, _ -> },
            onLogout = {}
        )
    }
}