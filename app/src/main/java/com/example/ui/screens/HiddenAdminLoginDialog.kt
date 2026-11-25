package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.firebase.isAuthorizedAdminPhoneInput
import com.example.data.firebase.onGoogleSignInClicked
import com.example.data.firebase.startPhoneNumberVerification
import com.example.data.firebase.verifyPhoneOtpCode
import com.example.util.isValidOtpCode
import com.example.util.normalizePakistanPhone
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay

@Composable
fun HiddenAdminLoginDialog(
    onDismiss: () -> Unit,
    onAdminAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var phoneInput by remember { mutableStateOf("") }
    var normalizedAdminPhone by remember { mutableStateOf<String?>(null) }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var otpInput by remember { mutableStateOf("") }
    var isOtpStep by remember { mutableStateOf(false) }

    var isPhoneLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var requestToken by remember { mutableIntStateOf(0) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isPhoneLoading, requestToken) {
        if (isPhoneLoading) {
            val captured = requestToken
            delay(35_000L)
            if (isPhoneLoading && requestToken == captured) {
                requestToken += 1
                isPhoneLoading = false
                errorMessage = "تصدیق میں زیادہ وقت لگ رہا ہے۔ براہِ کرم انٹرنیٹ چیک کریں اور دوبارہ کوشش کریں۔"
            }
        }
    }

    LaunchedEffect(isGoogleLoading) {
        if (isGoogleLoading) {
            delay(35_000L)
            if (isGoogleLoading) {
                isGoogleLoading = false
                errorMessage = "گوگل لاگ ان کا وقت ختم ہو گیا۔ براہِ کرم دوبارہ کوشش کریں۔"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("hidden_admin_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Filled.AdminPanelSettings,
                contentDescription = "سپر ایڈمن",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "خفیہ ایڈمن لاگ ان (Admin Only)",
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
                Text(
                    text = "سپر ایڈمن پینل تک رسائی صرف مجاز ایڈمن نمبرز (03098897096 / 03237644924) یا مالک کے گوگل اکاؤنٹ کو حاصل ہے۔",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isOtpStep || verificationId == null) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { raw ->
                                phoneInput = raw.filter { it.isDigit() || it == '+' }.take(14)
                                errorMessage = null
                            },
                            label = { Text("Admin Mobile (+92)") },
                            placeholder = { Text("03098897096") },
                            leadingIcon = { Icon(Icons.Filled.PhoneAndroid, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_phone_input")
                        )
                    }

                    Button(
                        onClick = {
                            val normalized = normalizePakistanPhone(phoneInput)
                            if (normalized == null || !isAuthorizedAdminPhoneInput(phoneInput)) {
                                errorMessage = "یہ نمبر مجاز ایڈمن فہرست میں شامل نہیں ہے۔ صرف 03098897096 یا 03237644924 سے ایڈمن لاگ ان ممکن ہے۔"
                                return@Button
                            }
                            errorMessage = null
                            infoMessage = null
                            val token = requestToken + 1
                            requestToken = token
                            isPhoneLoading = true
                            normalizedAdminPhone = normalized

                            startPhoneNumberVerification(
                                context = context,
                                e164Phone = normalized,
                                resendToken = resendToken,
                                scope = scope,
                                onCodeSent = { verId, forceToken ->
                                    if (requestToken != token) return@startPhoneNumberVerification
                                    isPhoneLoading = false
                                    verificationId = verId
                                    resendToken = forceToken
                                    isOtpStep = true
                                    infoMessage = "ایڈمن تصدیقی OTP کوڈ $normalized پر بھیج دیا گیا ہے۔"
                                },
                                onAutoVerifiedSuccess = {
                                    if (requestToken != token) return@startPhoneNumberVerification
                                    isPhoneLoading = false
                                    onAdminAuthenticated()
                                },
                                onError = { msg ->
                                    if (requestToken != token) return@startPhoneNumberVerification
                                    isPhoneLoading = false
                                    errorMessage = msg
                                }
                            )
                        },
                        enabled = !isPhoneLoading && !isGoogleLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_send_otp_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isPhoneLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("OTP بھیجا جا رہا ہے...")
                        } else {
                            Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("ایڈمن OTP حاصل کریں", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = "ایڈمن نمبر: ${normalizedAdminPhone ?: ""}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { raw ->
                                otpInput = raw.filter { it.isDigit() }.take(6)
                                errorMessage = null
                            },
                            label = { Text("6-Digit Admin OTP") },
                            placeholder = { Text("123456") },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_otp_input")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                isOtpStep = false
                                otpInput = ""
                                verificationId = null
                                errorMessage = null
                            },
                            enabled = !isPhoneLoading,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text("نمبر بدلیں")
                        }
                        Button(
                            onClick = {
                                val verId = verificationId
                                if (verId == null || !isValidOtpCode(otpInput)) {
                                    errorMessage = "براہِ کرم 6 ہندسوں کا OTP درج کریں۔"
                                    return@Button
                                }
                                errorMessage = null
                                val token = requestToken + 1
                                requestToken = token
                                isPhoneLoading = true
                                verifyPhoneOtpCode(
                                    context = context,
                                    verificationId = verId,
                                    otpCode = otpInput,
                                    scope = scope,
                                    onSuccess = {
                                        if (requestToken != token) return@verifyPhoneOtpCode
                                        isPhoneLoading = false
                                        onAdminAuthenticated()
                                    },
                                    onError = { msg ->
                                        if (requestToken != token) return@verifyPhoneOtpCode
                                        isPhoneLoading = false
                                        errorMessage = msg
                                    }
                                )
                            },
                            enabled = !isPhoneLoading && isValidOtpCode(otpInput),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("admin_verify_otp_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isPhoneLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("ایڈمن تصدیق کریں", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                OutlinedButton(
                    onClick = {
                        errorMessage = null
                        infoMessage = null
                        isGoogleLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isGoogleLoading = false
                                onAdminAuthenticated()
                            },
                            onAuthError = { msg ->
                                isGoogleLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isGoogleLoading = false
                            }
                        )
                    },
                    enabled = !isGoogleLoading && !isPhoneLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_google_login_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGoogleLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("گوگل تصدیق جاری ہے...")
                    } else {
                        Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("مالک گوگل اکاؤنٹ لاگ ان", fontWeight = FontWeight.Bold)
                    }
                }

                if (infoMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = infoMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = {
                    requestToken += 1
                    isPhoneLoading = false
                    isGoogleLoading = false
                    onDismiss()
                },
                modifier = Modifier.testTag("admin_dialog_close_button")
            ) {
                Text("بند کریں")
            }
        }
    )
}
