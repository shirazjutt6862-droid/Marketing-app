package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.firebase.StarterCatalogData
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.UiState
import com.example.ui.components.EmptyStateMessage
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.ProductImage
import com.example.ui.theme.WhatsAppGreen
import com.example.util.buildWhatsAppOrderMessage
import com.example.util.formatPkr
import com.example.util.formatTimestamp
import com.example.util.openWhatsAppChat
import com.example.viewmodel.StoreViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun CartAndOrdersScreen(
    viewModel: StoreViewModel,
    onBrowseProducts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val totalAmount by viewModel.cartTotalAmount.collectAsStateWithLifecycle()
    val shopState by viewModel.shopInfoState.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val myOrdersState by viewModel.myOrdersState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val shopInfo = (shopState as? UiState.Success)?.data ?: StarterCatalogData.defaultShopInfo
    val savedProfile = (profileState as? UiState.Success)?.data
    val myOrders = (myOrdersState as? UiState.Success)?.data ?: emptyList()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Cart, 1 = My Orders
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }
    var customerName by remember { mutableStateOf(Firebase.auth.currentUser?.displayName ?: "") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var orderNotes by remember { mutableStateOf("") }

    LaunchedEffect(savedProfile) {
        if (savedProfile != null) {
            if (customerName.isBlank()) customerName = savedProfile.displayName
            if (customerPhone.isBlank()) customerPhone = savedProfile.phoneNumber
            if (customerAddress.isBlank()) {
                customerAddress = savedProfile.shopOrAddress.ifBlank {
                    listOf(savedProfile.shopName, savedProfile.city)
                        .filter { it.isNotBlank() }
                        .joinToString("، ")
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("شاپنگ کارٹ (${cartItems.sumOf { it.quantity }})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("میرے آرڈرز (${myOrders.size})") }
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (cartItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_empty_cart),
                                    contentDescription = "خالی کارٹ",
                                    modifier = Modifier
                                        .size(160.dp)
                                        .clip(RoundedCornerShape(20.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = "آپ کا کارٹ فی الحال خالی ہے",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ہول سیل اسٹور سے آٹا، چاول، گھی، چینی یا دیگر کریانہ اشیاء منتخب کریں اور واٹس ایپ پر فوری آرڈر بھیجیں۔",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onBrowseProducts,
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("مصنوعات دیکھیں")
                                }
                            }
                        }
                    }
                } else {
                    items(cartItems, key = { it.product.id }) { item ->
                        CartLineCard(
                            item = item,
                            onChangeQty = { next -> viewModel.setCartQuantity(item.product.id, next) },
                            onRemove = { viewModel.removeFromCart(item.product.id) }
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "آرڈر کی تفصیل اور کل رقم",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(onClick = { viewModel.clearCart() }) {
                                        Text("کارٹ خالی کریں")
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "کل تخمینی بل (${cartItems.sumOf { it.quantity }} آئٹمز):",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = formatPkr(totalAmount),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = customerName,
                                    onValueChange = { customerName = it },
                                    label = { Text("آپ کا نام / دکان کا نام *") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("checkout_name_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = customerPhone,
                                    onValueChange = { customerPhone = it },
                                    label = { Text("موبائل / واٹس ایپ نمبر *") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("checkout_phone_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = customerAddress,
                                    onValueChange = { customerAddress = it },
                                    label = { Text("مکمل پتہ / علاقہ *") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("checkout_address_input"),
                                    maxLines = 2,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = orderNotes,
                                    onValueChange = { orderNotes = it },
                                    label = { Text("اضافی ہدایت (اختیاری)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Button(
                                    onClick = {
                                        viewModel.submitOrder(
                                            customerName = customerName,
                                            customerPhone = customerPhone,
                                            customerAddress = customerAddress,
                                            notes = orderNotes
                                        ) { submittedItems, submittedTotal ->
                                            val msg = buildWhatsAppOrderMessage(
                                                shopInfo = shopInfo,
                                                customerName = customerName.trim(),
                                                customerPhone = customerPhone.trim(),
                                                customerAddress = customerAddress.trim(),
                                                cartItems = submittedItems,
                                                totalAmount = submittedTotal,
                                                notes = orderNotes.trim()
                                            )
                                            openWhatsAppChat(context, shopInfo.whatsappNumber, msg)
                                            selectedTab = 1
                                        }
                                    },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("submit_whatsapp_order_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    if (isBusy) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Filled.Chat, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "واٹس ایپ پر آرڈر بھیجیں (${formatPkr(totalAmount)})",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (myOrders.isEmpty()) {
                    item {
                        EmptyStateMessage(
                            icon = Icons.Filled.ReceiptLong,
                            title = "ابھی تک کوئی آرڈر نہیں",
                            subtitle = "جب آپ کارٹ سے واٹس ایپ آرڈر بھیجیں گے تو اس کی تفصیل اور اسٹیٹس یہاں محفوظ رہے گا۔"
                        )
                    }
                } else {
                    items(myOrders, key = { it.id }) { order ->
                        CustomerOrderCard(
                            order = order,
                            onCancel = { viewModel.cancelPendingOrder(order.id) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun CartLineCard(
    item: CartItem,
    onChangeQty: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImage(
                imageData = item.product.imageData,
                category = item.product.category,
                contentDescription = item.product.name,
                modifier = Modifier.size(64.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.product.packingDetails} • ${formatPkr(item.product.wholesalePrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "مجموعی: ${formatPkr(item.lineTotal)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onChangeQty(item.quantity - 1) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "کم کریں")
                    }
                    Text(
                        text = item.quantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { onChangeQty(item.quantity + 1) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "بڑھائیں")
                    }
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "حذف کریں",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerOrderCard(
    order: Order,
    onCancel: () -> Unit
) {
    val parsedStatus = order.parsedStatus()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(order.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OrderStatusChip(status = parsedStatus)
            }

            HorizontalDivider()

            order.parsedItems().forEach { line ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${line.name} (${line.packing}) × ${line.qty}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatPkr(line.unitPrice * line.qty),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "کل رقم (${order.itemCount} آئٹمز):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatPkr(order.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (parsedStatus == OrderStatus.PENDING) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("آرڈر منسوخ کریں")
                }
            }
        }
    }
}
