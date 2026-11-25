package com.example.data.firebase

import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.SHOP_ID_CONSTANT
import com.example.data.model.ShopInfo

object StarterCatalogData {

    val defaultShopInfo = ShopInfo(
        id = "main",
        shopId = SHOP_ID_CONSTANT,
        businessName = "من سلوٰی ٹریڈر",
        address = "چک 225 احمد نگر، مین ٹینکی والا روڈ، فیصل آباد، نزد یا رسول اللہ مسجد، فیصل آباد۔",
        contact1Name = "محمد شہباز",
        contact1Phone = "03237644924",
        contact2Name = "شیراز احمد",
        contact2Phone = "03098897096",
        whatsappNumber = "03098897096",
        announcement = "آج کے تازہ تھوک ریٹس جاری کر دیے گئے ہیں۔ بلک آرڈرز کے لیے کارٹ سے واٹس ایپ پر آرڈر بھیجیں۔",
        lastRateUpdateLabel = "روزانہ تازہ تھوک ریٹ لسٹ"
    )

    val starterCategories: List<Pair<String, Category>> = listOf(
        "cat_atta" to Category(shopId = SHOP_ID_CONSTANT, name = "آٹا", sortOrder = 1, iconKey = "flour", active = true),
        "cat_chawal" to Category(shopId = SHOP_ID_CONSTANT, name = "چاول", sortOrder = 2, iconKey = "rice", active = true),
        "cat_ghee" to Category(shopId = SHOP_ID_CONSTANT, name = "گھی", sortOrder = 3, iconKey = "ghee", active = true),
        "cat_oil" to Category(shopId = SHOP_ID_CONSTANT, name = "آئل", sortOrder = 4, iconKey = "oil", active = true),
        "cat_drinks" to Category(shopId = SHOP_ID_CONSTANT, name = "کولڈ ڈرنکس", sortOrder = 5, iconKey = "drinks", active = true),
        "cat_biscuits" to Category(shopId = SHOP_ID_CONSTANT, name = "بسکٹ", sortOrder = 6, iconKey = "biscuit", active = true),
        "cat_toffees" to Category(shopId = SHOP_ID_CONSTANT, name = "ٹافیاں", sortOrder = 7, iconKey = "candy", active = true),
        "cat_sugar" to Category(shopId = SHOP_ID_CONSTANT, name = "چینی", sortOrder = 8, iconKey = "sugar", active = true),
        "cat_daal" to Category(shopId = SHOP_ID_CONSTANT, name = "دالیں", sortOrder = 9, iconKey = "pulses", active = true),
        "cat_kiryana" to Category(shopId = SHOP_ID_CONSTANT, name = "کریانہ آئٹمز", sortOrder = 10, iconKey = "kiryana", active = true)
    )

