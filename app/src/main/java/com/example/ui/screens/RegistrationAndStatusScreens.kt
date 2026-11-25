package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountStatus
import com.example.data.model.ShopInfo
import com.example.data.model.UserProfile
import com.example.ui.theme.WhatsAppGreen
import com.example.util.openDialer
import com.example.util.openWhatsAppChat

@Composable
fun CompleteProfileScreen(
    verifiedPhone: String,
    isBusy: Boolean,
    snackbarHostState: SnackbarHostState,
    onSubmit: (fullName: String, shopName: String, city: String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fullName by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
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
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "موبائل نمبر کی تصدیق مکمل ہو گئی",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "اکاؤنٹ کی تفصیلات مکمل کریں",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "من سلوٰی ٹریڈر میں رجسٹریشن مکمل کرنے کے لیے براہِ کرم اپنا پورا نام، دکان کا نام اور شہر درج کریں۔",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhoneAndroid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "تصدیق شدہ موبائل نمبر",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = verifiedPhone.ifBlank { "—" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                validationError = null
                            },
                            label = { Text("پورا نام (Full Name) *") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_fullname_input")
                        )

                        OutlinedTextField(
                            value = shopName,
                            onValueChange = {
                                shopName = it
                                validationError = null
                            },
                            label = { Text("دکان کا نام (Shop Name) *") },
                            leadingIcon = { Icon(Icons.Filled.Storefront, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_shopname_input")
                        )

                        OutlinedTextField(
                            value = city,
                            onValueChange = {
                                city = it
                                validationError = null
                            },
                            label = { Text("شہر (City) *") },
                            leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_city_input")
                        )

                        if (validationError != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = validationError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                when {
                                    fullName.isBlank() -> validationError = "براہِ کرم اپنا پورا نام درج کریں۔"
                                    shopName.isBlank() -> validationError = "براہِ کرم اپنی دکان کا نام درج کریں۔"
                                    city.isBlank() -> validationError = "براہِ کرم اپنے شہر کا نام درج کریں۔"
                                    else -> {
                                        validationError = null
                                        onSubmit(fullName.trim(), shopName.trim(), city.trim())
                                    }
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("submit_registration_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = "اکاؤنٹ مکمل کریں",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("registration_logout_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("لاگ آؤٹ کریں")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountStatusGateScreen(
    profile: UserProfile,
    shopInfo: ShopInfo,
    snackbarHostState: SnackbarHostState,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val status = profile.parsedStatus()
    val isBlocked = status == AccountStatus.BLOCKED

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
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
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            color = if (isBlocked) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Icon(
                                imageVector = if (isBlocked) Icons.Filled.Block else Icons.Filled.HourglassTop,
                                contentDescription = null,
                                tint = if (isBlocked) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Text(
                            text = if (isBlocked) {
                                "آپ کا اکاؤنٹ بلاک کر دیا گیا ہے"
                            } else {
                                "آپ کا اکاؤنٹ ایڈمن کی منظوری کا منتظر ہے"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isBlocked) {
                                "معذرت! من سلوٰی ٹریڈر کے ایڈمن نے فی الحال آپ کا اکاؤنٹ عارضی طور پر بند کر دیا ہے۔ مزید معلومات کے لیے براہِ راست رابطہ کریں۔"
                            } else {
                                "شکریہ ${profile.displayName}! آپ کی رجسٹریشن موصول ہو گئی ہے۔ جیسے ہی من سلوٰی ٹریڈر کے مالک آپ کا اکاؤنٹ منظور کریں گے، یہ اسکرین خود بخود اسٹور میں کھل جائے گی۔"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        HorizontalDivider()

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "نام: ${profile.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "دکان: ${profile.shopName} • شہر: ${profile.city}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("موبائل:", style = MaterialTheme.typography.bodyMedium)
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = profile.phoneNumber,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { openDialer(context, shopInfo.contact2Phone) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("کال کریں")
                            }
                            Button(
                                onClick = {
                                    openWhatsAppChat(
                                        context,
                                        shopInfo.whatsappNumber,
                                        "السلام علیکم ${shopInfo.businessName}! میرا نام ${profile.displayName} (${profile.shopName}، ${profile.city}) ہے، نمبر ${profile.phoneNumber}۔ براہِ کرم میرا اکاؤنٹ منظور فرمائیں۔"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("واٹس ایپ")
                            }
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("status_gate_logout_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("لاگ آؤٹ کریں (Logout)")
                        }
                    }
                }
            }
        }
    }
}
