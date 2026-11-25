package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.OrderStatus
import com.example.data.model.ShopInfo
import com.example.ui.theme.PriceDecreaseContainer
import com.example.ui.theme.PriceDecreaseGreen
import com.example.ui.theme.PriceIncreaseContainer
import com.example.ui.theme.PriceIncreaseRed
import com.example.ui.theme.WhatsAppGreen
import com.example.util.formatPkr
import com.example.util.formatTimestamp
import com.google.firebase.Timestamp
import kotlin.math.abs

fun categoryIconFor(categoryOrKey: String): ImageVector {
    return when {
        categoryOrKey.contains("آٹا") || categoryOrKey == "flour" -> Icons.Filled.BakeryDining
        categoryOrKey.contains("چاول") || categoryOrKey == "rice" -> Icons.Filled.RiceBowl
        categoryOrKey.contains("گھی") || categoryOrKey.contains("آئل") || categoryOrKey == "ghee" || categoryOrKey == "oil" -> Icons.Filled.OilBarrel
        categoryOrKey.contains("کولڈ") || categoryOrKey == "drinks" -> Icons.Filled.LocalDrink
        categoryOrKey.contains("بسکٹ") || categoryOrKey.contains("ٹافی") || categoryOrKey == "biscuit" || categoryOrKey == "candy" -> Icons.Filled.Cookie
        categoryOrKey.contains("چینی") || categoryOrKey.contains("دال") || categoryOrKey == "sugar" || categoryOrKey == "pulses" -> Icons.Filled.Grain
        else -> Icons.Filled.Inventory2
    }
}

@Composable
fun ProductImage(
    imageData: String,
    category: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val trimmed = imageData.trim()
    val decodedBitmap = remember(trimmed) {
        if (trimmed.startsWith("data:image") && trimmed.contains("base64,")) {
            runCatching {
                val b64Part = trimmed.substringAfter("base64,")
                val bytes = Base64.decode(b64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        } else null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> {
                AsyncImage(
                    model = trimmed,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIconFor(category),
                        contentDescription = contentDescription,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PriceMovementBadge(
    currentPrice: Double,
    previousPrice: Double,
    modifier: Modifier = Modifier
) {
    val delta = currentPrice - previousPrice
    if (abs(delta) < 0.5) return

    val increased = delta > 0
    val containerColor = if (increased) PriceIncreaseContainer else PriceDecreaseContainer
    val contentColor = if (increased) PriceIncreaseRed else PriceDecreaseGreen
    val icon = if (increased) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
    val label = if (increased) "+${formatPkr(delta)} اضافہ" else "-${formatPkr(abs(delta))} کمی"

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = formatPkr(previousPrice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = TextDecoration.LineThrough
        )
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        OrderStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.DISPATCHED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        OrderStatus.COMPLETED -> PriceDecreaseContainer to PriceDecreaseGreen
        OrderStatus.CANCELLED -> PriceIncreaseContainer to PriceIncreaseRed
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = status.urduLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ShopHeroBanner(
    shopInfo: ShopInfo,
    latestUpdateTimestamp: Timestamp?,
    onWhatsAppClick: () -> Unit,
    onCallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_shop_hero),
                    contentDescription = shopInfo.businessName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x66062E1F),
                                    Color(0xE6083E29)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "ہول سیل کریانہ مرچنٹ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "آخری اپ ڈیٹ: ${formatTimestamp(latestUpdateTimestamp)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = shopInfo.businessName,
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(16.dp)
                            )
                            Text(
                                text = shopInfo.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (shopInfo.announcement.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = shopInfo.announcement,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AssistChip(
                        onClick = onWhatsAppClick,
                        label = {
                            Text(
                                text = "واٹس ایپ: ${shopInfo.whatsappNumber}",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Chat,
                                contentDescription = "واٹس ایپ",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(WhatsAppGreen, CircleShape)
                                    .padding(3.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = PriceDecreaseContainer,
                            labelColor = PriceDecreaseGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    AssistChip(
                        onClick = onCallClick,
                        label = {
                            Text(
                                text = "کال کریں",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = "فون کال",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(0.75f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateMessage(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = CircleShape,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
