package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.firebase.StarterCatalogData
import com.example.data.firebase.startPhoneNumberVerification
import com.example.data.firebase.verifyPhoneOtpCode
import com.example.ui.theme.WhatsAppGreen
import com.example.util.isValidOtpCode
import com.example.util.normalizePakistanPhone
import com.example.util.openDialer
import com.example.util.openWhatsAppChat
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay

private enum class AuthTabMode(val title: String) {
    LOGIN("لاگ ان (Login)"),
    SIGNUP("نیا اکاؤنٹ (Signup)")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var authMode by remember { mutableStateOf(AuthTabMode.LOGIN) }
    var phoneInput by remember { mutableStateOf("") }
    var normalizedPhone by remember { mutableStateOf<String?>(null) }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var isOtpStep by remember { mutableStateOf(false) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var otpInput by remember { mutableStateOf("") }
    var resendCountdown by remember { mutableIntStateOf(0) }

    var isPhoneLoading by remember { mutableStateOf(false) }
    var phoneRequestToken by remember { mutableIntStateOf(0) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHiddenAdminDialog by remember { mutableStateOf(false) }
    var hiddenAdminTapCount by remember { mutableIntStateOf(0) }

    val shop = StarterCatalogData.defaultShopInfo

    // Safety back navigation from OTP step back to mobile number entry
    BackHandler(enabled = isOtpStep) {
        phoneRequestToken += 1
        isPhoneLoading = false
        isOtpStep = false
        errorMessage = null
    }

    // Safety timeout guard so Play Services/reCAPTCHA delays never freeze loading UI
    LaunchedEffect(isPhoneLoading, phoneRequestToken) {
        if (isPhoneLoading) {
            val capturedToken = phoneRequestToken
            delay(35_000L)
            if (isPhoneLoading && phoneRequestToken == capturedToken) {
                phoneRequestToken += 1
                isPhoneLoading = false
                errorMessage = "تصدیق میں زیادہ وقت لگ رہا ہے۔ براہِ کرم انٹرنیٹ چیک کریں اور دوبارہ کوشش کریں۔"
            }
        }
    }

    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000L)
            resendCountdown -= 1
        }
    }

    fun triggerSendOtp(useResendToken: Boolean) {
        val e164 = normalizePakistanPhone(phoneInput)
        if (e164 == null) {
            errorMessage = "براہِ کرم درست پاکستانی موبائل نمبر درج کریں (مثلاً 3001234567 یا 03001234567)۔"
            return
        }
        errorMessage = null
        infoMessage = null
        val requestToken = phoneRequestToken + 1
        phoneRequestToken = requestToken
        isPhoneLoading = true
        normalizedPhone = e164

        startPhoneNumberVerification(
            context = context,
            e164Phone = e164,
            resendToken = if (useResendToken) resendToken else null,
            scope = scope,
            onCodeSent = { verifId, token ->
                if (phoneRequestToken != requestToken) return@startPhoneNumberVerification
                isPhoneLoading = false
                verificationId = verifId
                isOtpStep = true
                resendToken = token
                resendCountdown = 60
                infoMessage = "6 ہندسوں کا تصدیقی OTP کوڈ $e164 پر بذریعہ SMS بھیج دیا گیا ہے۔"
            },
            onAutoVerifiedSuccess = {
                if (phoneRequestToken != requestToken) return@startPhoneNumberVerification
                isPhoneLoading = false
                onAuthSuccess()
            },
            onError = { msg ->
                if (phoneRequestToken != requestToken) return@startPhoneNumberVerification
                isPhoneLoading = false
                errorMessage = msg
            }
        )
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_shop_hero),
                        contentDescription = shop.businessName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x44062E1F), Color(0xEB083E29))
                                )
                            )
                            .padding(18.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Storefront,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "ہول سیل کریانہ اسٹور • فیصل آباد",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = shop.businessName,
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White,
                                modifier = Modifier
                                    .testTag("auth_hidden_admin_trigger")
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            hiddenAdminTapCount += 1
                                            if (hiddenAdminTapCount >= 5) {
                                                hiddenAdminTapCount = 0
                                                showHiddenAdminDialog = true
                                            }
                                        },
                                        onLongClick = {
                                            hiddenAdminTapCount = 0
                                            showHiddenAdminDialog = true
                                        }
                                    )
                            )
                        }
                    }
                }

                // Mobile Phone OTP Login & Signup Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AuthTabMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = authMode == mode,
                                    onClick = {
                                        authMode = mode
                                        errorMessage = null
                                        infoMessage = null
                                    },
                                    label = {
                                        Text(
                                            text = mode.title,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("auth_mode_${mode.name.lowercase()}")
                                )
                            }
                        }

                        // Step Back / Forward Bar when OTP was already sent
                        if (verificationId != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        isOtpStep = false
                                        errorMessage = null
                                    },
                                    enabled = isOtpStep && !isPhoneLoading,
                                    modifier = Modifier.testTag("auth_step_back_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "پیچھے", modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("پیچھے (نمبر)")
                                }
                                TextButton(
                                    onClick = {
                                        isOtpStep = true
                                        errorMessage = null
                                    },
                                    enabled = !isOtpStep && !isPhoneLoading,
                                    modifier = Modifier.testTag("auth_step_forward_button")
                                ) {
                                    Text("آگے (OTP)")
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "آگے", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Text(
                            text = if (authMode == AuthTabMode.SIGNUP) {
                                "نیا کسٹمر / دکاندار اکاؤنٹ بنائیں"
                            } else {
                                "موبائل نمبر سے لاگ ان کریں"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (authMode == AuthTabMode.SIGNUP) {
                                "اپنا پاکستانی موبائل نمبر (+92) درج کریں۔ SMS OTP تصدیق کے بعد اپنا نام، دکان کا نام اور شہر درج کریں (فی موبائل نمبر صرف ایک اکاؤنٹ)۔"
                            } else {
                                "اپنے رجسٹرڈ موبائل نمبر پر 6 ہندسوں کا SMS OTP کوڈ حاصل کر کے فوری لاگ ان کریں۔"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!isOtpStep || verificationId == null) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(56.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "🇵🇰 +92",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = phoneInput,
                                        onValueChange = { raw ->
                                            val cleaned = raw.filter { it.isDigit() }.take(12)
                                            phoneInput = cleaned
                                            if (normalizePakistanPhone(cleaned) != normalizedPhone) {
                                                verificationId = null
                                            }
                                            errorMessage = null
                                        },
                                        label = { Text("3001234567") },
                                        placeholder = { Text("03001234567") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.PhoneAndroid, contentDescription = null)
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("phone_number_input")
                                    )
                                }
                            }

                            Button(
                                onClick = { triggerSendOtp(useResendToken = false) },
                                enabled = !isPhoneLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("send_otp_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isPhoneLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("OTP بھیجا جا رہا ہے...")
                                } else {
                                    Icon(Icons.Filled.Sms, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "SMS کے ذریعے OTP کوڈ بھیجیں",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (isPhoneLoading) {
                                TextButton(
                                    onClick = {
                                        phoneRequestToken += 1
                                        isPhoneLoading = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("منسوخ کریں")
                                }
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "کوڈ اس نمبر پر بھیجا گیا:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = normalizedPhone ?: "",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = {
                                            isOtpStep = false
                                            verificationId = null
                                            otpInput = ""
                                            errorMessage = null
                                            infoMessage = null
                                        },
                                        enabled = !isPhoneLoading
                                    ) {
                                        Text("نمبر تبدیل کریں")
                                    }
                                }
                            }

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                OutlinedTextField(
                                    value = otpInput,
                                    onValueChange = { raw ->
                                        otpInput = raw.filter { it.isDigit() }.take(6)
                                        errorMessage = null
                                    },
                                    label = { Text("6-Digit OTP Code") },
                                    placeholder = { Text("123456") },
                                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("otp_code_input")
                                )
                            }

                            Button(
                                onClick = {
                                    val verifId = verificationId
                                    if (verifId == null || !isValidOtpCode(otpInput)) {
                                        errorMessage = "براہِ کرم پورا 6 ہندسوں کا OTP کوڈ درج کریں۔"
                                        return@Button
                                    }
                                    errorMessage = null
                                    val requestToken = phoneRequestToken + 1
                                    phoneRequestToken = requestToken
                                    isPhoneLoading = true
                                    verifyPhoneOtpCode(
                                        context = context,
                                        verificationId = verifId,
                                        otpCode = otpInput,
                                        scope = scope,
                                        onSuccess = {
                                            if (phoneRequestToken != requestToken) return@verifyPhoneOtpCode
                                            isPhoneLoading = false
                                            onAuthSuccess()
                                        },
                                        onError = { msg ->
                                            if (phoneRequestToken != requestToken) return@verifyPhoneOtpCode
                                            isPhoneLoading = false
                                            errorMessage = msg
                                        }
                                    )
                                },
                                enabled = !isPhoneLoading && isValidOtpCode(otpInput),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("verify_otp_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isPhoneLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("تصدیق جاری ہے...")
                                } else {
                                    Text(
                                        text = "OTP تصدیق کریں اور آگے بڑھیں",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (isPhoneLoading) {
                                TextButton(
                                    onClick = {
                                        phoneRequestToken += 1
                                        isPhoneLoading = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("منسوخ کریں")
                                }
                            }

                            OutlinedButton(
                                onClick = { triggerSendOtp(useResendToken = true) },
                                enabled = !isPhoneLoading && resendCountdown == 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("resend_otp_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (resendCountdown > 0) {
                                        "کوڈ دوبارہ بھیجیں (${resendCountdown} سیکنڈ)"
                                    } else {
                                        "OTP کوڈ دوبارہ بھیجیں"
                                    }
                                )
                            }
                        }

                        if (infoMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = infoMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Shop Contact Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "پتہ",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = shop.address,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { openDialer(context, shop.contact1Phone) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(shop.contact1Name)
                            }
                            Button(
                                onClick = {
                                    openWhatsAppChat(
                                        context,
                                        shop.whatsappNumber,
                                        "السلام علیکم ${shop.businessName}! مجھے ہول سیل ریٹس اور اکاؤنٹ کی معلومات درکار ہیں۔"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("واٹس ایپ")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHiddenAdminDialog) {
        HiddenAdminLoginDialog(
            onDismiss = { showHiddenAdminDialog = false },
            onAdminAuthenticated = {
                showHiddenAdminDialog = false
                onAuthSuccess()
            }
        )
    }
}
