package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.StarterCatalogData
import com.example.data.model.AccountStatus
import com.example.data.model.BackupSnapshot
import com.example.data.model.Category
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.ShopInfo
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.OrderStatusChip
import com.example.ui.components.ProductImage
import com.example.util.formatPkr
import com.example.util.formatTimestamp
import com.example.util.openDialer
import com.example.util.openWhatsAppChat
import com.example.viewmodel.StoreViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: StoreViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabHistory = remember { mutableStateListOf(0) }
    var tabHistoryIndex by remember { mutableIntStateOf(0) }
    val selectedTab = tabHistory.getOrElse(tabHistoryIndex) { 0 }

    fun selectAdminTab(nextTab: Int) {
        if (selectedTab == nextTab) return
        while (tabHistory.size > tabHistoryIndex + 1) {
            tabHistory.removeAt(tabHistory.lastIndex)
        }
        tabHistory.add(nextTab)
        tabHistoryIndex = tabHistory.lastIndex
    }

    fun stepAdminBack() {
        if (tabHistoryIndex > 0) {
            tabHistoryIndex -= 1
        } else {
            onBack()
        }
    }

    fun stepAdminForward() {
        if (tabHistoryIndex < tabHistory.lastIndex) {
            tabHistoryIndex += 1
        }
    }

    BackHandler { stepAdminBack() }

    val context = LocalContext.current
    val shopState by viewModel.shopInfoState.collectAsStateWithLifecycle()
    val categoriesState by viewModel.categoriesState.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val ordersState by viewModel.allOrdersState.collectAsStateWithLifecycle()
    val usersState by viewModel.allUsersState.collectAsStateWithLifecycle()
    val updatesState by viewModel.priceUpdatesState.collectAsStateWithLifecycle()
    val backupsState by viewModel.backupsState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val isUploadingImage by viewModel.isUploadingImage.collectAsStateWithLifecycle()

    val shopInfo = (shopState as? UiState.Success)?.data ?: StarterCatalogData.defaultShopInfo
    val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()
    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val orders = (ordersState as? UiState.Success)?.data ?: emptyList()
    val users = (usersState as? UiState.Success)?.data ?: emptyList()
    val priceUpdates = (updatesState as? UiState.Success)?.data ?: emptyList()
    val backups = (backupsState as? UiState.Success)?.data ?: emptyList()
    val isTabDataLoading = productsState is UiState.Loading ||
        ordersState is UiState.Loading ||
        usersState is UiState.Loading

    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showNewProductDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val json = viewModel.buildExportBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input)).readText()
                }
            }.getOrNull()
            if (!content.isNullOrBlank()) {
                viewModel.importBackupJson(content)
            }
        }
    }

    val adminTabs = listOf(
        "جائزہ" to Icons.Filled.Dashboard,
        "رپورٹس" to Icons.Filled.BarChart,
        "روزانہ ریٹس" to Icons.Filled.PriceChange,
        "مصنوعات" to Icons.Filled.Inventory2,
        "کیٹیگریز" to Icons.Filled.Category,
        "آرڈرز (${orders.size})" to Icons.Filled.ReceiptLong,
        "صارفین (${users.size})" to Icons.Filled.People,
        "سیٹنگز و بیک اپ" to Icons.Filled.Settings
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("سپر ایڈمن کنٹرول پینل", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { stepAdminBack() }, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "واپس")
                    }
                },
                actions = {
                    if (tabHistoryIndex < tabHistory.lastIndex) {
                        IconButton(onClick = { stepAdminForward() }, modifier = Modifier.testTag("admin_forward_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "آگے")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                3 -> FloatingActionButton(
                    onClick = { showNewProductDialog = true },
                    modifier = Modifier.testTag("admin_add_product_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "نیا پروڈکٹ شامل کریں")
                }
                4 -> FloatingActionButton(
                    onClick = { showNewCategoryDialog = true },
                    modifier = Modifier.testTag("admin_add_category_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "نئی کیٹیگری شامل کریں")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) {
                adminTabs.forEachIndexed { idx, (label, icon) ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectAdminTab(idx) },
                        text = { Text(label) },
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
            if (isBusy || isTabDataLoading) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (selectedTab) {
                0 -> AdminOverviewTab(
                    products = products,
                    categories = categories,
                    orders = orders,
                    users = users,
                    priceUpdatesCount = priceUpdates.size,
                    isBusy = isBusy,
                    onSeedStarter = { viewModel.seedStarterCatalogIfNeeded(silent = false) },
                    onJumpTab = { selectAdminTab(it) }
                )
                1 -> AdminSalesReportsTab(
                    orders = orders
                )
                2 -> AdminDailyRatesTab(
                    products = products,
                    isBusy = isBusy,
                    onSaveRate = { prod, newWs, newRt ->
                        viewModel.applyDailyRateUpdate(prod, newWs, newRt)
                    }
                )
                3 -> AdminProductsTab(
                    products = products,
                    onEdit = { editingProduct = it },
                    onToggleAvailable = { prod, avail -> viewModel.toggleProductAvailability(prod, avail) },
                    onToggleTrending = { prod, trending -> viewModel.toggleProductTrending(prod, trending) },
                    onToggleOffer = { prod, offer -> viewModel.toggleProductOffer(prod, offer) },
                    onDelete = { viewModel.deleteProduct(it.id) }
                )
                4 -> AdminCategoriesTab(
                    categories = categories,
                    onEdit = { editingCategory = it },
                    onDelete = { viewModel.deleteCategory(it.id) }
                )
                5 -> AdminOrdersTab(
                    orders = orders,
                    onUpdateStatus = { id, status -> viewModel.updateOrderStatus(id, status) },
                    onCallCustomer = { order -> openDialer(context, order.customerPhone) },
                    onContactCustomer = { order ->
                        openWhatsAppChat(
                            context,
                            order.customerPhone,
                            "السلام علیکم ${order.customerName}! من سلوٰی ٹریڈر سے آپ کے آرڈر (${formatPkr(order.totalAmount)}) کے بارے میں رابطہ کر رہے ہیں۔"
                        )
                    }
                )
                6 -> AdminUsersTab(
                    users = users,
                    isBusy = isBusy,
                    onUpdateStatus = { targetUserId, nextStatus ->
                        viewModel.updateUserAccountStatus(targetUserId, nextStatus)
                    }
                )
                7 -> AdminSettingsBackupTab(
                    shopInfo = shopInfo,
                    backups = backups,
                    isBusy = isBusy,
                    onSaveShopInfo = { viewModel.saveShopInfo(it) },
                    onCreateCloudBackup = { title -> viewModel.createCloudBackup(title) },
                    onRestoreCloudBackup = { viewModel.restoreFromCloudBackup(it) },
                    onExportJson = { exportLauncher.launch("mann_salwa_backup.json") },
                    onImportJson = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                )
            }
        }
    }

    if (showNewProductDialog || editingProduct != null) {
        ProductEditorDialog(
            existingProduct = editingProduct,
            categories = categories,
            isBusy = isBusy,
            isUploadingImage = isUploadingImage,
            onUploadImage = { uri, onUploaded ->
                viewModel.uploadProductImage(context, uri, onUploaded)
            },
            onDismiss = {
                showNewProductDialog = false
                editingProduct = null
            },
            onSave = { name, cat, packing, ws, rt, avail, offer, trending, stockCount, offerTxt, img ->
                viewModel.saveProduct(
                    existingProduct = editingProduct,
                    name = name,
                    category = cat,
                    packingDetails = packing,
                    wholesalePrice = ws,
                    retailPrice = rt,
                    available = avail,
                    specialOffer = offer,
                    trending = trending,
                    stockCount = stockCount,
                    offerText = offerTxt,
                    imageData = img
                ) {
                    showNewProductDialog = false
                    editingProduct = null
                }
            }
        )
    }

    if (showNewCategoryDialog || editingCategory != null) {
        CategoryEditorDialog(
            existingCategory = editingCategory,
            nextOrder = (categories.maxOfOrNull { it.sortOrder } ?: 0L) + 1L,
            isBusy = isBusy,
            onDismiss = {
                showNewCategoryDialog = false
                editingCategory = null
            },
            onSave = { name, sortOrder, iconKey, active ->
                viewModel.saveCategory(
                    categoryId = editingCategory?.id ?: "",
                    name = name,
                    sortOrder = sortOrder,
                    iconKey = iconKey,
                    active = active
                ) {
                    showNewCategoryDialog = false
                    editingCategory = null
                }
            }
        )
    }
}

@Composable
private fun AdminOverviewTab(
    products: List<Product>,
    categories: List<Category>,
    orders: List<Order>,
    users: List<UserProfile>,
    priceUpdatesCount: Int,
    isBusy: Boolean,
    onSeedStarter: () -> Unit,
    onJumpTab: (Int) -> Unit
) {
    val availableCount = products.count { it.available }
    val offersCount = products.count { it.specialOffer }
    val pendingOrders = orders.count { it.parsedStatus() == OrderStatus.PENDING }
    val totalOrderVolume = orders.filter { it.parsedStatus() != OrderStatus.CANCELLED }.sumOf { it.totalAmount }
    val pendingUsers = users.count { it.parsedStatus() == AccountStatus.PENDING }
    val approvedUsers = users.count { it.parsedStatus() == AccountStatus.APPROVED }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "کاروباری اعداد و شمار",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("کل مصنوعات", products.size.toString(), "دستیاب: $availableCount", Modifier.weight(1f))
                StatCard("کل آرڈرز", orders.size.toString(), "زیرِ التواء: $pendingOrders", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("رجسٹرڈ صارفین", users.size.toString(), "منظوری کے منتظر: $pendingUsers | منظور: $approvedUsers", Modifier.weight(1f))
                StatCard("آرڈرز کی مالیت", formatPkr(totalOrderVolume), "آفرز: $offersCount • کیٹیگریز: ${categories.size} • ریٹس: $priceUpdatesCount", Modifier.weight(1f))
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("فوری کنٹرولز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onJumpTab(2) }, modifier = Modifier.weight(1f)) {
                            Text("آج کے ریٹس اپ ڈیٹ")
                        }
                        OutlinedButton(onClick = { onJumpTab(1) }, modifier = Modifier.weight(1f)) {
                            Text("سیلز رپورٹس")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onJumpTab(3) }, modifier = Modifier.weight(1f)) {
                            Text("نیا سامان شامل کریں")
                        }
                        OutlinedButton(onClick = { onJumpTab(6) }, modifier = Modifier.weight(1f)) {
                            Text("صارفین (${pendingUsers} منتظر)")
                        }
                    }
                    OutlinedButton(
                        onClick = onSeedStarter,
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("خالی اسٹور میں بنیادی کیٹیگریز اور تھوک اشیاء لوڈ کریں")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdminDailyRatesTab(
    products: List<Product>,
    isBusy: Boolean,
    onSaveRate: (Product, Double, Double) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "کسی بھی پروڈکٹ کی نئی ہول سیل اور پرچون قیمت لکھ کر محفوظ کریں — کسٹمرز کے پاس ریٹ فوراً اپ ڈیٹ ہو جائے گا اور گزشتہ قیمت کے ساتھ فرق نظر آئے گا۔",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        items(products, key = { it.id }) { product ->
            DailyRateEditorRow(product = product, isBusy = isBusy, onSaveRate = onSaveRate)
        }
    }
}

@Composable
private fun DailyRateEditorRow(
    product: Product,
    isBusy: Boolean,
    onSaveRate: (Product, Double, Double) -> Unit
) {
    var wsInput by remember(product.id, product.wholesalePrice) {
        mutableStateOf(product.wholesalePrice.toLong().toString())
    }
    var rtInput by remember(product.id, product.retailPrice) {
        mutableStateOf(product.retailPrice.toLong().toString())
    }

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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${product.category} • ${product.packingDetails}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "موجودہ: ${formatPkr(product.wholesalePrice)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = wsInput,
                    onValueChange = { wsInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("نیا ہول سیل") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = rtInput,
                    onValueChange = { rtInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("نیا پرچون") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
                Button(
                    enabled = !isBusy,
                    onClick = {
                        val ws = wsInput.toDoubleOrNull()
                        val rt = rtInput.toDoubleOrNull()
                        if (ws != null && rt != null && ws >= 0 && rt >= 0) {
                            onSaveRate(product, ws, rt)
                        }
                    },
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "محفوظ کریں")
                }
            }
        }
    }
}

@Composable
private fun AdminProductsTab(
    products: List<Product>,
    onEdit: (Product) -> Unit,
    onToggleAvailable: (Product, Boolean) -> Unit,
    onToggleTrending: (Product, Boolean) -> Unit,
    onToggleOffer: (Product, Boolean) -> Unit,
    onDelete: (Product) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(products, key = { it.id }) { prod ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductImage(
                            imageData = prod.imageData,
                            category = prod.category,
                            contentDescription = prod.name,
                            modifier = Modifier.size(58.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            val stockText = if (prod.stockCount > 0L) " • اسٹاک: ${prod.stockCount}" else ""
                            Text("${prod.category} • ${prod.packingDetails}$stockText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("ہول سیل: ${formatPkr(prod.wholesalePrice)} | پرچون: ${formatPkr(prod.retailPrice)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Switch(
                                checked = prod.available,
                                onCheckedChange = { onToggleAvailable(prod, it) }
                            )
                            Row {
                                IconButton(onClick = { onEdit(prod) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "ترمیم")
                                }
                                IconButton(onClick = { onDelete(prod) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = prod.trending,
                            onClick = { onToggleTrending(prod, !prod.trending) },
                            label = { Text("ٹرینڈنگ") },
                            leadingIcon = {
                                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        FilterChip(
                            selected = prod.specialOffer,
                            onClick = { onToggleOffer(prod, !prod.specialOffer) },
                            label = { Text("خصوصی آفر") },
                            leadingIcon = {
                                Icon(Icons.Filled.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun AdminCategoriesTab(
    categories: List<Category>,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories, key = { it.id }) { cat ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(cat.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "ترتیب: ${cat.sortOrder} • ${if (cat.active) "فعال" else "غیر فعال"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        IconButton(onClick = { onEdit(cat) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "ترمیم")
                        }
                        IconButton(onClick = { onDelete(cat) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun AdminOrdersTab(
    orders: List<Order>,
    onUpdateStatus: (String, OrderStatus) -> Unit,
    onCallCustomer: (Order) -> Unit,
    onContactCustomer: (Order) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(orders, key = { it.id }) { order ->
            val currentStatus = order.parsedStatus()
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
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("کسٹمر: ${order.customerName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("فون: ${order.customerPhone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text("ڈلیوری پتہ: ${order.customerAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatTimestamp(order.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OrderStatusChip(status = currentStatus)
                    }

                    HorizontalDivider()
                    order.parsedItems().forEach { line ->
                        Text(
                            text = "• ${line.name} (${line.packing}) × ${line.qty} = ${formatPkr(line.unitPrice * line.qty)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (order.notes.isNotBlank()) {
                        Text("ہدایت: ${order.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("کل بل: ${formatPkr(order.totalAmount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { onCallCustomer(order) }) {
                                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("کال")
                            }
                            OutlinedButton(onClick = { onContactCustomer(order) }) {
                                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("واٹس ایپ")
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(OrderStatus.entries) { st ->
                            FilterChip(
                                selected = currentStatus == st,
                                onClick = { onUpdateStatus(order.id, st) },
                                label = { Text(st.urduLabel) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSettingsBackupTab(
    shopInfo: ShopInfo,
    backups: List<BackupSnapshot>,
    isBusy: Boolean,
    onSaveShopInfo: (ShopInfo) -> Unit,
    onCreateCloudBackup: (String) -> Unit,
    onRestoreCloudBackup: (BackupSnapshot) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit
) {
    var businessName by remember(shopInfo) { mutableStateOf(shopInfo.businessName) }
    var address by remember(shopInfo) { mutableStateOf(shopInfo.address) }
    var contact1Name by remember(shopInfo) { mutableStateOf(shopInfo.contact1Name) }
    var contact1Phone by remember(shopInfo) { mutableStateOf(shopInfo.contact1Phone) }
    var contact2Name by remember(shopInfo) { mutableStateOf(shopInfo.contact2Name) }
    var contact2Phone by remember(shopInfo) { mutableStateOf(shopInfo.contact2Phone) }
    var whatsappNumber by remember(shopInfo) { mutableStateOf(shopInfo.whatsappNumber) }
    var announcement by remember(shopInfo) { mutableStateOf(shopInfo.announcement) }
    var backupTitle by remember { mutableStateOf("روزانہ بیک اپ") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("دکان کی معلومات اور رابطہ نمبرز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = businessName, onValueChange = { businessName = it }, label = { Text("دکان کا نام") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("مکمل پتہ") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = contact1Name, onValueChange = { contact1Name = it }, label = { Text("رابطہ 1 نام") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = contact1Phone, onValueChange = { contact1Phone = it }, label = { Text("نمبر 1") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = contact2Name, onValueChange = { contact2Name = it }, label = { Text("رابطہ 2 نام") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = contact2Phone, onValueChange = { contact2Phone = it }, label = { Text("نمبر 2") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = whatsappNumber, onValueChange = { whatsappNumber = it }, label = { Text("واٹس ایپ آرڈر نمبر") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = announcement, onValueChange = { announcement = it }, label = { Text("ہوم اسکرین اعلان / پیغام") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                    Button(
                        enabled = !isBusy,
                        onClick = {
                            onSaveShopInfo(
                                shopInfo.copy(
                                    businessName = businessName,
                                    address = address,
                                    contact1Name = contact1Name,
                                    contact1Phone = contact1Phone,
                                    contact2Name = contact2Name,
                                    contact2Phone = contact2Phone,
                                    whatsappNumber = whatsappNumber,
                                    announcement = announcement
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("دکان کی معلومات محفوظ کریں")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("بیک اپ اور ری اسٹور (Backup & Restore)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = backupTitle,
                        onValueChange = { backupTitle = it },
                        label = { Text("بیک اپ کا نام") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = !isBusy,
                        onClick = { onCreateCloudBackup(backupTitle) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Backup, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("کلاؤڈ بیک اپ محفوظ کریں")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onExportJson, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("JSON ایکسپورٹ")
                        }
                        OutlinedButton(onClick = onImportJson, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("JSON امپورٹ")
                        }
                    }
                }
            }
        }

        items(backups, key = { it.id }) { bkp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(bkp.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${bkp.productCount} آئٹمز • ${bkp.categoryCount} کیٹیگریز", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatTimestamp(bkp.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(
                        enabled = !isBusy,
                        onClick = { onRestoreCloudBackup(bkp) }
                    ) {
                        Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ری اسٹور")
                    }
                }
            }
        }
    }
}
