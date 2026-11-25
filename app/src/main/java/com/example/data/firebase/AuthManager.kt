package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.ADMIN_PHONE_NUMBERS
import com.example.data.model.OWNER_EMAILS
import com.example.util.findActivity
import com.example.util.normalizePakistanPhone
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

private const val AUTH_TAG = "Auth"

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun isVerifiedOwner(user: FirebaseUser?): Boolean {
    if (user == null) return false
    val email = user.email?.trim()?.lowercase()
    if (email != null && user.isEmailVerified && email in OWNER_EMAILS) {
        return true
    }
    val rawPhone = user.phoneNumber?.trim()
    if (!rawPhone.isNullOrBlank()) {
        val normalized = normalizePakistanPhone(rawPhone) ?: rawPhone
        if (normalized in ADMIN_PHONE_NUMBERS) {
            return true
        }
    }
    return false
}

fun isVerifiedAdmin(user: FirebaseUser?): Boolean = isVerifiedOwner(user)

fun isAuthorizedAdminPhoneInput(rawPhone: String): Boolean {
    val normalized = normalizePakistanPhone(rawPhone) ?: return false
    return normalized in ADMIN_PHONE_NUMBERS
}

fun startPhoneNumberVerification(
    context: Context,
    e164Phone: String,
    resendToken: PhoneAuthProvider.ForceResendingToken?,
    scope: CoroutineScope,
    onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
    onAutoVerifiedSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    if (FirebaseApp.getApps(context).isEmpty()) {
        onError("Firebase کنفیگریشن دستیاب نہیں ہے۔ براہِ کرم ایپ دوبارہ شروع کریں۔")
        return
    }
    val activity = context.findActivity()
    if (activity == null) {
        onError("اسکرین دستیاب نہیں ہے، براہِ کرم دوبارہ کوشش کریں۔")
        return
    }

    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            scope.launch {
                try {
                    Firebase.auth.signInWithCredential(credential).await()
                    onAutoVerifiedSuccess()
                } catch (e: Exception) {
                    Log.e(AUTH_TAG, "Auto phone sign-in failed", e)
                    onError(mapPhoneAuthError(e))
                }
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.e(AUTH_TAG, "Phone verification failed", e)
            onError(mapPhoneAuthError(e))
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            onCodeSent(verificationId, token)
        }
    }

    try {
        val builder = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(e164Phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resendToken != null) {
            builder.setForceResendingToken(resendToken)
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    } catch (e: Exception) {
        Log.e(AUTH_TAG, "Phone verification invocation failed", e)
        onError(mapPhoneAuthError(e))
    }
}

fun verifyPhoneOtpCode(
    context: Context,
    verificationId: String,
    otpCode: String,
    scope: CoroutineScope,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    if (FirebaseApp.getApps(context).isEmpty()) {
        onError("Firebase کنفیگریشن دستیاب نہیں ہے۔ براہِ کرم ایپ دوبارہ شروع کریں۔")
        return
    }
    scope.launch {
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
            Firebase.auth.signInWithCredential(credential).await()
            onSuccess()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "OTP verification failed", e)
            onError(mapPhoneAuthError(e))
        }
    }
}

