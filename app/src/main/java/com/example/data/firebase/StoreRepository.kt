package com.example.data.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.model.BackupSnapshot
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.Order
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.PriceUpdate
import com.example.data.model.Product
import com.example.data.model.SHOP_ID_CONSTANT
import com.example.data.model.SHOP_INFO_DOC_ID
import com.example.data.model.ShopInfo
import com.example.data.model.UserProfile
import com.example.util.uriToCompressedBase64DataUri
import com.example.util.uriToCompressedJpegBytes
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class StoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("براہِ کرم پہلے لاگ ان کریں۔")
    }

    private fun <T> Flow<T>.logFirestoreErrors(
        uid: String,
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    fun observeShopInfo(): Flow<ShopInfo> = flow {
        val uid = requireUserId()
        val path = "shop_info/$SHOP_INFO_DOC_ID"
        emitAll(
            db.collection("shop_info").document(SHOP_INFO_DOC_ID)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            ShopInfo::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: StarterCatalogData.defaultShopInfo
                    } else {
                        StarterCatalogData.defaultShopInfo
                    }
                }
                .logFirestoreErrors(uid, OperationType.GET, path)
        )
    }

    fun observeCategories(): Flow<List<Category>> = flow {
        val uid = requireUserId()
        val path = "categories"
        emitAll(
            db.collection("categories")
                .whereEqualTo("shopId", SHOP_ID_CONSTANT)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Category::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.sortOrder }
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeProducts(): Flow<List<Product>> = flow {
        val uid = requireUserId()
        val path = "products"
        emitAll(
            db.collection("products")
                .whereEqualTo("shopId", SHOP_ID_CONSTANT)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.priceUpdatedAt ?: it.updatedAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observePriceUpdates(): Flow<List<PriceUpdate>> = flow {
        val uid = requireUserId()
        val path = "price_updates"
        emitAll(
            db.collection("price_updates")
                .whereEqualTo("shopId", SHOP_ID_CONSTANT)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PriceUpdate::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeMyOrders(userId: String): Flow<List<Order>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "سائن ان یوزر تبدیل ہو گیا ہے۔" }
        val path = "orders"
        emitAll(
            db.collection("orders")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeAllOrdersForOwner(): Flow<List<Order>> = flow {
        val uid = requireUserId()
        val path = "orders"
        emitAll(
            db.collection("orders")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "سائن ان یوزر تبدیل ہو گیا ہے۔" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .logFirestoreErrors(uid, OperationType.GET, path)
        )
    }

    fun observeAllUsersForOwner(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeBackupsForOwner(): Flow<List<BackupSnapshot>> = flow {
        val uid = requireUserId()
        val path = "backups"
        emitAll(
            db.collection("backups")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BackupSnapshot::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    suspend fun registerCustomerProfile(
        phoneNumber: String,
        fullName: String,
        shopName: String,
        city: String
    ): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            val cleanPhone = (auth.currentUser?.phoneNumber?.takeIf { it.isNotBlank() } ?: phoneNumber)
                .trim().take(30)
            val cleanName = fullName.trim().take(100)
            val cleanShop = shopName.trim().take(120)
            val cleanCity = city.trim().take(80)

            require(cleanPhone.length >= 8) { "تصدیق شدہ موبائل نمبر موجود نہیں ہے۔ براہِ کرم موبائل نمبر اور OTP سے لاگ ان کریں۔" }
            require(cleanName.isNotEmpty()) { "براہِ کرم اپنا پورا نام درج کریں۔" }
            require(cleanShop.isNotEmpty()) { "براہِ کرم اپنی دکان کا نام درج کریں۔" }
            require(cleanCity.isNotEmpty()) { "براہِ کرم اپنے شہر کا نام درج کریں۔" }

            val combinedAddress = "$cleanShop، $cleanCity".take(300)
            val existingUser = userRef.get().await()

            if (existingUser.exists()) {
                userRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "shopName" to cleanShop,
                        "city" to cleanCity,
                        "shopOrAddress" to combinedAddress,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val registryId = com.example.util.phoneToRegistryDocId(cleanPhone)
                val registryRef = db.collection("phone_registry").document(registryId)
                val existingClaim = registryRef.get().await()
                if (existingClaim.exists() && existingClaim.getString("userId") != uid) {
                    throw IllegalStateException("اس موبائل نمبر پر پہلے سے ایک اکاؤنٹ رجسٹرڈ ہے۔")
                }

                val batch = db.batch()
                batch.set(
                    userRef,
                    mapOf(
                        "userId" to uid,
                        "phoneNumber" to cleanPhone,
                        "displayName" to cleanName,
                        "shopName" to cleanShop,
                        "city" to cleanCity,
                        "shopOrAddress" to combinedAddress,
                        "accountStatus" to com.example.data.model.AccountStatus.PENDING.wireValue,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (!existingClaim.exists()) {
                    batch.set(
                        registryRef,
                        mapOf(
                            "userId" to uid,
                            "phoneNumber" to cleanPhone,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: IllegalStateException) {
            Result.failure(e)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserAccountStatus(
        targetUserId: String,
        newStatus: com.example.data.model.AccountStatus
    ): Result<Unit> {
        val docPath = "users/$targetUserId"
        return try {
            requireUserId()
            require(targetUserId.isNotBlank()) { "صارف کی آئی ڈی غلط ہے۔" }
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "accountStatus" to newStatus.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(
        displayName: String,
        phone: String,
        shopOrAddress: String
    ): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val cleanName = displayName.trim().take(100)
            require(cleanName.isNotEmpty()) { "براہِ کرم اپنا نام درج کریں۔" }
            val cleanAddr = shopOrAddress.trim().take(300)

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "shopOrAddress" to cleanAddr,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitOrder(
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        cartItems: List<CartItem>,
        notes: String
    ): Result<String> {
        var docPath = "orders"
        return try {
            val uid = requireUserId()
            val cleanName = customerName.trim().take(100)
            val cleanPhone = customerPhone.trim().take(30)
            val cleanAddress = customerAddress.trim().take(300)
            val cleanNotes = notes.trim().take(500)

            require(cleanName.isNotEmpty()) { "کسٹمر یا دکان کا نام ضروری ہے۔" }
            require(cleanPhone.isNotEmpty()) { "فون نمبر ضروری ہے۔" }
            require(cleanAddress.isNotEmpty()) { "پتہ درج کرنا ضروری ہے۔" }
            require(cartItems.isNotEmpty() && cartItems.size <= 50) { "کارٹ میں کم از کم 1 اور زیادہ سے زیادہ 50 آئٹمز ہو سکتے ہیں۔" }

            val lineMaps = cartItems.map { item ->
                OrderLineItem(
                    productId = item.product.id,
                    name = item.product.name,
                    packing = item.product.packingDetails,
                    qty = item.quantity.toLong(),
                    unitPrice = item.product.wholesalePrice
                ).toMap()
            }
            val totalQty = cartItems.sumOf { it.quantity.toLong() }.coerceIn(1L, 10000L)
            val totalAmount = cartItems.sumOf { it.lineTotal }

            val docRef = db.collection("orders").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "customerName" to cleanName,
                "customerPhone" to cleanPhone,
                "customerAddress" to cleanAddress,
                "items" to lineMaps,
                "itemCount" to totalQty,
                "totalAmount" to totalAmount,
                "notes" to cleanNotes.ifBlank { null },
                "status" to OrderStatus.PENDING.wireValue,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            // Also keep profile details synced for faster future orders
            saveUserProfile(cleanName, cleanPhone, cleanAddress)
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun cancelPendingOrder(orderId: String): Result<Unit> {
        val docPath = "orders/$orderId"
        return try {
            requireUserId()
            require(orderId.isNotBlank()) { "آرڈر آئی ڈی غلط ہے۔" }
            db.collection("orders").document(orderId).update(
                mapOf(
                    "status" to OrderStatus.CANCELLED.wireValue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // ========================
    // OWNER / ADMIN OPERATIONS
    // ========================

    suspend fun seedStarterCatalogIfEmpty(): Result<Boolean> {
        val docPath = "products"
        return try {
            val uid = requireUserId()
            val existingCats = db.collection("categories")
                .whereEqualTo("shopId", SHOP_ID_CONSTANT)
                .get()
                .await()
            val existingProds = db.collection("products")
                .whereEqualTo("shopId", SHOP_ID_CONSTANT)
                .get()
                .await()
            val existingShop = db.collection("shop_info").document(SHOP_INFO_DOC_ID).get().await()

            if (!existingCats.isEmpty && !existingProds.isEmpty && existingShop.exists()) {
                return Result.success(false)
            }

            val batch = db.batch()
            if (!existingShop.exists()) {
                val info = StarterCatalogData.defaultShopInfo
                batch.set(
                    db.collection("shop_info").document(SHOP_INFO_DOC_ID),
                    mapOf(
                        "shopId" to SHOP_ID_CONSTANT,
                        "businessName" to info.businessName,
                        "address" to info.address,
                        "contact1Name" to info.contact1Name,
                        "contact1Phone" to info.contact1Phone,
                        "contact2Name" to info.contact2Name,
                        "contact2Phone" to info.contact2Phone,
                        "whatsappNumber" to info.whatsappNumber,
                        "announcement" to info.announcement,
                        "lastRateUpdateLabel" to info.lastRateUpdateLabel,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (existingCats.isEmpty) {
                StarterCatalogData.starterCategories.forEach { (catId, cat) ->
                    batch.set(
                        db.collection("categories").document(catId),
                        mapOf(
                            "shopId" to SHOP_ID_CONSTANT,
                            "name" to cat.name,
                            "sortOrder" to cat.sortOrder,
                            "iconKey" to cat.iconKey,
                            "active" to cat.active,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            if (existingProds.isEmpty) {
                StarterCatalogData.starterProducts(uid).forEach { (prodId, prod) ->
                    batch.set(
                        db.collection("products").document(prodId),
                        mapOf(
                            "shopId" to SHOP_ID_CONSTANT,
                            "name" to prod.name,
                            "category" to prod.category,
                            "packingDetails" to prod.packingDetails,
                            "wholesalePrice" to prod.wholesalePrice,
                            "previousWholesalePrice" to prod.previousWholesalePrice,
                            "retailPrice" to prod.retailPrice,
                            "available" to prod.available,
                            "specialOffer" to prod.specialOffer,
                            "trending" to prod.trending,
                            "stockCount" to prod.stockCount,
                            "offerText" to prod.offerText.ifBlank { null },
                            "imageData" to prod.imageData.ifBlank { null },
                            "updatedByUid" to uid,
                            "priceUpdatedAt" to FieldValue.serverTimestamp(),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ).filterValues { it != null } as Map<String, Any>
                    )

                    val delta = prod.wholesalePrice - prod.previousWholesalePrice
                    if (delta != 0.0) {
                        val updRef = db.collection("price_updates").document()
                        batch.set(
                            updRef,
                            mapOf(
                                "shopId" to SHOP_ID_CONSTANT,
                                "productId" to prodId,
                                "productName" to prod.name,
                                "category" to prod.category,
                                "packingDetails" to prod.packingDetails,
                                "oldPrice" to prod.previousWholesalePrice,
                                "newPrice" to prod.wholesalePrice,
                                "changeAmount" to delta,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }

            batch.commit().await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveShopInfo(info: ShopInfo): Result<Unit> {
        val docPath = "shop_info/$SHOP_INFO_DOC_ID"
        return try {
            requireUserId()
            val name = info.businessName.trim().take(120)
            val addr = info.address.trim().take(300)
            val c1Name = info.contact1Name.trim().take(80)
            val c1Phone = info.contact1Phone.trim().take(30)
            val c2Name = info.contact2Name.trim().take(80)
            val c2Phone = info.contact2Phone.trim().take(30)
            val wa = info.whatsappNumber.trim().take(30)

            require(name.isNotEmpty() && addr.isNotEmpty()) { "دکان کا نام اور پتہ خالی نہیں ہو سکتا۔" }
            require(c1Name.isNotEmpty() && c1Phone.isNotEmpty() && c2Name.isNotEmpty() && c2Phone.isNotEmpty() && wa.isNotEmpty()) {
                "تمام فون نمبرز اور واٹس ایپ نمبر درج کرنا ضروری ہے۔"
            }

            val docRef = db.collection("shop_info").document(SHOP_INFO_DOC_ID)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "businessName" to name,
                        "address" to addr,
                        "contact1Name" to c1Name,
                        "contact1Phone" to c1Phone,
                        "contact2Name" to c2Name,
                        "contact2Phone" to c2Phone,
                        "whatsappNumber" to wa,
                        "announcement" to info.announcement.trim().take(500),
                        "lastRateUpdateLabel" to info.lastRateUpdateLabel.trim().take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "shopId" to SHOP_ID_CONSTANT,
                        "businessName" to name,
                        "address" to addr,
                        "contact1Name" to c1Name,
                        "contact1Phone" to c1Phone,
                        "contact2Name" to c2Name,
                        "contact2Phone" to c2Phone,
                        "whatsappNumber" to wa,
                        "announcement" to info.announcement.trim().take(500),
                        "lastRateUpdateLabel" to info.lastRateUpdateLabel.trim().take(120),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveCategory(
        categoryId: String,
        name: String,
        sortOrder: Long,
        iconKey: String,
        active: Boolean
    ): Result<Unit> {
        var docPath = "categories"
        return try {
            requireUserId()
            val cleanName = name.trim().take(60)
            require(cleanName.isNotEmpty()) { "کیٹیگری کا نام درج کریں۔" }
            val safeSort = sortOrder.coerceIn(0L, 1000L)
            val safeIcon = iconKey.trim().ifBlank { "kiryana" }.take(40)

            if (categoryId.isBlank()) {
                val docRef = db.collection("categories").document()
                docPath = docRef.path
                docRef.set(
                    mapOf(
                        "shopId" to SHOP_ID_CONSTANT,
                        "name" to cleanName,
                        "sortOrder" to safeSort,
                        "iconKey" to safeIcon,
                        "active" to active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val docRef = db.collection("categories").document(categoryId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "sortOrder" to safeSort,
                        "iconKey" to safeIcon,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        val docPath = "categories/$categoryId"
        return try {
            requireUserId()
            require(categoryId.isNotBlank()) { "کیٹیگری آئی ڈی غلط ہے۔" }
            db.collection("categories").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun uploadProductImage(
        context: Context,
        imageUri: Uri
    ): Result<Pair<String, Boolean>> {
        return try {
            val uid = requireUserId()
            val bytes = uriToCompressedJpegBytes(context, imageUri)
                ?: return Result.failure(IllegalArgumentException("تصویر پروسیس کرنے میں مسئلہ پیش آیا۔"))

            try {
                val safeFileName = "products/${uid}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
                val ref = Firebase.storage.reference.child(safeFileName)
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build()
                ref.putBytes(bytes, metadata).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                Result.success(downloadUrl to true)
            } catch (e: CancellationException) {
                throw e
            } catch (storageEx: Exception) {
                Log.w("StoreRepository", "Firebase Storage upload unavailable, falling back to inline JPEG URI: ${storageEx.message}")
                val fallback = uriToCompressedBase64DataUri(context, imageUri)
                if (fallback != null && fallback.length <= 450_000) {
                    Result.success(fallback to false)
                } else {
                    Result.failure(storageEx)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProduct(
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
        imageData: String
    ): Result<Unit> {
        var docPath = "products"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(120)
            val cleanCat = category.trim().take(60)
            val cleanPacking = packingDetails.trim().take(120)
            val cleanOffer = offerText.trim().take(160)
            val cleanImage = imageData.trim()
            val safeStock = stockCount.coerceIn(0L, 1_000_000L)

            require(cleanName.isNotEmpty()) { "پروڈکٹ کا نام درج کریں۔" }
            require(cleanCat.isNotEmpty()) { "کیٹیگری منتخب کریں۔" }
            require(cleanPacking.isNotEmpty()) { "وزن اور پیکنگ کی تفصیل درج کریں۔" }
            require(wholesalePrice in 0.0..10_000_000.0) { "ہول سیل قیمت درست نہیں ہے۔" }
            require(retailPrice in 0.0..10_000_000.0) { "پرچون قیمت درست نہیں ہے۔" }
            require(cleanImage.length <= 450_000) { "تصویر کا سائز بہت بڑا ہے، براہِ کرم چھوٹی تصویر منتخب کریں۔" }

            val batch = db.batch()
            if (existingProduct == null || existingProduct.id.isBlank()) {
                val docRef = db.collection("products").document()
                docPath = docRef.path
                val payload = mapOf(
                    "shopId" to SHOP_ID_CONSTANT,
                    "name" to cleanName,
                    "category" to cleanCat,
                    "packingDetails" to cleanPacking,
                    "wholesalePrice" to wholesalePrice,
                    "previousWholesalePrice" to wholesalePrice,
                    "retailPrice" to retailPrice,
                    "available" to available,
                    "specialOffer" to specialOffer,
                    "trending" to trending,
                    "stockCount" to safeStock,
                    "offerText" to cleanOffer.ifBlank { null },
                    "imageData" to cleanImage.ifBlank { null },
                    "updatedByUid" to uid,
                    "priceUpdatedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                batch.set(docRef, payload)
            } else {
                val docRef = db.collection("products").document(existingProduct.id)
                docPath = docRef.path
                val priceChanged = existingProduct.wholesalePrice != wholesalePrice
                val updates = mutableMapOf<String, Any>(
                    "name" to cleanName,
                    "category" to cleanCat,
                    "packingDetails" to cleanPacking,
                    "wholesalePrice" to wholesalePrice,
                    "previousWholesalePrice" to if (priceChanged) existingProduct.wholesalePrice else existingProduct.previousWholesalePrice,
                    "retailPrice" to retailPrice,
                    "available" to available,
                    "specialOffer" to specialOffer,
                    "trending" to trending,
                    "stockCount" to safeStock,
                    "offerText" to cleanOffer,
                    "imageData" to cleanImage,
                    "updatedByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (priceChanged) {
                    updates["priceUpdatedAt"] = FieldValue.serverTimestamp()
                    val logRef = db.collection("price_updates").document()
                    batch.set(
                        logRef,
                        mapOf(
                            "shopId" to SHOP_ID_CONSTANT,
                            "productId" to existingProduct.id,
                            "productName" to cleanName,
                            "category" to cleanCat,
                            "packingDetails" to cleanPacking,
                            "oldPrice" to existingProduct.wholesalePrice,
                            "newPrice" to wholesalePrice,
                            "changeAmount" to (wholesalePrice - existingProduct.wholesalePrice),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.update(docRef, updates)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: IllegalStateException) {
            Result.failure(e)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun applyDailyRateUpdate(
        product: Product,
        newWholesalePrice: Double,
        newRetailPrice: Double
    ): Result<Unit> {
        val docPath = "products/${product.id}"
        return try {
            val uid = requireUserId()
            require(product.id.isNotBlank()) { "پروڈکٹ آئی ڈی موجود نہیں ہے۔" }
            require(newWholesalePrice in 0.0..10_000_000.0) { "ہول سیل قیمت درست نہیں ہے۔" }
            require(newRetailPrice in 0.0..10_000_000.0) { "پرچون قیمت درست نہیں ہے۔" }

            val batch = db.batch()
            val prodRef = db.collection("products").document(product.id)
            val priceChanged = product.wholesalePrice != newWholesalePrice
            batch.update(
                prodRef,
                mapOf(
                    "wholesalePrice" to newWholesalePrice,
                    "previousWholesalePrice" to if (priceChanged) product.wholesalePrice else product.previousWholesalePrice,
                    "retailPrice" to newRetailPrice,
                    "updatedByUid" to uid,
                    "priceUpdatedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (priceChanged) {
                val logRef = db.collection("price_updates").document()
                batch.set(
                    logRef,
                    mapOf(
                        "shopId" to SHOP_ID_CONSTANT,
                        "productId" to product.id,
                        "productName" to product.name,
                        "category" to product.category,
                        "packingDetails" to product.packingDetails,
                        "oldPrice" to product.wholesalePrice,
                        "newPrice" to newWholesalePrice,
                        "changeAmount" to (newWholesalePrice - product.wholesalePrice),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleProductAvailability(product: Product, available: Boolean): Result<Unit> {
        val docPath = "products/${product.id}"
        return try {
            val uid = requireUserId()
            db.collection("products").document(product.id).update(
                mapOf(
                    "available" to available,
                    "updatedByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleProductTrending(product: Product, trending: Boolean): Result<Unit> {
        val docPath = "products/${product.id}"
        return try {
            val uid = requireUserId()
            db.collection("products").document(product.id).update(
                mapOf(
                    "trending" to trending,
                    "updatedByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleProductOffer(product: Product, specialOffer: Boolean): Result<Unit> {
        val docPath = "products/${product.id}"
        return try {
            val uid = requireUserId()
            db.collection("products").document(product.id).update(
                mapOf(
                    "specialOffer" to specialOffer,
                    "updatedByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProductStock(product: Product, available: Boolean, stockCount: Long): Result<Unit> {
        val docPath = "products/${product.id}"
        return try {
            val uid = requireUserId()
            val safeStock = stockCount.coerceIn(0L, 1_000_000L)
            db.collection("products").document(product.id).update(
                mapOf(
                    "available" to available,
                    "stockCount" to safeStock,
                    "updatedByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val docPath = "products/$productId"
        return try {
            requireUserId()
            require(productId.isNotBlank()) { "پروڈکٹ آئی ڈی غلط ہے۔" }
            db.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus, notes: String? = null): Result<Unit> {
        val docPath = "orders/$orderId"
        return try {
            requireUserId()
            require(orderId.isNotBlank()) { "آرڈر آئی ڈی غلط ہے۔" }
            val updates = mutableMapOf<String, Any>(
                "status" to status.wireValue,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (notes != null) {
                updates["notes"] = notes.trim().take(500)
            }
            db.collection("orders").document(orderId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createCloudBackup(
        title: String,
        products: List<Product>,
        categories: List<Category>
    ): Result<Unit> {
        var docPath = "backups"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().ifBlank { "اسٹور بیک اپ" }.take(120)
            val boundedProducts = products.take(200).map { p ->
                mapOf(
                    "docId" to p.id,
                    "name" to p.name,
                    "category" to p.category,
                    "packingDetails" to p.packingDetails,
                    "wholesalePrice" to p.wholesalePrice,
                    "previousWholesalePrice" to p.previousWholesalePrice,
                    "retailPrice" to p.retailPrice,
                    "available" to p.available,
                    "specialOffer" to p.specialOffer,
                    "trending" to p.trending,
                    "stockCount" to p.stockCount,
                    "offerText" to p.offerText
                )
            }
            val boundedCategories = categories.take(100).map { c ->
                mapOf(
                    "docId" to c.id,
                    "name" to c.name,
                    "sortOrder" to c.sortOrder,
                    "iconKey" to c.iconKey,
                    "active" to c.active
                )
            }
            val docRef = db.collection("backups").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "title" to cleanTitle,
                    "productCount" to boundedProducts.size.toLong(),
                    "categoryCount" to boundedCategories.size.toLong(),
                    "productsBackup" to boundedProducts,
                    "categoriesBackup" to boundedCategories,
                    "createdByUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloudBackup(backup: BackupSnapshot): Result<Unit> {
        val docPath = "backups/${backup.id}"
        return try {
            val uid = requireUserId()
            val existingCatIds = db.collection("categories")
                .whereEqualTo("shopId", SHOP_ID_CONSTANT)
                .get().await().documents.map { it.id }.toSet()
            val existingProdIds = db.collection("products")
                .whereEqualTo("shopId", SHOP_ID_CONSTANT)
                .get().await().documents.map { it.id }.toSet()

            val batch = db.batch()
            backup.categoriesBackup.forEach { catMap ->
                val rawId = (catMap["docId"] as? String)?.takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                val catRef = if (rawId != null) db.collection("categories").document(rawId)
                else db.collection("categories").document()
                val baseFields = mapOf(
                    "name" to (catMap["name"] as? String ?: "کریانہ آئٹمز").take(60),
                    "sortOrder" to ((catMap["sortOrder"] as? Number)?.toLong() ?: 1L).coerceIn(0L, 1000L),
                    "iconKey" to (catMap["iconKey"] as? String ?: "kiryana").take(40),
                    "active" to (catMap["active"] as? Boolean ?: true),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (rawId != null && existingCatIds.contains(rawId)) {
                    batch.update(catRef, baseFields)
                } else {
                    batch.set(
                        catRef,
                        baseFields + mapOf(
                            "shopId" to SHOP_ID_CONSTANT,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            backup.productsBackup.forEach { prodMap ->
                val rawId = (prodMap["docId"] as? String)?.takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                val prodRef = if (rawId != null) db.collection("products").document(rawId)
                else db.collection("products").document()
                val wholesale = (prodMap["wholesalePrice"] as? Number)?.toDouble() ?: 0.0
                val prevWholesale = (prodMap["previousWholesalePrice"] as? Number)?.toDouble() ?: wholesale
                val retail = (prodMap["retailPrice"] as? Number)?.toDouble() ?: wholesale
                val offer = (prodMap["offerText"] as? String)?.take(160) ?: ""
                val trendingFlag = prodMap["trending"] as? Boolean ?: false
                val stockVal = ((prodMap["stockCount"] as? Number)?.toLong() ?: 0L).coerceIn(0L, 1_000_000L)
                val isUpdate = rawId != null && existingProdIds.contains(rawId)
                if (isUpdate) {
                    batch.update(
                        prodRef,
                        mapOf(
                            "name" to (prodMap["name"] as? String ?: "آئٹم").take(120),
                            "category" to (prodMap["category"] as? String ?: "کریانہ آئٹمز").take(60),
                            "packingDetails" to (prodMap["packingDetails"] as? String ?: "پیکنگ").take(120),
                            "wholesalePrice" to wholesale,
                            "previousWholesalePrice" to prevWholesale,
                            "retailPrice" to retail,
                            "available" to (prodMap["available"] as? Boolean ?: true),
                            "specialOffer" to (prodMap["specialOffer"] as? Boolean ?: false),
                            "trending" to trendingFlag,
                            "stockCount" to stockVal,
                            "offerText" to offer,
                            "updatedByUid" to uid,
                            "priceUpdatedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    batch.set(
                        prodRef,
                        mapOf(
                            "shopId" to SHOP_ID_CONSTANT,
                            "name" to (prodMap["name"] as? String ?: "آئٹم").take(120),
                            "category" to (prodMap["category"] as? String ?: "کریانہ آئٹمز").take(60),
                            "packingDetails" to (prodMap["packingDetails"] as? String ?: "پیکنگ").take(120),
                            "wholesalePrice" to wholesale,
                            "previousWholesalePrice" to prevWholesale,
                            "retailPrice" to retail,
                            "available" to (prodMap["available"] as? Boolean ?: true),
                            "specialOffer" to (prodMap["specialOffer"] as? Boolean ?: false),
                            "trending" to trendingFlag,
                            "stockCount" to stockVal,
                            "offerText" to offer.ifBlank { null },
                            "updatedByUid" to uid,
                            "priceUpdatedAt" to FieldValue.serverTimestamp(),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ).filterValues { it != null } as Map<String, Any>
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun exportCatalogToJson(
        shopInfo: ShopInfo,
        categories: List<Category>,
        products: List<Product>
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("shopInfo", JSONObject().apply {
            put("businessName", shopInfo.businessName)
            put("address", shopInfo.address)
            put("contact1Name", shopInfo.contact1Name)
            put("contact1Phone", shopInfo.contact1Phone)
            put("contact2Name", shopInfo.contact2Name)
            put("contact2Phone", shopInfo.contact2Phone)
            put("whatsappNumber", shopInfo.whatsappNumber)
            put("announcement", shopInfo.announcement)
        })
        root.put("categories", JSONArray().apply {
            categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("sortOrder", c.sortOrder)
                    put("iconKey", c.iconKey)
                    put("active", c.active)
                })
            }
        })
        root.put("products", JSONArray().apply {
            products.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("category", p.category)
                    put("packingDetails", p.packingDetails)
                    put("wholesalePrice", p.wholesalePrice)
                    put("previousWholesalePrice", p.previousWholesalePrice)
                    put("retailPrice", p.retailPrice)
                    put("available", p.available)
                    put("specialOffer", p.specialOffer)
                    put("offerText", p.offerText)
                })
            }
        })
        return root.toString(2)
    }

    suspend fun importCatalogFromJson(jsonString: String): Result<Int> {
        return try {
            val uid = requireUserId()
            val root = JSONObject(jsonString)
            val catsArr = root.optJSONArray("categories") ?: JSONArray()
            val prodsArr = root.optJSONArray("products") ?: JSONArray()

            val existingCatIds = db.collection("categories")
                .whereEqualTo("shopId", SHOP_ID_CONSTANT)
                .get().await().documents.map { it.id }.toSet()
            val existingProdIds = db.collection("products")
                .whereEqualTo("shopId", SHOP_ID_CONSTANT)
                .get().await().documents.map { it.id }.toSet()

            val batch = db.batch()
            for (i in 0 until minOf(catsArr.length(), 100)) {
                val obj = catsArr.getJSONObject(i)
                val id = obj.optString("id").takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                val ref = if (id != null) db.collection("categories").document(id)
                else db.collection("categories").document()
                val baseFields = mapOf(
                    "name" to obj.optString("name", "کریانہ آئٹمز").take(60),
                    "sortOrder" to obj.optLong("sortOrder", i.toLong() + 1).coerceIn(0L, 1000L),
                    "iconKey" to obj.optString("iconKey", "kiryana").take(40),
                    "active" to obj.optBoolean("active", true),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (id != null && existingCatIds.contains(id)) {
                    batch.update(ref, baseFields)
                } else {
                    batch.set(
                        ref,
                        baseFields + mapOf(
                            "shopId" to SHOP_ID_CONSTANT,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            var count = 0
            for (i in 0 until minOf(prodsArr.length(), 200)) {
                val obj = prodsArr.getJSONObject(i)
                val id = obj.optString("id").takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                val ref = if (id != null) db.collection("products").document(id)
                else db.collection("products").document()
                val wholesale = obj.optDouble("wholesalePrice", 0.0).coerceIn(0.0, 10_000_000.0)
                val prev = obj.optDouble("previousWholesalePrice", wholesale).coerceIn(0.0, 10_000_000.0)
                val retail = obj.optDouble("retailPrice", wholesale).coerceIn(0.0, 10_000_000.0)
                val offer = obj.optString("offerText", "").take(160)
                if (id != null && existingProdIds.contains(id)) {
                    batch.update(
                        ref,
                        mapOf(
                            "name" to obj.optString("name", "آئٹم").take(120),
                            "category" to obj.optString("category", "کریانہ آئٹمز").take(60),
                            "packingDetails" to obj.optString("packingDetails", "پیکنگ").take(120),
                            "wholesalePrice" to wholesale,
                            "previousWholesalePrice" to prev,
                            "retailPrice" to retail,
                            "available" to obj.optBoolean("available", true),
                            "specialOffer" to obj.optBoolean("specialOffer", false),
                            "offerText" to offer,
                            "updatedByUid" to uid,
                            "priceUpdatedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    batch.set(
                        ref,
                        mapOf(
                            "shopId" to SHOP_ID_CONSTANT,
                            "name" to obj.optString("name", "آئٹم").take(120),
                            "category" to obj.optString("category", "کریانہ آئٹمز").take(60),
                            "packingDetails" to obj.optString("packingDetails", "پیکنگ").take(120),
                            "wholesalePrice" to wholesale,
                            "previousWholesalePrice" to prev,
                            "retailPrice" to retail,
                            "available" to obj.optBoolean("available", true),
                            "specialOffer" to obj.optBoolean("specialOffer", false),
                            "offerText" to offer.ifBlank { null },
                            "updatedByUid" to uid,
                            "priceUpdatedAt" to FieldValue.serverTimestamp(),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ).filterValues { it != null } as Map<String, Any>
                    )
                }
                count++
            }
            batch.commit().await()
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "import_json")
            Result.failure(e)
        }
    }
}
