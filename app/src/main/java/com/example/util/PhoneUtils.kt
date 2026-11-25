package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Normalizes Pakistani mobile numbers into strict E.164 format (+923XXXXXXXXX).
 * Accepts inputs such as:
 * - "3001234567"
 * - "03001234567"
 * - "923001234567"
 * - "+92 300-1234567"
 * Returns null if the number is not a valid 10-digit Pakistani mobile number.
 */
fun normalizePakistanPhone(rawInput: String): String? {
    val digits = rawInput.filter { it.isDigit() }
    val subscriber = when {
        digits.startsWith("92") && digits.length == 12 -> digits.drop(2)
        digits.startsWith("0") && digits.length == 11 -> digits.drop(1)
        digits.length == 10 -> digits
        else -> return null
    }
    if (!subscriber.startsWith("3") || subscriber.length != 10) {
        return null
    }
    return "+92$subscriber"
}

fun phoneToRegistryDocId(e164Phone: String): String {
    val digits = e164Phone.filter { it.isDigit() }
    return "pk_$digits"
}

fun isValidOtpCode(otp: String): Boolean {
    return otp.length == 6 && otp.all { it.isDigit() }
}