    fun starterProducts(ownerUid: String): List<Pair<String, Product>> = listOf(
        "prod_atta_fine_20" to Product(
            shopId = SHOP_ID_CONSTANT,
            name = "فائن آٹا نمبر 1",
            category = "آٹا",
            packingDetails = "20 کلو تھیلا",
            wholesalePrice = 2620.0,
            previousWholesalePrice = 2680.0,
            retailPrice = 2780.0,
            available = true,
            specialOffer = true,
            offerText = "10 تھیلوں پر 20 روپے فی تھیلا مزید رعایت",
            updatedByUid = ownerUid
        ),
        "prod_atta_chakki_10" to Product(
            shopId = SHOP_ID_CONSTANT,
            name = "چکی خالص گندم آٹا",
            category = "آٹا",
            packingDetails = "10 کلو تھیلا",
            wholesalePrice = 1360.0,
            previousWholesalePrice = 1360.0,
            retailPrice = 1450.0,
            available = true,
            specialOffer = false,
            updatedByUid = ownerUid
        ),
        "prod_chawal_basmati_25" to Product(
            shopId = SHOP_ID_CONSTANT,
            name = "سپر کرنل باسمتی پرانا",
            category = "چاول",
            packingDetails = "25 کلو بوری",
            wholesalePrice = 7350.0,
            previousWholesalePrice = 7200.0,
            retailPrice = 7800.0,
            available = true,
            specialOffer = false,
            updatedByUid = ownerUid
        ),
        "prod_chawal_sella_25" to Product(
            shopId = SHOP_ID_CONSTANT,
            name = "کائنات 1121 سیلا چاول",
            category = "چاول",
            packingDetails = "25 کلو بوری",
            wholesalePrice = 7950.0,
            previousWholesalePrice = 8100.0,
            retailPrice = 8400.0,
            available = true,
            specialOffer = true,
            offerText = "آج کی خصوصی تھوک قیمت",
            updatedByUid = ownerUid
        ),
        "prod_ghee_banaspati_16" to Product(
            shopId = SHOP_ID_CONSTANT,
            name = "بناسپتی گھی بالٹی",
            category = "گھی",
            packingDetails = "16 کلو ٹین / بالٹی",
            wholesalePrice = 7680.0,
            previousWholesalePrice = 7600.0,
            retailPrice = 8050.0,
            available = true,
            specialOffer = false,
            updatedByUid = ownerUid
        ),
        "prod_ghee_pouch_12" to Product(
            shopId = SHOP_ID_CONSTANT,
            name = "پریمیم گھی پاؤچ کارٹن",
            category = "گھی",
            packingDetails = "1 کلو × 12 پیکٹ کارٹن",
            wholesalePrice = 5880.0,
            previousWholesalePrice = 5940.0,
            retailPrice = 6200.0,
            available = true,
            specialOffer = false,
            updatedByUid = ownerUid
        ),
        "prod_oil_cooking_5l" to Product(
            shopId = SHOP_ID_CONSTANT,
            name = "خالص کوکنگ آئل بوتل",
            category = "آئل",
            packingDetails = "5 لیٹر × 4 بوتل کارٹن",
            wholesalePrice = 10200.0,
            previousWholesalePrice = 10350.0,
            retailPrice = 10800.0,
            available = true,
            specialOffer = true,
            offerText = "ہول سیل کارٹن اسپیشل ڈسکاؤنٹ",
            updatedByUid = ownerUid
        ),
        "prod_drinks_15l" to Product(
            shopId = SHOP_ID_CONSTANT,
            name = "کولڈ ڈرنک جمبو پیک",
            category = "کولڈ ڈرنکس",
            packingDetails = "1.5 لیٹر × 6 بوتل کیس",
            wholesalePrice = 1020.0,
            previousWholesalePrice = 1000.0,
            retailPrice = 1200.0,
            available = true,
            specialOffer = false,
            updatedByUid = ownerUid
        ),
        "prod_biscuits_family" to Product(
            shopId = SHOP_ID_CONSTANT,
            name = "فیملی بسکٹ ہاف رول کارٹن",
            category = "بسکٹ",
            packingDetails = "24 پیکٹ فل کارٹن",
            wholesalePrice = 1440.0,
            previousWholesalePrice = 1440.0,
            retailPrice = 1680.0,
            available = true,
            specialOffer = false,
            updatedByUid = ownerUid
        ),
        "prod_toffees_jar" to Product(
            shopId = SHOP_ID_CONSTANT,
            name = "مکس فروٹ و ملک ٹافی جار",
            category = "ٹافیاں",
            packingDetails = "200 عدد بڑا جار",
            wholesalePrice = 680.0,
            previousWholesalePrice = 700.0,
            retailPrice = 800.0,
            available = true,
            specialOffer = false,
            updatedByUid = ownerUid
        ),
        "prod_cheeni_50kg" to Product(
            shopId = SHOP_ID_CONSTANT,
            name = "سفید دانے دار چینی",
            category = "چینی",
            packingDetails = "50 کلو بوری",
            wholesalePrice = 6750.0,
            previousWholesalePrice = 6850.0,
            retailPrice = 7100.0,
            available = true,
            specialOffer = true,
            offerText = "نقد خریداری پر 100 روپے کم ریٹ",
            updatedByUid = ownerUid
        ),
        "prod_daal_chana_25" to Product(
            shopId = SHOP_ID_CONSTANT,
            name = "دیسی دال چنا اسپیشل",
            category = "دالیں",
            packingDetails = "25 کلو تھیلا",
            wholesalePrice = 6950.0,
            previousWholesalePrice = 6850.0,
            retailPrice = 7350.0,
            available = true,
            specialOffer = false,
            updatedByUid = ownerUid
        ),
        "prod_kiryana_tea_900" to Product(
            shopId = SHOP_ID_CONSTANT,
            name = "کڑک دانے دار چائے پتی",
            category = "کریانہ آئٹمز",
            packingDetails = "900 گرام × 10 پیکٹ",
            wholesalePrice = 14800.0,
            previousWholesalePrice = 15000.0,
            retailPrice = 15800.0,
            available = true,
            specialOffer = false,
            updatedByUid = ownerUid
        )
    )
}