internal fun mapPhoneAuthError(e: Throwable): String {
    val msg = e.localizedMessage ?: e.message ?: ""
    return when {
        e is FirebaseAuthInvalidCredentialsException ->
            "درج کردہ موبائل نمبر یا 6 ہندسوں کا OTP کوڈ غلط ہے۔ براہِ کرم دوبارہ چیک کریں۔"
        e is FirebaseTooManyRequestsException ->
            "بہت زیادہ کوششیں کی گئیں۔ براہِ کرم کچھ دیر بعد دوبارہ کوشش کریں۔"
        e is FirebaseNetworkException ->
            "انٹرنیٹ کنکشن دستیاب نہیں ہے۔ براہِ کرم نیٹ ورک چیک کریں۔"
        e is FirebaseAuthMissingActivityForRecaptchaException ||
            msg.contains("siteKey", ignoreCase = true) ||
            msg.contains("reCAPTCHA", ignoreCase = true) ||
            msg.contains("CAPTCHA_CHECK_FAILED", ignoreCase = true) ||
            msg.contains("WEB_CONTEXT_CANCELED", ignoreCase = true) ->
            "reCAPTCHA تصدیق مکمل نہیں ہو سکی (Firebase/Google Cloud پر reCAPTCHA Enterprise یا Play Store دستیاب نہیں ہے)۔ حقیقی موبائل پر Google Play Store اپڈیٹ رکھیں یا Firebase Console میں Phone Auth اور Test Phone Numbers سیٹ کریں۔"
        msg.contains("Integrity API error", ignoreCase = true) ||
            msg.contains("CANNOT_BIND_TO_SERVICE", ignoreCase = true) ||
            msg.contains("Play Integrity", ignoreCase = true) ->
            "اس ڈیوائس/ایمولیٹر پر Google Play Store / Play Integrity سروس سے رابطہ نہیں ہو سکا (Error -9)۔ براہِ کرم Google Play Store اپڈیٹ کریں یا حقیقی Android موبائل استعمال کریں۔"
        msg.contains("MISSING_CLIENT_IDENTIFIER", ignoreCase = true) ||
            msg.contains("INVALID_APP_CREDENTIAL", ignoreCase = true) ||
            msg.contains("app-not-authorized", ignoreCase = true) ->
            "ایپ کی SHA-1 / SHA-256 تصدیق Firebase میں مکمل نہیں ہے۔ براہِ کرم Firebase Console میں ایپ کی SHA فنگرپرنٹس چیک کریں۔"
        msg.contains("BILLING_NOT_ENABLED", ignoreCase = true) ||
            msg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
            msg.contains("SMS_NOT_SUPPORTED", ignoreCase = true) ->
            "Firebase Console میں Phone Authentication (SMS OTP) فعال کرنا ضروری ہے۔ براہِ کرم Authentication > Sign-in method > Phone آن کریں۔"
        else -> msg.ifBlank { "فون تصدیق میں خرابی پیش آئی۔" }
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (FirebaseApp.getApps(context).isEmpty()) {
        onUnauthenticated()
        return
    }
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val targetContext = context.findActivity() ?: context
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val response = withTimeoutOrNull(4_000L) {
                credentialManager.getCredential(targetContext, request)
            }
            val cred = response?.credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    if (FirebaseApp.getApps(context).isEmpty()) {
        onAuthError("Firebase کنفیگریشن دستیاب نہیں ہے۔")
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("گوگل سائن ان کنفیگریشن موجود نہیں ہے (default_web_client_id)۔")
        return
    }
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("اسکرین کا سیاق دستیاب نہیں ہے، براہِ کرم دوبارہ کوشش کریں۔")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val response = withTimeoutOrNull(30_000L) {
                credentialManager.getCredential(activity, request)
            }
            if (response == null) {
                onAuthError("گوگل سائن ان کا وقت ختم ہو گیا۔ براہِ کرم دوبارہ کوشش کریں۔")
                return@launch
            }
            val cred = response.credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                val authResult = Firebase.auth
                    .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                    .await()
                if (!isVerifiedOwner(authResult.user)) {
                    Firebase.auth.signOut()
                    withTimeoutOrNull(1_500L) {
                        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
                    }
                    onAuthError("یہ گوگل اکاؤنٹ صرف ایڈمن / مالک کے لیے مخصوص ہے۔ براہِ کرم کسٹمر لاگ ان کے لیے موبائل نمبر اور OTP استعمال کریں۔")
                } else {
                    onAuthSuccess()
                }
            } else {
                onAuthError("غیر متوقع کریڈینشل قسم موصول ہوئی۔")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "گوگل سائن ان میں مسئلہ پیش آیا۔")
        }
    }
}

fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    Firebase.auth.signOut()
    onSignOutComplete()
    scope.launch {
        withContext(NonCancellable) {
            try {
                withTimeoutOrNull(2_000L) {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                }
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            }
        }
    }
}
