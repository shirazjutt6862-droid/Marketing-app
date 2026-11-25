package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.components.ProductImage

@Composable
fun ProductEditorDialog(
    existingProduct: Product?,
    categories: List<Category>,
    isBusy: Boolean,
    isUploadingImage: Boolean = false,
    onUploadImage: (Uri, onUploaded: (String) -> Unit) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    val defaultCat = existingProduct?.category?.takeIf { it.isNotBlank() }
        ?: categories.firstOrNull()?.name ?: "کریانہ آئٹمز"

    var name by remember(existingProduct) { mutableStateOf(existingProduct?.name ?: "") }
    var category by remember(existingProduct) { mutableStateOf(defaultCat) }
    var packingDetails by remember(existingProduct) { mutableStateOf(existingProduct?.packingDetails ?: "") }
    var wholesaleStr by remember(existingProduct) {
        mutableStateOf(existingProduct?.wholesalePrice?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var retailStr by remember(existingProduct) {
        mutableStateOf(existingProduct?.retailPrice?.takeIf { it > 0 }?.toLong()?.toString() ?: "")
    }
    var stockStr by remember(existingProduct) {
        mutableStateOf((existingProduct?.stockCount ?: 0L).takeIf { it > 0L }?.toString() ?: "")
    }
    var available by remember(existingProduct) { mutableStateOf(existingProduct?.available ?: true) }
    var specialOffer by remember(existingProduct) { mutableStateOf(existingProduct?.specialOffer ?: false) }
    var trending by remember(existingProduct) { mutableStateOf(existingProduct?.trending ?: false) }
    var offerText by remember(existingProduct) { mutableStateOf(existingProduct?.offerText ?: "") }
    var imageData by remember(existingProduct) { mutableStateOf(existingProduct?.imageData ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            validationError = null
            onUploadImage(uri) { uploadedUrlOrData ->
                imageData = uploadedUrlOrData
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingProduct == null) "نیا پروڈکٹ شامل کریں" else "پروڈکٹ میں ترمیم کریں",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProductImage(
                        imageData = imageData,
                        category = category,
                        contentDescription = name.ifBlank { "تصویر" },
                        modifier = Modifier.size(72.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            enabled = !isUploadingImage,
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isUploadingImage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("تصویر اپ لوڈ ہو رہی ہے...")
                            } else {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("گیلری سے تصویر چنیں")
                            }
                        }
                        if (imageData.isNotBlank()) {
                            TextButton(onClick = { imageData = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تصویر ہٹائیں")
                            }
                        }
                    }
                }

                if (!imageData.startsWith("data:image")) {
                    OutlinedTextField(
                        value = imageData,
                        onValueChange = { imageData = it },
                        label = { Text("یا تصویر کا لنک (HTTPS URL)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("پروڈکٹ کا نام *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "کیٹیگری منتخب کریں:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = category == cat.name,
                            onClick = { category = cat.name },
                            label = { Text(cat.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = packingDetails,
                    onValueChange = { packingDetails = it },
                    label = { Text("وزن اور پیکنگ (مثلاً 20 کلو تھیلا) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wholesaleStr,
                        onValueChange = { wholesaleStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("ہول سیل قیمت *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = retailStr,
                        onValueChange = { retailStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("پرچون قیمت *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it.filter { ch -> ch.isDigit() }.take(7) },
                    label = { Text("اسٹاک کی تعداد (Stock Quantity)") },
                    placeholder = { Text("50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_stock_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("اسٹاک میں دستیاب ہے")
                    Switch(checked = available, onCheckedChange = { available = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ٹرینڈنگ آئٹم (Trending)")
                    Switch(
                        checked = trending,
                        onCheckedChange = { trending = it },
                        modifier = Modifier.testTag("product_trending_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("خصوصی آفر لگائیں (Special Offer)")
                    Switch(checked = specialOffer, onCheckedChange = { specialOffer = it })
                }

                if (specialOffer) {
                    OutlinedTextField(
                        value = offerText,
                        onValueChange = { offerText = it },
                        label = { Text("آفر کی تفصیل (مثلاً 50 روپے رعایت)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy && !isUploadingImage,
                onClick = {
                    val ws = wholesaleStr.toDoubleOrNull()
                    val rt = retailStr.toDoubleOrNull()
                    val stockVal = stockStr.toLongOrNull() ?: 0L
                    when {
                        name.isBlank() -> validationError = "پروڈکٹ کا نام ضروری ہے۔"
                        packingDetails.isBlank() -> validationError = "پیکنگ کی تفصیل ضروری ہے۔"
                        ws == null || ws < 0.0 -> validationError = "درست ہول سیل قیمت درج کریں۔"
                        rt == null || rt < 0.0 -> validationError = "درست پرچون قیمت درج کریں۔"
                        else -> onSave(
                            name, category, packingDetails, ws, rt,
                            available, specialOffer, trending, stockVal,
                            if (specialOffer) offerText else "", imageData
                        )
                    }
                }
            ) {
                Text("محفوظ کریں")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("منسوخ کریں")
            }
        }
    )
}

@Composable
fun CategoryEditorDialog(
    existingCategory: Category?,
    nextOrder: Long,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, sortOrder: Long, iconKey: String, active: Boolean) -> Unit
) {
    var name by remember(existingCategory) { mutableStateOf(existingCategory?.name ?: "") }
    var sortStr by remember(existingCategory) {
        mutableStateOf((existingCategory?.sortOrder ?: nextOrder).toString())
    }
    var iconKey by remember(existingCategory) { mutableStateOf(existingCategory?.iconKey ?: "kiryana") }
    var active by remember(existingCategory) { mutableStateOf(existingCategory?.active ?: true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val iconOptions = listOf(
        "flour" to "آٹا",
        "rice" to "چاول",
        "ghee" to "گھی",
        "oil" to "آئل",
        "drinks" to "کولڈ ڈرنکس",
        "biscuit" to "بسکٹ",
        "candy" to "ٹافیاں",
        "sugar" to "چینی",
        "pulses" to "دالیں",
        "kiryana" to "کریانہ"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingCategory == null) "نئی کیٹیگری شامل کریں" else "کیٹیگری میں ترمیم",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("کیٹیگری کا نام *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sortStr,
                    onValueChange = { sortStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("ترتیب نمبر") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("آئیکن منتخب کریں:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(iconOptions, key = { it.first }) { (key, label) ->
                        FilterChip(
                            selected = iconKey == key,
                            onClick = { iconKey = key },
                            label = { Text(label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("کیٹیگری فعال ہے")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                onClick = {
                    val order = sortStr.toLongOrNull() ?: 1L
                    if (name.isBlank()) {
                        errorMsg = "کیٹیگری کا نام درج کریں۔"
                    } else {
                        onSave(name, order, iconKey, active)
                    }
                }
            ) {
                Text("محفوظ کریں")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("منسوخ کریں")
            }
        }
    )
}
