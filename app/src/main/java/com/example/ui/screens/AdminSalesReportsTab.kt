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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.SalesReportCalculator
import com.example.ui.components.EmptyStateMessage
import com.example.ui.theme.PriceDecreaseContainer
import com.example.ui.theme.PriceDecreaseGreen
import com.example.util.formatPkr

@Composable
fun AdminSalesReportsTab(
    orders: List<Order>,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis()
) {
    val snapshot = remember(orders, nowMillis) {
        SalesReportCalculator.calculate(orders = orders, nowMillis = nowMillis)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_reports_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_sales_report_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Today,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "آج کی فروخت رپورٹ (Daily Report)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = snapshot.daily.dateLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${snapshot.daily.orderCount} آرڈرز",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportMetricTile(
                            label = "آج کی کل فروخت",
                            value = formatPkr(snapshot.daily.revenuePkr),
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricTile(
                            label = "مکمل آرڈرز آمدنی",
                            value = formatPkr(snapshot.daily.completedRevenuePkr),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportMetricTile(
                            label = "فروخت شدہ آئٹمز",
                            value = snapshot.daily.itemCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricTile(
                            label = "اوسط آرڈر بل",
                            value = formatPkr(snapshot.daily.averageOrderValue),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "زیرِ التواء: ${snapshot.daily.pendingCount} • مکمل: ${snapshot.daily.completedCount} • منسوخ: ${snapshot.daily.cancelledCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("monthly_sales_report_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Text(
                                    text = "ماہانہ فروخت رپورٹ (Monthly Report)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = snapshot.monthly.monthLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${snapshot.monthly.orderCount} آرڈرز",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportMetricTile(
                            label = "ماہانہ کل فروخت",
                            value = formatPkr(snapshot.monthly.revenuePkr),
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricTile(
                            label = "مکمل شدہ فروخت",
                            value = formatPkr(snapshot.monthly.completedRevenuePkr),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "اس ماہ کل فروخت شدہ تعداد: ${snapshot.monthly.itemCount} یونٹس / پیکٹس",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (orders.isEmpty()) {
            item {
                EmptyStateMessage(
                    icon = Icons.Filled.BarChart,
                    title = "ابھی تک کوئی آرڈر موجود نہیں",
                    subtitle = "جیسے ہی کسٹمرز آرڈرز بھیجیں گے، روزانہ اور ماہانہ فروخت کی تفصیلی رپورٹس یہاں خودکار طور پر نظر آئیں گی۔"
                )
            }
        } else {
            if (snapshot.monthly.topProducts.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "اس ماہ کی سرفہرست مصنوعات",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider()
                            snapshot.monthly.topProducts.forEachIndexed { idx, prod ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${prod.name} (تعداد: ${prod.totalQty})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatPkr(prod.totalRevenue),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (snapshot.recentDays.isNotEmpty()) {
                item {
                    Text(
                        text = "روزانہ فروخت کا ریکارڈ (گزشتہ ایام)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(snapshot.recentDays, key = { "day_${it.periodKey}" }) { row ->
                    SalesHistoryRowCard(
                        title = row.label,
                        subtitle = "${row.orderCount} آرڈرز • ${row.itemCount} آئٹمز",
                        amountPkr = row.revenuePkr
                    )
                }
            }

            if (snapshot.recentMonths.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "ماہانہ فروخت کا خلاصہ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                items(snapshot.recentMonths, key = { "month_${it.periodKey}" }) { row ->
                    SalesHistoryRowCard(
                        title = row.label,
                        subtitle = "${row.orderCount} آرڈرز • ${row.itemCount} آئٹمز",
                        amountPkr = row.revenuePkr
                    )
                }
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

@Composable
private fun ReportMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SalesHistoryRowCard(
    title: String,
    subtitle: String,
    amountPkr: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                color = PriceDecreaseContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = formatPkr(amountPkr),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PriceDecreaseGreen,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
