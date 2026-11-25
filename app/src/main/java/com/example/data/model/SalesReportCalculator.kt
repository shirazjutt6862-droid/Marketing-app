package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class TopSellingProduct(
    val name: String,
    val totalQty: Long,
    val totalRevenue: Double
)

data class PeriodSalesRow(
    val periodKey: String,
    val label: String,
    val orderCount: Int,
    val revenuePkr: Double,
    val itemCount: Long
)

data class DailySalesSummary(
    val dateLabel: String,
    val orderCount: Int,
    val revenuePkr: Double,
    val completedRevenuePkr: Double,
    val itemCount: Long,
    val averageOrderValue: Double,
    val pendingCount: Int,
    val completedCount: Int,
    val cancelledCount: Int
)

data class MonthlySalesSummary(
    val monthLabel: String,
    val orderCount: Int,
    val revenuePkr: Double,
    val completedRevenuePkr: Double,
    val itemCount: Long,
    val topProducts: List<TopSellingProduct>
)

data class SalesReportSnapshot(
    val daily: DailySalesSummary,
    val monthly: MonthlySalesSummary,
    val recentDays: List<PeriodSalesRow>,
    val recentMonths: List<PeriodSalesRow>
)

object SalesReportCalculator {

    fun calculate(
        orders: List<Order>,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): SalesReportSnapshot {
        val urLocale = Locale("ur", "PK")
        val dayKeyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { this.timeZone = timeZone }
        val dayDisplayFmt = SimpleDateFormat("dd MMM yyyy", urLocale).apply { this.timeZone = timeZone }
        val monthKeyFmt = SimpleDateFormat("yyyy-MM", Locale.US).apply { this.timeZone = timeZone }
        val monthDisplayFmt = SimpleDateFormat("MMMM yyyy", urLocale).apply { this.timeZone = timeZone }

        val nowDate = Date(nowMillis)
        val todayKey = dayKeyFmt.format(nowDate)
        val currentMonthKey = monthKeyFmt.format(nowDate)

        val todayOrders = mutableListOf<Order>()
        val monthOrders = mutableListOf<Order>()
        val dayBuckets = linkedMapOf<String, MutableList<Order>>()
        val monthBuckets = linkedMapOf<String, MutableList<Order>>()

        for (order in orders) {
            val orderDate = order.createdAt?.toDate() ?: nowDate
            val dKey = dayKeyFmt.format(orderDate)
            val mKey = monthKeyFmt.format(orderDate)

            if (dKey == todayKey) {
                todayOrders.add(order)
            }
            if (mKey == currentMonthKey) {
                monthOrders.add(order)
            }

            if (order.parsedStatus() != OrderStatus.CANCELLED) {
                dayBuckets.getOrPut(dKey) { mutableListOf() }.add(order)
                monthBuckets.getOrPut(mKey) { mutableListOf() }.add(order)
            }
        }

        val activeToday = todayOrders.filter { it.parsedStatus() != OrderStatus.CANCELLED }
        val todayRevenue = activeToday.sumOf { it.totalAmount }
        val todayCompletedRevenue = activeToday
            .filter { it.parsedStatus() == OrderStatus.COMPLETED }
            .sumOf { it.totalAmount }
        val todayItems = activeToday.sumOf { it.itemCount }
        val todayAvg = if (activeToday.isNotEmpty()) todayRevenue / activeToday.size else 0.0

        val dailySummary = DailySalesSummary(
            dateLabel = dayDisplayFmt.format(nowDate),
            orderCount = activeToday.size,
            revenuePkr = todayRevenue,
            completedRevenuePkr = todayCompletedRevenue,
            itemCount = todayItems,
            averageOrderValue = todayAvg,
            pendingCount = todayOrders.count { it.parsedStatus() == OrderStatus.PENDING },
            completedCount = todayOrders.count { it.parsedStatus() == OrderStatus.COMPLETED },
            cancelledCount = todayOrders.count { it.parsedStatus() == OrderStatus.CANCELLED }
        )

        val activeMonth = monthOrders.filter { it.parsedStatus() != OrderStatus.CANCELLED }
        val monthRevenue = activeMonth.sumOf { it.totalAmount }
        val monthCompletedRevenue = activeMonth
            .filter { it.parsedStatus() == OrderStatus.COMPLETED }
            .sumOf { it.totalAmount }
        val monthItems = activeMonth.sumOf { it.itemCount }

        val productAccumulator = mutableMapOf<String, Pair<Long, Double>>()
        for (order in activeMonth) {
            for (line in order.parsedItems()) {
                val key = line.name.ifBlank { line.productId }
                val prev = productAccumulator[key] ?: (0L to 0.0)
                productAccumulator[key] = (prev.first + line.qty) to (prev.second + (line.qty * line.unitPrice))
            }
        }
        val topProducts = productAccumulator.entries
            .map { (name, totals) ->
                TopSellingProduct(
                    name = name,
                    totalQty = totals.first,
                    totalRevenue = totals.second
                )
            }
            .sortedByDescending { it.totalRevenue }
            .take(5)

        val monthlySummary = MonthlySalesSummary(
            monthLabel = monthDisplayFmt.format(nowDate),
            orderCount = activeMonth.size,
            revenuePkr = monthRevenue,
            completedRevenuePkr = monthCompletedRevenue,
            itemCount = monthItems,
            topProducts = topProducts
        )

        val recentDays = dayBuckets.entries
            .sortedByDescending { it.key }
            .take(14)
            .map { (key, bucket) ->
                val sampleDate = bucket.firstOrNull()?.createdAt?.toDate() ?: nowDate
                PeriodSalesRow(
                    periodKey = key,
                    label = if (key == todayKey) "آج (${dayDisplayFmt.format(sampleDate)})" else dayDisplayFmt.format(sampleDate),
                    orderCount = bucket.size,
                    revenuePkr = bucket.sumOf { it.totalAmount },
                    itemCount = bucket.sumOf { it.itemCount }
                )
            }

        val recentMonths = monthBuckets.entries
            .sortedByDescending { it.key }
            .take(12)
            .map { (key, bucket) ->
                val sampleDate = bucket.firstOrNull()?.createdAt?.toDate() ?: nowDate
                PeriodSalesRow(
                    periodKey = key,
                    label = monthDisplayFmt.format(sampleDate),
                    orderCount = bucket.size,
                    revenuePkr = bucket.sumOf { it.totalAmount },
                    itemCount = bucket.sumOf { it.itemCount }
                )
            }

        return SalesReportSnapshot(
            daily = dailySummary,
            monthly = monthlySummary,
            recentDays = recentDays,
            recentMonths = recentMonths
        )
    }
}
