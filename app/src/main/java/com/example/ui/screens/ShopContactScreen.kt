package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.StarterCatalogData
import com.example.data.firebase.signOut
import com.example.data.model.UiState
import com.example.ui.theme.WhatsAppGreen
import com.example.util.openDialer
import com.example.util.openWhatsAppChat
import com.example.viewmodel.StoreViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun ShopContactScreen(
    viewModel: StoreViewModel,
    onOpenAdminDashboard: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val shopState by viewModel.shopInfoState.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val shopInfo = (shopState as? UiState.Success)?.data ?: StarterCatalogData.defaultShopInfo
    val userProfile = (profileState as? UiState.Success)?.data
    val user = Firebase.auth.currentUser

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = shopInfo.businessName,
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "ہول سیل کریانہ اینڈ جنرل سپلائرز",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "پتہ",
                            tint = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = shopInfo.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        if (viewModel.isOwnerAdmin) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = "سپر ایڈمن",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "سپر ایڈمن کنٹرول پینل",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "صرف دکان کے مالک کے لیے: روزانہ ریٹس، مصنوعات، آرڈرز اور بیک اپ مینیج کریں۔",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Button(
                            onClick = onOpenAdminDashboard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("open_admin_dashboard_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("ایڈمن ڈیش بورڈ کھولیں", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "براہِ راست رابطہ نمبرز",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ContactPersonRow(
                        name = shopInfo.contact1Name,
                        phone = shopInfo.contact1Phone,
                        onCall = { openDialer(context, shopInfo.contact1Phone) }
                    )

                    ContactPersonRow(
                        name = shopInfo.contact2Name,
                        phone = shopInfo.contact2Phone,
                        onCall = { openDialer(context, shopInfo.contact2Phone) }
                    )

                    Button(
                        onClick = {
                            openWhatsAppChat(
                                context,
                                shopInfo.whatsappNumber,
                                "السلام علیکم ${shopInfo.businessName}! مجھے سامان کے ریٹ اور آرڈر کے سلسلے میں رابطہ کرنا ہے۔"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("contact_whatsapp_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "واٹس ایپ پر رابطہ کریں (${shopInfo.whatsappNumber})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile?.displayName?.ifBlank { null }
                                    ?: user?.displayName?.ifBlank { null }
                                    ?: "رجسٹرڈ کسٹمر",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            val secondaryLine = when {
                                userProfile != null -> {
                                    val shopCity = listOf(userProfile.shopName, userProfile.city)
                                        .filter { it.isNotBlank() }
                                        .joinToString("، ")
                                    if (shopCity.isNotBlank()) "${userProfile.phoneNumber} • $shopCity"
                                    else userProfile.phoneNumber
                                }
                                !user?.phoneNumber.isNullOrBlank() -> user.phoneNumber ?: ""
                                else -> user?.email ?: ""
                            }
                            Text(
                                text = secondaryLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = if (viewModel.isOwnerAdmin) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (viewModel.isOwnerAdmin) "مالک (Admin)"
                                else userProfile?.parsedStatus()?.urduLabel ?: "کسٹمر",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            signOut(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = onSignedOut
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_out_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("لاگ آؤٹ کریں (Logout)")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun ContactPersonRow(
    name: String,
    phone: String,
    onCall: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
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
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            OutlinedButton(
                onClick = onCall,
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Filled.Call, contentDescription = "کال کریں", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("کال کریں")
            }
        }
    }
}
