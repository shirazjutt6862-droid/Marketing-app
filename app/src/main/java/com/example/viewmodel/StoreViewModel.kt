package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.StoreRepository
import com.example.data.firebase.StarterCatalogData
import com.example.data.model.AccountStatus
import com.example.data.model.BackupSnapshot
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PriceUpdate
import com.example.data.model.Product
import com.example.data.model.ShopInfo
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class StoreViewModel(
    private val repository: StoreRepository,
    val currentUserId: String,
    val isOwnerAdmin: Boolean
) : ViewModel() {

    private companion object {
        const val TAG = "StoreViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }

    private fun <T> Flow<T>.asUiState(label: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "ڈیٹا لوڈ کرنے میں مسئلہ پیش آیا۔"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _refreshTrigger = MutableStateFlow(0)

    fun retryConnection() {
        _refreshTrigger.value += 1
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val shopInfoState: StateFlow<UiState<ShopInfo>> = _refreshTrigger
        .flatMapLatest { repository.observeShopInfo() }
        .asUiState("shop_info")

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoriesState: StateFlow<UiState<List<Category>>> = _refreshTrigger
        .flatMapLatest { repository.observeCategories() }
        .asUiState("categories")

    @OptIn(ExperimentalCoroutinesApi::class)
    val productsState: StateFlow<UiState<List<Product>>> = _refreshTrigger
        .flatMapLatest { repository.observeProducts() }
        .asUiState("products")

    @OptIn(ExperimentalCoroutinesApi::class)
    val priceUpdatesState: StateFlow<UiState<List<PriceUpdate>>> = _refreshTrigger
        .flatMapLatest { repository.observePriceUpdates() }
        .asUiState("price_updates")

    @OptIn(ExperimentalCoroutinesApi::class)
    val myOrdersState: StateFlow<UiState<List<Order>>> = _refreshTrigger
        .flatMapLatest { repository.observeMyOrders(currentUserId) }
        .asUiState("my_orders")

    @OptIn(ExperimentalCoroutinesApi::class)
    val userProfileState: StateFlow<UiState<UserProfile?>> = _refreshTrigger
        .flatMapLatest { repository.observeUserProfile(currentUserId) }
        .asUiState("user_profile")

    @OptIn(ExperimentalCoroutinesApi::class)
    val allOrdersState: StateFlow<UiState<List<Order>>> =
        if (isOwnerAdmin) {
            _refreshTrigger.flatMapLatest { repository.observeAllOrdersForOwner() }.asUiState("all_orders")
        } else {
            MutableStateFlow<UiState<List<Order>>>(UiState.Success(emptyList())).asStateFlow()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val allUsersState: StateFlow<UiState<List<UserProfile>>> =
        if (isOwnerAdmin) {
            _refreshTrigger.flatMapLatest { repository.observeAllUsersForOwner() }.asUiState("all_users")
        } else {
            MutableStateFlow<UiState<List<UserProfile>>>(UiState.Success(emptyList())).asStateFlow()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val backupsState: StateFlow<UiState<List<BackupSnapshot>>> =
        if (isOwnerAdmin) {
            _refreshTrigger.flatMapLatest { repository.observeBackupsForOwner() }.asUiState("backups")
        } else {
            MutableStateFlow<UiState<List<BackupSnapshot>>>(UiState.Success(emptyList())).asStateFlow()
        }

    // Search & Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _offersOnly = MutableStateFlow(false)
    val offersOnly: StateFlow<Boolean> = _offersOnly.asStateFlow()

    private val _trendingOnly = MutableStateFlow(false)
    val trendingOnly: StateFlow<Boolean> = _trendingOnly.asStateFlow()

    val filteredProductsState: StateFlow<UiState<List<Product>>> = combine(
        productsState,
        searchQuery,
        selectedCategory,
        offersOnly,
        trendingOnly
    ) { state, query, category, onlyOffers, onlyTrending ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val q = query.trim().lowercase()
                val filtered = state.data.filter { prod ->
                    val matchesQuery = q.isEmpty() ||
                        prod.name.lowercase().contains(q) ||
                        prod.category.lowercase().contains(q) ||
                        prod.packingDetails.lowercase().contains(q)
                    val matchesCat = category == null || prod.category == category
                    val matchesOffer = !onlyOffers || prod.specialOffer
                    val matchesTrending = !onlyTrending || prod.trending
                    matchesQuery && matchesCat && matchesOffer && matchesTrending
                }
                UiState.Success(filtered)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    // Shopping Cart State
    private val _cartMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cartItems: StateFlow<List<CartItem>> = combine(productsState, _cartMap) { prodState, qtyMap ->
        val products = (prodState as? UiState.Success)?.data ?: emptyList()
        val byId = products.associateBy { it.id }
        qtyMap.entries.mapNotNull { (id, qty) ->
            val product = byId[id]
            if (product != null && qty > 0) CartItem(product, qty) else null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    val cartTotalCount: StateFlow<Int> = _cartMap
        .map { it.values.sum() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

    val cartTotalAmount: StateFlow<Double> = cartItems
        .map { items -> items.sumOf { it.lineTotal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0.0)

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents = _toastEvents.asSharedFlow()

    private var seededAttempted = false

    init {
        if (isOwnerAdmin) {
            seedStarterCatalogIfNeeded(silent = true)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleOffersOnly() {
        _offersOnly.value = !_offersOnly.value
    }

    fun toggleTrendingOnly() {
        _trendingOnly.value = !_trendingOnly.value
    }

    fun addToCart(product: Product, delta: Int = 1) {
        if (!product.available) {
            _toastEvents.tryEmit("یہ آئٹم فی الحال دستیاب نہیں ہے۔")
            return
        }
        val current = _cartMap.value.toMutableMap()
        val nextQty = (current[product.id] ?: 0) + delta
        if (nextQty <= 0) {
            current.remove(product.id)
        } else {
            current[product.id] = nextQty.coerceAtMost(999)
        }
        _cartMap.value = current
        if (delta > 0) {
            _toastEvents.tryEmit("${product.name} کارٹ میں شامل ہو گیا۔")
        }
    }

    fun setCartQuantity(productId: String, quantity: Int) {
        val current = _cartMap.value.toMutableMap()
        if (quantity <= 0) {
            current.remove(productId)
        } else {
            current[productId] = quantity.coerceAtMost(999)
        }
        _cartMap.value = current
    }

    fun removeFromCart(productId: String) {
        val current = _cartMap.value.toMutableMap()
        current.remove(productId)
        _cartMap.value = current
    }

    fun clearCart() {
        _cartMap.value = emptyMap()
    }

    fun submitOrder(
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        notes: String,
        onSuccessLaunchWhatsApp: (List<CartItem>, Double) -> Unit
    ) {
        val itemsSnapshot = cartItems.value
        val totalSnapshot = itemsSnapshot.sumOf { it.lineTotal }
        if (itemsSnapshot.isEmpty()) {
            _toastEvents.tryEmit("آرڈر بھیجنے سے پہلے کارٹ میں آئٹمز شامل کریں۔")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.submitOrder(
                customerName = customerName,
                customerPhone = customerPhone,
                customerAddress = customerAddress,
                cartItems = itemsSnapshot,
                notes = notes
            )
            _isBusy.value = false
            result.onSuccess {
                _cartMap.value = emptyMap()
                _toastEvents.tryEmit("آرڈر محفوظ ہو گیا، واٹس ایپ کھل رہا ہے...")
                onSuccessLaunchWhatsApp(itemsSnapshot, totalSnapshot)
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "آرڈر محفوظ کرنے میں مسئلہ پیش آیا۔")
            }
        }
    }

    fun cancelPendingOrder(orderId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.cancelPendingOrder(orderId)
            _isBusy.value = false
            res.onSuccess {
                _toastEvents.tryEmit("آرڈر منسوخ کر دیا گیا۔")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "آرڈر منسوخ نہیں ہو سکا۔")
            }
        }
    }

    fun registerCustomerProfile(
        phoneNumber: String,
        fullName: String,
        shopName: String,
        city: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.registerCustomerProfile(
                phoneNumber = phoneNumber,
                fullName = fullName,
                shopName = shopName,
                city = city
            )
            _isBusy.value = false
            res.onSuccess {
                _toastEvents.tryEmit("آپ کا اکاؤنٹ کامیابی سے رجسٹر ہو گیا ہے۔")
                onDone()
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "رجسٹریشن محفوظ کرنے میں خرابی آئی۔")
            }
        }
    }

    // ========================
    // OWNER / ADMIN ACTIONS
    // ========================

    fun seedStarterCatalogIfNeeded(silent: Boolean = false) {
        if (!isOwnerAdmin) return
        if (silent && seededAttempted) return
        seededAttempted = true
        viewModelScope.launch {
            if (!silent) _isBusy.value = true
            val res = repository.seedStarterCatalogIfEmpty()
            if (!silent) _isBusy.value = false
            res.onSuccess { seeded ->
                if (seeded || !silent) {
                    _toastEvents.tryEmit(
                        if (seeded) "ابتدائی کیٹیگریز اور تھوک اشیاء لوڈ ہو گئیں۔"
                        else "دکان کا ڈیٹا پہلے سے موجود ہے۔"
                    )
                }
            }.onFailure { err ->
                if (!silent) {
                    _toastEvents.tryEmit(err.localizedMessage ?: "ڈیٹا لوڈ کرنے میں خرابی آئی۔")
                }
            }
        }
    }

    fun saveShopInfo(info: ShopInfo, onDone: () -> Unit = {}) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveShopInfo(info)
            _isBusy.value = false
            res.onSuccess {
                _toastEvents.tryEmit("دکان کی معلومات محفوظ ہو گئیں۔")
                onDone()
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "محفوظ کرنے میں خرابی آئی۔")
            }
        }
    }

    fun saveCategory(
        categoryId: String,
        name: String,
        sortOrder: Long,
        iconKey: String,
        active: Boolean,
        onDone: () -> Unit = {}
    ) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveCategory(categoryId, name, sortOrder, iconKey, active)
            _isBusy.value = false
            res.onSuccess {
                _toastEvents.tryEmit("کیٹیگری محفوظ ہو گئی۔")
                onDone()
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "کیٹیگری محفوظ نہ ہو سکی۔")
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteCategory(categoryId)
            _isBusy.value = false
            res.onSuccess { _toastEvents.tryEmit("کیٹیگری حذف کر دی گئی۔") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "حذف نہ ہو سکی۔") }
        }
    }

    fun uploadProductImage(
        context: Context,
        imageUri: Uri,
        onUploaded: (String) -> Unit
    ) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _isUploadingImage.value = true
            val res = repository.uploadProductImage(context, imageUri)
            _isUploadingImage.value = false
            res.onSuccess { (urlOrData, usedFirebaseStorage) ->
                onUploaded(urlOrData)
                _toastEvents.tryEmit(
                    if (usedFirebaseStorage) "تصویر Firebase Storage پر اپ لوڈ ہو گئی۔"
                    else "تصویر کمپریس کر کے محفوظ کر لی گئی۔"
                )
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "تصویر اپ لوڈ کرنے میں مسئلہ پیش آیا۔")
            }
        }
    }

    fun saveProduct(
        existingProduct: Product?,
        name: String,
        category: String,
        packingDetails: String,
        wholesalePrice: Double,
        retailPrice: Double,
        available: Boolean,
        specialOffer: Boolean,
        trending: Boolean,
        stockCount: Long,
        offerText: String,
        imageData: String,
        onDone: () -> Unit = {}
    ) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveProduct(
                existingProduct = existingProduct,
                name = name,
                category = category,
                packingDetails = packingDetails,
                wholesalePrice = wholesalePrice,
                retailPrice = retailPrice,
                available = available,
                specialOffer = specialOffer,
                trending = trending,
                stockCount = stockCount,
                offerText = offerText,
                imageData = imageData
            )
            _isBusy.value = false
            res.onSuccess {
                _toastEvents.tryEmit("پروڈکٹ محفوظ ہو گیا۔")
                onDone()
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "پروڈکٹ محفوظ نہ ہو سکا۔")
            }
        }
    }

    fun applyDailyRateUpdate(product: Product, newWholesalePrice: Double, newRetailPrice: Double) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.applyDailyRateUpdate(product, newWholesalePrice, newRetailPrice)
            _isBusy.value = false
            res.onSuccess {
                _toastEvents.tryEmit("${product.name} کا نیا ریٹ فوری طور پر اپ ڈیٹ ہو گیا!")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "ریٹ اپ ڈیٹ نہ ہو سکا۔")
            }
        }
    }

    fun toggleProductAvailability(product: Product, available: Boolean) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            val res = repository.toggleProductAvailability(product, available)
            res.onSuccess {
                _toastEvents.tryEmit(if (available) "دستیاب کر دیا گیا۔" else "اسٹاک ختم قرار دیا گیا۔")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "تبدیلی ناکام رہی۔")
            }
        }
    }

    fun toggleProductTrending(product: Product, trending: Boolean) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            val res = repository.toggleProductTrending(product, trending)
            res.onSuccess {
                _toastEvents.tryEmit(if (trending) "ٹرینڈنگ میں شامل کر دیا گیا۔" else "ٹرینڈنگ سے ہٹا دیا گیا۔")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "تبدیلی ناکام رہی۔")
            }
        }
    }

    fun toggleProductOffer(product: Product, specialOffer: Boolean) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            val res = repository.toggleProductOffer(product, specialOffer)
            res.onSuccess {
                _toastEvents.tryEmit(if (specialOffer) "خصوصی آفر فعال کر دی گئی۔" else "خصوصی آفر ختم کر دی گئی۔")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "تبدیلی ناکام رہی۔")
            }
        }
    }

    fun updateProductStock(product: Product, available: Boolean, stockCount: Long) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            val res = repository.updateProductStock(product, available, stockCount)
            res.onSuccess {
                _toastEvents.tryEmit("اسٹاک کی تفصیل اپ ڈیٹ ہو گئی۔")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "اسٹاک اپ ڈیٹ نہ ہو سکا۔")
            }
        }
    }

    fun deleteProduct(productId: String) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteProduct(productId)
            _isBusy.value = false
            res.onSuccess { _toastEvents.tryEmit("پروڈکٹ حذف کر دیا گیا۔") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "پروڈکٹ حذف نہ ہو سکا۔") }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            val res = repository.updateOrderStatus(orderId, status)
            res.onSuccess { _toastEvents.tryEmit("آرڈر اسٹیٹس اپ ڈیٹ ہو گیا۔") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "اسٹیٹس اپ ڈیٹ نہ ہو سکا۔") }
        }
    }

    fun updateUserAccountStatus(targetUserId: String, status: AccountStatus) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateUserAccountStatus(targetUserId, status)
            _isBusy.value = false
            res.onSuccess {
                val msg = when (status) {
                    AccountStatus.APPROVED -> "صارف کا اکاؤنٹ منظور کر لیا گیا۔"
                    AccountStatus.BLOCKED -> "صارف کا اکاؤنٹ بلاک کر دیا گیا۔"
                    AccountStatus.PENDING -> "صارف کا اکاؤنٹ زیرِ التواء کر دیا گیا۔"
                }
                _toastEvents.tryEmit(msg)
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "صارف کا اسٹیٹس تبدیل نہ ہو سکا۔")
            }
        }
    }

    fun createCloudBackup(title: String) {
        if (!isOwnerAdmin) return
        val prods = (productsState.value as? UiState.Success)?.data ?: emptyList()
        val cats = (categoriesState.value as? UiState.Success)?.data ?: emptyList()
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createCloudBackup(title, prods, cats)
            _isBusy.value = false
            res.onSuccess { _toastEvents.tryEmit("کلاؤڈ بیک اپ محفوظ ہو گیا۔") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "بیک اپ ناکام رہا۔") }
        }
    }

    fun restoreFromCloudBackup(backup: BackupSnapshot) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.restoreFromCloudBackup(backup)
            _isBusy.value = false
            res.onSuccess { _toastEvents.tryEmit("بیک اپ کامیابی سے بحال ہو گیا۔") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "ری اسٹور ناکام رہا۔") }
        }
    }

    fun buildExportBackupJson(): String {
        val shop = (shopInfoState.value as? UiState.Success)?.data ?: StarterCatalogData.defaultShopInfo
        val cats = (categoriesState.value as? UiState.Success)?.data ?: emptyList()
        val prods = (productsState.value as? UiState.Success)?.data ?: emptyList()
        return repository.exportCatalogToJson(shop, cats, prods)
    }

    fun importBackupJson(jsonString: String) {
        if (!isOwnerAdmin) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.importCatalogFromJson(jsonString)
            _isBusy.value = false
            res.onSuccess { count ->
                _toastEvents.tryEmit("$count آئٹمز کامیابی سے امپورٹ ہو گئے۔")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "فائل امپورٹ ناکام رہی۔")
            }
        }
    }
}
