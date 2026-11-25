package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val SHOP_ID_CONSTANT = "mann_salwa"
const val SHOP_INFO_DOC_ID = "main"
const val OWNER_EMAIL = "shiraz6862@gmail.com"
val OWNER_EMAILS = setOf("shiraz6862@gmail.com", "shirazahmad6862@gmail.com")
val ADMIN_PHONE_NUMBERS = setOf("+923098897096", "+923237644924")

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class ShopInfo(
    @DocumentId val id: String = "",
    val shopId: String = SHOP_ID_CONSTANT,
    val businessName: String = "من سلوٰی ٹریڈر",
    val address: String = "چک 225 احمد نگر، مین ٹینکی والا روڈ، فیصل آباد، نزد یا رسول اللہ مسجد، فیصل آباد۔",
    val contact1Name: String = "محمد شہباز",
    val contact1Phone: String = "03237644924",
    val contact2Name: String = "شیراز احمد",
    val contact2Phone: String = "03098897096",
    val whatsappNumber: String = "03098897096",
    val announcement: String = "تازہ ترین تھوک ریٹس روزانہ اپ ڈیٹ کیے جاتے ہیں۔ آرڈر کے لیے کارٹ یا واٹس ایپ استعمال کریں۔",
    val lastRateUpdateLabel: String = "آج کے تازہ تھوک ریٹس",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val shopId: String = SHOP_ID_CONSTANT,
    val name: String = "",
    val sortOrder: Long = 0L,
    val iconKey: String = "kiryana",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val shopId: String = SHOP_ID_CONSTANT,
    val name: String = "",
    val category: String = "",
    val packingDetails: String = "",
    val wholesalePrice: Double = 0.0,
    val previousWholesalePrice: Double = 0.0,
    val retailPrice: Double = 0.0,
    val available: Boolean = true,
    val specialOffer: Boolean = false,
    val trending: Boolean = false,
    val stockCount: Long = 0L,
    val offerText: String = "",
    val imageData: String = "",
    val updatedByUid: String = "",
    @ServerTimestamp val priceUpdatedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val priceDelta: Double
        get() = wholesalePrice - previousWholesalePrice
}

@Keep
@IgnoreExtraProperties
data class PriceUpdate(
    @DocumentId val id: String = "",
    val shopId: String = SHOP_ID_CONSTANT,
    val productId: String = "",
    val productName: String = "",
    val category: String = "",
    val packingDetails: String = "",
    val oldPrice: Double = 0.0,
    val newPrice: Double = 0.0,
    val changeAmount: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OrderLineItem(
    val productId: String = "",
    val name: String = "",
    val packing: String = "",
    val qty: Long = 1L,
    val unitPrice: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "name" to name,
        "packing" to packing,
        "qty" to qty,
        "unitPrice" to unitPrice
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): OrderLineItem {
            return OrderLineItem(
                productId = map["productId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                packing = map["packing"] as? String ?: "",
                qty = (map["qty"] as? Number)?.toLong() ?: 1L,
                unitPrice = (map["unitPrice"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}

enum class OrderStatus(val wireValue: String, val urduLabel: String) {
    PENDING("pending", "زیرِ التواء"),
    CONFIRMED("confirmed", "تصدیق شدہ"),
    DISPATCHED("dispatched", "روانہ"),
    COMPLETED("completed", "مکمل"),
    CANCELLED("cancelled", "منسوخ");

    companion object {
        fun fromWire(value: String?): OrderStatus =
            entries.firstOrNull { it.wireValue == value } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val items: List<Map<String, Any?>> = emptyList(),
    val itemCount: Long = 0L,
    val totalAmount: Double = 0.0,
    val notes: String = "",
    val status: String = OrderStatus.PENDING.wireValue,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedItems(): List<OrderLineItem> = items.map { OrderLineItem.fromMap(it) }
    fun parsedStatus(): OrderStatus = OrderStatus.fromWire(status)
}

enum class AccountStatus(val wireValue: String, val urduLabel: String) {
    PENDING("pending", "منظوری زیرِ التواء"),
    APPROVED("approved", "منظور شدہ"),
    BLOCKED("blocked", "بلاک");

    companion object {
        fun fromWire(value: String?): AccountStatus =
            entries.firstOrNull { it.wireValue == value } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val phoneNumber: String = "",
    val displayName: String = "",
    val shopName: String = "",
    val city: String = "",
    val shopOrAddress: String = "",
    val accountStatus: String = AccountStatus.PENDING.wireValue,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedStatus(): AccountStatus = AccountStatus.fromWire(accountStatus)
}

@Keep
@IgnoreExtraProperties
data class BackupSnapshot(
    @DocumentId val id: String = "",
    val title: String = "",
    val productCount: Long = 0L,
    val categoryCount: Long = 0L,
    val productsBackup: List<Map<String, Any?>> = emptyList(),
    val categoriesBackup: List<Map<String, Any?>> = emptyList(),
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val lineTotal: Double
        get() = product.wholesalePrice * quantity
}
