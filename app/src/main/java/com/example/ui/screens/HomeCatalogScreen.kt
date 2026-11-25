package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.StarterCatalogData
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateMessage
import com.example.ui.components.PriceMovementBadge
import com.example.ui.components.ProductImage
import com.example.ui.components.ShopHeroBanner
import com.example.ui.components.categoryIconFor
import com.example.ui.theme.OfferAmber
import com.example.ui.theme.PriceDecreaseContainer
import com.example.ui.theme.PriceDecreaseGreen
import com.example.ui.theme.PriceIncreaseContainer
import com.example.ui.theme.PriceIncreaseRed
import com.example.util.formatPkr
import com.example.util.formatShortDate
import com.example.util.openDialer
import com.example.util.openWhatsAppChat
import com.example.viewmodel.StoreViewModel

@Composable
fun HomeCatalogScreen(
    viewModel: StoreViewModel,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shopState by viewModel.shopInfoState.collectAsStateWithLifecycle()
    val categoriesState by viewModel.categoriesState.collectAsStateWithLifecycle()
    val productsState by viewModel.filteredProductsState.collectAsStateWithLifecycle()
    val allProductsState by viewModel.productsState.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val offersOnly by viewModel.offersOnly.collectAsStateWithLifecycle()
    val trendingOnly by viewModel.trendingOnly.collectAsStateWithLifecycle()

    val shopInfo = (shopState as? UiState.Success)?.data ?: StarterCatalogData.defaultShopInfo
    val categories = (categoriesState as? UiState.Success)?.data?.filter { it.active } ?: emptyList()
    val allProducts = (allProductsState as? UiState.Success)?.data ?: emptyList()
    val latestTimestamp = remember(allProducts) {
        allProducts.maxByOrNull { (it.priceUpdatedAt ?: it.updatedAt)?.seconds ?: 0L }
            ?.let { it.priceUpdatedAt ?: it.updatedAt }
    }
    val cartQuantities = remember(cartItems) {
        cartItems.associate { it.product.id to it.quantity }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 680.dp) 2 else 1

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ShopHeroBanner(
                    shopInfo = shopInfo,
                    latestUpdateTimestamp = latestTimestamp,
                    onWhatsAppClick = {
                        openWhatsAppChat(
                            context,
                            shopInfo.whatsappNumber,
                            "السلام علیکم ${shopInfo.businessName}! مجھے آج کی ریٹ لسٹ اور آرڈر کے بارے میں بات کرنی ہے۔"
                        )
                    },
                    onCallClick = { openDialer(context, shopInfo.contact1Phone) }
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_products_input"),
                    placeholder = { Text("مصنوعات تلاش کریں (آٹا، چاول، گھی، چینی...)") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "تلاش") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "تلاش صاف کریں")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null && !offersOnly && !trendingOnly,
                            onClick = {
                                viewModel.selectCategory(null)
                                if (offersOnly) viewModel.toggleOffersOnly()
                                if (trendingOnly) viewModel.toggleTrendingOnly()
                            },
                            label = { Text("تمام اشیاء (${allProducts.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = trendingOnly,
                            onClick = { viewModel.toggleTrendingOnly() },
                            label = { Text("ٹرینڈنگ") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            modifier = Modifier.testTag("home_filter_trending")
                        )
                    }
                    item {
                        FilterChip(
                            selected = offersOnly,
                            onClick = { viewModel.toggleOffersOnly() },
                            label = { Text("خصوصی آفرز") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.LocalOffer,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                    items(categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.name,
                            onClick = {
                                viewModel.selectCategory(
                                    if (selectedCategory == cat.name) null else cat.name
                                )
                            },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIconFor(cat.iconKey.ifBlank { cat.name }),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            when (val state = productsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        EmptyStateMessage(
                            icon = Icons.Filled.ShoppingCart,
                            title = "ڈیٹا لوڈ نہیں ہو سکا",
                            subtitle = state.message
                        )
                    }
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        item {
                            EmptyStateMessage(
                                icon = Icons.Filled.Search,
                                title = "کوئی پروڈکٹ نہیں ملا",
                                subtitle = "تلاش کا لفظ یا کیٹیگری تبدیل کر کے دیکھیں، یا براہِ راست واٹس ایپ پر رابطہ کریں۔"
                            )
                        }
                    } else {
                        val rows = state.data.chunked(columns)
                        items(rows, key = { row -> row.joinToString("_") { it.id } }) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { product ->
                                    ProductCatalogCard(
                                        product = product,
                                        cartQty = cartQuantities[product.id] ?: 0,
                                        onAddToCart = { qty -> viewModel.addToCart(product, qty) },
                                        onGoToCart = onNavigateToCart,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowItems.size < columns) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
fun ProductCatalogCard(
    product: Product,
    cartQty: Int,
    onAddToCart: (Int) -> Unit,
    onGoToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedQty by remember(product.id) { mutableIntStateOf(1) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                ProductImage(
                    imageData = product.imageData,
                    category = product.category,
                    contentDescription = product.name,
                    modifier = Modifier.size(84.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = product.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            if (product.trending) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "ٹرینڈنگ",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            color = if (product.available) PriceDecreaseContainer else PriceIncreaseContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (product.available) "دستیاب" else "اسٹاک ختم",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (product.available) PriceDecreaseGreen else PriceIncreaseRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val stockSuffix = if (product.available && product.stockCount > 0L) {
                        " • اسٹاک: ${product.stockCount}"
                    } else ""
                    Text(
                        text = "پیکنگ: ${product.packingDetails}$stockSuffix",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (product.specialOffer) {
                Surface(
                    color = OfferAmber.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalOffer,
                            contentDescription = "خصوصی آفر",
                            tint = OfferAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = product.offerText.ifBlank { "خصوصی ہول سیل آفر دستیاب ہے!" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = OfferAmber
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "تازہ ہول سیل ریٹ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatPkr(product.wholesalePrice),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "پرچون ریٹ: ${formatPkr(product.retailPrice)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "ریٹ تاریخ: ${formatShortDate(product.priceUpdatedAt ?: product.updatedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    PriceMovementBadge(
                        currentPrice = product.wholesalePrice,
                        previousPrice = product.previousWholesalePrice
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (selectedQty > 1) selectedQty-- },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "تعداد کم کریں")
                        }
                        Text(
                            text = selectedQty.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { if (selectedQty < 99) selectedQty++ },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "تعداد بڑھائیں")
                        }
                    }
                }

                Button(
                    onClick = {
                        onAddToCart(selectedQty)
                        selectedQty = 1
                    },
                    enabled = product.available,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_to_cart_${product.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (cartQty > 0) Icons.Filled.CheckCircle else Icons.Filled.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (cartQty > 0) "مزید شامل کریں ($cartQty)" else "کارٹ میں ڈالیں",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (cartQty > 0) {
                Surface(
                    onClick = onGoToCart,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "اس آئٹم کی $cartQty تعداد کارٹ میں موجود ہے — آرڈر مکمل کرنے کے لیے یہاں ٹیپ کریں",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}
