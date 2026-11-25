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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountStatus
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateMessage
import com.example.util.formatTimestamp

@Composable
fun AdminUsersTab(
    users: List<UserProfile>,
    isBusy: Boolean,
    onUpdateStatus: (userId: String, status: AccountStatus) -> Unit
) {
    var statusFilter by remember { mutableStateOf<AccountStatus?>(null) }

    val filteredUsers = remember(users, statusFilter) {
        if (statusFilter == null) users
        else users.filter { it.parsedStatus() == statusFilter }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("سب (${users.size})") }
                )
            }
            items(AccountStatus.entries) { status ->
                val count = users.count { it.parsedStatus() == status }
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { statusFilter = status },
                    label = { Text("${status.urduLabel} ($count)") }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("admin_users_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredUsers.isEmpty()) {
                item {
                    EmptyStateMessage(
                        icon = Icons.Filled.People,
                        title = "کوئی رجسٹرڈ صارف نہیں ملا",
                        subtitle = "جب کسٹمرز اپنے موبائل نمبر اور OTP سے سائن اپ کریں گے تو وہ یہاں منظور یا بلاک کرنے کے لیے نظر آئیں گے۔"
                    )
                }
            } else {
                items(filteredUsers, key = { it.id.ifBlank { it.userId } }) { user ->
                    val targetId = user.id.ifBlank { user.userId }
                    val status = user.parsedStatus()
                    AdminUserCard(
                        user = user,
                        status = status,
                        isBusy = isBusy,
                        onApprove = { onUpdateStatus(targetId, AccountStatus.APPROVED) },
                        onBlock = { onUpdateStatus(targetId, AccountStatus.BLOCKED) },
                        onUnblock = { onUpdateStatus(targetId, AccountStatus.APPROVED) }
                    )
                }
            }
            item { Spacer(Modifier.height(64.dp)) }
        }
    }
}

@Composable
private fun AdminUserCard(
    user: UserProfile,
    status: AccountStatus,
    isBusy: Boolean,
    onApprove: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit
) {
    val badgeBg = when (status) {
        AccountStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer
        AccountStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
        AccountStatus.BLOCKED -> MaterialTheme.colorScheme.errorContainer
    }
    val badgeFg = when (status) {
        AccountStatus.APPROVED -> MaterialTheme.colorScheme.onPrimaryContainer
        AccountStatus.PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
        AccountStatus.BLOCKED -> MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName.ifBlank { "بغیر نام صارف" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "رجسٹریشن: ${formatTimestamp(user.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = status.urduLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = user.phoneNumber.ifBlank { "—" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = user.shopName.ifBlank { user.shopOrAddress }.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.LocationCity, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = user.city.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (status) {
                    AccountStatus.PENDING -> {
                        Button(
                            onClick = onApprove,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("approve_user_${user.userId}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("منظور کریں (Approve)", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onBlock,
                            enabled = !isBusy,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("block_user_${user.userId}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(6.dp))
                            Text("بلاک کریں", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    AccountStatus.APPROVED -> {
                        OutlinedButton(
                            onClick = onBlock,
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("block_user_${user.userId}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(6.dp))
                            Text("اکاؤنٹ بلاک کریں (Block)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                    AccountStatus.BLOCKED -> {
                        Button(
                            onClick = onUnblock,
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("unblock_user_${user.userId}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ان بلاک کریں (Unblock)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
