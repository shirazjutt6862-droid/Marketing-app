package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PriceUpdate
import com.example.data.model.Product
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateMessage
import com.example.ui.components.PriceMovementBadge
import com.example.ui.theme.PriceDecreaseContainer
import com.example.ui.theme.PriceDecreaseGreen
import com.example.ui.theme.PriceIncreaseContainer
import com.example.ui.theme.PriceIncreaseRed
import com.example.util.formatPkr
import com.example.util.formatTimestamp
import com.example.viewmodel.StoreViewModel
import kotlin.math.abs

@Composable
fun DailyRatesScreen(
    viewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val priceUpdatesState by viewModel.priceUpdatesState.collectAsStateWithLifecycle()
    var filterTab by remember { mutableIntStateOf(0) } // 0: All rates, 1: History log, 2: Decreased, 3: Increased

    BackHandler(enabled = filterTab != 0) {
        filterTab = 0
    }

    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val updates = (priceUpdatesState as? UiState.Success)?.data ?: emptyList()

    val increasedCount = remember(products) { products.count { it.priceDelta > 0.5 } }
    val decreasedCount = remember(products) { products.count { it.priceDelta < -0.5 } }
    val latestTimestamp = remember(products, updates) {
        updates.firstOrNull()?.createdAt
            ?: products.maxByOrNull { (it.priceUpdatedAt ?: it.updatedAt)?.seconds ?: 0L }
                ?.let { it.priceUpdatedAt ?: it.updatedAt }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "روزانہ تازہ ریٹ لسٹ",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "آخری اپ ڈیٹ: ${formatTimestamp(latestTimestamp)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Filled.PriceChange,
                            contentDescription = "روزانہ ریٹس",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RateSummaryPill(
                            label = "کل آئٹمز",
                            value = products.size.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        RateSummaryPill(
                            label = "قیمت میں کمی",
                            value = decreasedCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        RateSummaryPill(
                            label = "قیمت میں اضافہ",
                            value = increasedCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filterTab == 0,
                        onClick = { filterTab = 0 },
                        label = { Text("مکمل ریٹ لسٹ (${products.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = filterTab == 1,
                        onClick = { filterTab = 1 },
                        label = { Text("ریٹ تبدیلی ہسٹری (${updates.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = filterTab == 2,
                        onClick = { filterTab = 2 },
                        label = { Text("کمی والے ریٹس ($decreasedCount)") }
                    )
                }
                item {
                    FilterChip(
                        selected = filterTab == 3,
                        onClick = { filterTab = 3 },
                        label = { Text("اضافے والے ریٹس ($increasedCount)") }
                    )
                }
            }
        }

        if (productsState is UiState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (filterTab == 1) {
            if (updates.isEmpty()) {
                item {
                    EmptyStateMessage(
                        icon = Icons.Filled.PriceChange,
                        title = "ابھی کوئی ریٹ تبدیلی ریکارڈ نہیں ہوئی",
                        subtitle = "جب دکان کا مالک روزانہ کا نیا ریٹ اپ ڈیٹ کرے گا تو یہاں پوری تفصیل فوراً ظاہر ہو گی۔"
                    )
                }
            } else {
                items(updates, key = { it.id }) { upd ->
                    PriceHistoryRowCard(update = upd)
                }
            }
        } else {
            val filtered = when (filterTab) {
                2 -> products.filter { it.priceDelta < -0.5 }
                3 -> products.filter { it.priceDelta > 0.5 }
                else -> products
            }
            if (filtered.isEmpty()) {
                item {
                    EmptyStateMessage(
                        icon = Icons.Filled.PriceChange,
                        title = "اس زمرے میں کوئی آئٹم نہیں",
                        subtitle = "مکمل ریٹ لسٹ دیکھنے کے لیے اوپر پہلا فلٹر منتخب کریں۔"
                    )
                }
            } else {
                items(filtered, key = { it.id }) { product ->
                    LiveRateRowCard(product = product)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun RateSummaryPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun LiveRateRowCard(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${product.category} • ${product.packingDetails}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatPkr(product.wholesalePrice),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "پرچون: ${formatPkr(product.retailPrice)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (abs(product.priceDelta) >= 0.5) {
                    PriceMovementBadge(
                        currentPrice = product.wholesalePrice,
                        previousPrice = product.previousWholesalePrice
                    )
                } else {
                    Text(
                        text = "گزشتہ ریٹ برقرار: ${formatPkr(product.previousWholesalePrice)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = formatTimestamp(product.priceUpdatedAt ?: product.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PriceHistoryRowCard(update: PriceUpdate) {
    val increased = update.changeAmount > 0
    val bg = if (increased) PriceIncreaseContainer else PriceDecreaseContainer
    val fg = if (increased) PriceIncreaseRed else PriceDecreaseGreen
    val icon = if (increased) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = update.productName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${update.category} • ${update.packingDetails}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimestamp(update.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPkr(update.oldPrice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                    Text(
                        text = formatPkr(update.newPrice),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = fg,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (increased) "+${formatPkr(update.changeAmount)} اضافہ"
                            else "-${formatPkr(abs(update.changeAmount))} کمی",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = fg
                        )
                    }
                }
            }
        }
    }
}
