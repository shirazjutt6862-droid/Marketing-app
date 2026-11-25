package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.data.model.CartItem
import com.example.data.model.ShopInfo
import com.google.firebase.Timestamp
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

private val priceFormatter = DecimalFormat("#,##0")
private val dateTimeFormatter = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale("ur", "PK"))
private val shortDateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("ur", "PK"))

fun formatPkr(amount: Double): String {
    return "Rs. ${priceFormatter.format(amount.toLong())}"
}

fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "ابھی اپ ڈیٹ ہوا"
    return dateTimeFormatter.format(timestamp.toDate())
}

fun formatShortDate(timestamp: Timestamp?): String {
    if (timestamp == null) return "آج"
    return shortDateFormatter.format(timestamp.toDate())
}

fun normalizePakistanWhatsApp(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return when {
        digits.startsWith("92") -> digits
        digits.startsWith("0") && digits.length >= 10 -> "92${digits.drop(1)}"
        else -> digits
    }
}

fun buildWhatsAppOrderMessage(
    shopInfo: ShopInfo,
    customerName: String,
    customerPhone: String,
    customerAddress: String,
    cartItems: List<CartItem>,
    totalAmount: Double,
    notes: String
): String {
    val sb = StringBuilder()
    sb.appendLine("السلام علیکم *${shopInfo.businessName}*!")
    sb.appendLine("نیا ہول سیل آرڈر:")
    sb.appendLine("————————————")
    cartItems.forEachIndexed { index, item ->
        sb.appendLine("${index + 1}. ${item.product.name} (${item.product.packingDetails})")
        sb.appendLine("   تعداد: ${item.quantity} × ${formatPkr(item.product.wholesalePrice)} = ${formatPkr(item.lineTotal)}")
    }
    sb.appendLine("————————————")
    sb.appendLine("*کل بل: ${formatPkr(totalAmount)}*")
    sb.appendLine("کسٹمر / دکان: $customerName")
    sb.appendLine("فون: $customerPhone")
    sb.appendLine("پتہ: $customerAddress")
    if (notes.isNotBlank()) {
        sb.appendLine("ہدایت: $notes")
    }
    return sb.toString()
}

fun openWhatsAppChat(context: Context, phoneNumber: String, message: String) {
    val intl = normalizePakistanWhatsApp(phoneNumber)
    val encoded = Uri.encode(message)
    val url = "https://wa.me/$intl?text=$encoded"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

fun openDialer(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

fun uriToCompressedJpegBytes(context: Context, uri: Uri): ByteArray? {
    return runCatching {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        val maxDim = 640
        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > maxDim * 2 ||
            boundsOptions.outHeight / sampleSize > maxDim * 2
        ) {
            sampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val ratio = minOf(
            maxDim.toFloat() / decoded.width.coerceAtLeast(1),
            maxDim.toFloat() / decoded.height.coerceAtLeast(1),
            1f
        )
        val targetW = (decoded.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (decoded.height * ratio).toInt().coerceAtLeast(1)
        val scaled = if (targetW != decoded.width || targetH != decoded.height) {
            Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
        } else {
            decoded
        }
        val out = ByteArrayOutputStream()
        var quality = 78
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        while (out.size() > 140_000 && quality > 35) {
            out.reset()
            quality -= 10
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        out.toByteArray()
    }.getOrNull()
}

fun uriToCompressedBase64DataUri(context: Context, uri: Uri): String? {
    val bytes = uriToCompressedJpegBytes(context, uri) ?: return null
    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return "data:image/jpeg;base64,$b64"
}
