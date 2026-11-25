package com.example

import com.example.data.firebase.isAuthorizedAdminPhoneInput
import com.example.data.model.ADMIN_PHONE_NUMBERS
import com.example.data.model.AccountStatus
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.SalesReportCalculator
import com.example.data.model.UserProfile
import com.example.util.isValidOtpCode
import com.example.util.normalizePakistanPhone
import com.example.util.phoneToRegistryDocId
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.TimeZone

class ExampleUnitTest {

    @Test
    fun `normalizePakistanPhone converts all valid PK formats to E164`() {
        assertEquals("+923001234567", normalizePakistanPhone("3001234567"))
        assertEquals("+923001234567", normalizePakistanPhone("03001234567"))
        assertEquals("+923001234567", normalizePakistanPhone("923001234567"))
        assertEquals("+923098897096", normalizePakistanPhone("+92 309-8897096"))
    }

    @Test
    fun `normalizePakistanPhone rejects invalid or non-mobile numbers`() {
        assertNull(normalizePakistanPhone(""))
        assertNull(normalizePakistanPhone("12345"))
        assertNull(normalizePakistanPhone("0411234567")) // landline 041, not 03xx
        assertNull(normalizePakistanPhone("0300123456789"))
        assertNull(normalizePakistanPhone("2001234567"))
    }

    @Test
    fun `phoneToRegistryDocId maps normalized numbers deterministically for single-account check`() {
        val e164 = normalizePakistanPhone("03237644924")!!
        assertEquals("pk_923237644924", phoneToRegistryDocId(e164))
    }

    @Test
    fun `isValidOtpCode validates exact 6-digit numeric OTPs`() {
        assertTrue(isValidOtpCode("123456"))
        assertTrue(isValidOtpCode("009911"))
        assertFalse(isValidOtpCode("12345"))
        assertFalse(isValidOtpCode("1234567"))
        assertFalse(isValidOtpCode("12a456"))
        assertFalse(isValidOtpCode(""))
    }

    @Test
    fun `accountStatus wire parsing and user profile status defaults safely`() {
        assertEquals(AccountStatus.PENDING, AccountStatus.fromWire("pending"))
        assertEquals(AccountStatus.APPROVED, AccountStatus.fromWire("approved"))
        assertEquals(AccountStatus.BLOCKED, AccountStatus.fromWire("blocked"))
        assertEquals(AccountStatus.PENDING, AccountStatus.fromWire("unknown"))

        val pendingUser = UserProfile(userId = "u1", phoneNumber = "+923001234567")
        assertEquals(AccountStatus.PENDING, pendingUser.parsedStatus())

        val approvedUser = pendingUser.copy(accountStatus = AccountStatus.APPROVED.wireValue)
        assertEquals(AccountStatus.APPROVED, approvedUser.parsedStatus())

        val blockedUser = approvedUser.copy(accountStatus = AccountStatus.BLOCKED.wireValue)
        assertEquals(AccountStatus.BLOCKED, blockedUser.parsedStatus())
    }

    @Test
    fun `routeHistoryState supports back forward trimming and deduplication`() {
        val history = com.example.ui.RouteHistoryState("home")
        assertEquals("home", history.currentRoute)
        assertFalse(history.canGoBack)
        assertFalse(history.canGoForward)

        // Pushing the active route is a no-op
        assertFalse(history.push("home"))

        assertTrue(history.push("rates"))
        assertTrue(history.push("cart"))
        assertEquals("cart", history.currentRoute)
        assertTrue(history.canGoBack)
        assertFalse(history.canGoForward)

        // Step back twice: cart -> rates -> home
        assertEquals("rates", history.goBack())
        assertTrue(history.canGoBack)
        assertTrue(history.canGoForward)

        assertEquals("home", history.goBack())
        assertFalse(history.canGoBack)
        assertTrue(history.canGoForward)
        assertNull(history.goBack())

        // Step forward: home -> rates
        assertEquals("rates", history.goForward())
        assertEquals("rates", history.currentRoute)

        // Navigating to a new route clears the stale forward branch ("cart")
        assertTrue(history.push("contact"))
        assertFalse(history.canGoForward)
        assertEquals(listOf("home", "rates", "contact"), history.entries)
        assertNull(history.goForward())
    }

    @Test
    fun `mapPhoneAuthError translates Play Integrity -9 and reCAPTCHA config errors clearly`() {
        val recaptchaError = IllegalStateException(
            "Failed to initialize reCAPTCHA config: No Recaptcha Enterprise siteKey configured for tenant/project *"
        )
        val recaptchaMsg = com.example.data.firebase.mapPhoneAuthError(recaptchaError)
        assertTrue(recaptchaMsg.contains("reCAPTCHA"))

        val integrityError = IllegalStateException(
            "Integrity API error (-9): Binding to the service in the Play Store has failed."
        )
        val integrityMsg = com.example.data.firebase.mapPhoneAuthError(integrityError)
        assertTrue(integrityMsg.contains("Error -9"))

        val billingError = IllegalStateException("OPERATION_NOT_ALLOWED")
        val billingMsg = com.example.data.firebase.mapPhoneAuthError(billingError)
        assertTrue(billingMsg.contains("Phone Authentication"))
    }

    @Test
    fun `isAuthorizedAdminPhoneInput allows only 03098897096 and 03237644924`() {
        assertEquals(setOf("+923098897096", "+923237644924"), ADMIN_PHONE_NUMBERS)
        assertTrue(isAuthorizedAdminPhoneInput("03098897096"))
        assertTrue(isAuthorizedAdminPhoneInput("+92 309-8897096"))
        assertTrue(isAuthorizedAdminPhoneInput("03237644924"))
        assertTrue(isAuthorizedAdminPhoneInput("923237644924"))

        assertFalse(isAuthorizedAdminPhoneInput("03001234567"))
        assertFalse(isAuthorizedAdminPhoneInput("03121234567"))
        assertFalse(isAuthorizedAdminPhoneInput(""))
    }

    @Test
    fun `salesReportCalculator computes daily and monthly totals excluding cancelled orders`() {
        val utc = TimeZone.getTimeZone("UTC")
        // 2026-06-15 12:00:00 UTC
        val nowMillis = 1781524800000L
        val todayTs = Timestamp(Date(nowMillis - 3600_000L))
        val earlierThisMonthTs = Timestamp(Date(nowMillis - 5L * 86_400_000L))
        val lastMonthTs = Timestamp(Date(nowMillis - 35L * 86_400_000L))

        val orders = listOf(
            Order(
                id = "o1",
                itemCount = 3,
                totalAmount = 10000.0,
                status = OrderStatus.COMPLETED.wireValue,
                items = listOf(
                    mapOf("productId" to "p1", "name" to "چینی", "packing" to "50kg", "qty" to 2L, "unitPrice" to 4000.0),
                    mapOf("productId" to "p2", "name" to "آٹا", "packing" to "20kg", "qty" to 1L, "unitPrice" to 2000.0)
                ),
                createdAt = todayTs
            ),
            Order(
                id = "o2",
                itemCount = 2,
                totalAmount = 5000.0,
                status = OrderStatus.PENDING.wireValue,
                items = listOf(
                    mapOf("productId" to "p1", "name" to "چینی", "packing" to "50kg", "qty" to 1L, "unitPrice" to 5000.0)
                ),
                createdAt = todayTs
            ),
            Order(
                id = "o3",
                itemCount = 4,
                totalAmount = 9999.0,
                status = OrderStatus.CANCELLED.wireValue,
                createdAt = todayTs
            ),
            Order(
                id = "o4",
                itemCount = 5,
                totalAmount = 20000.0,
                status = OrderStatus.COMPLETED.wireValue,
                items = listOf(
                    mapOf("productId" to "p3", "name" to "چاول", "packing" to "25kg", "qty" to 5L, "unitPrice" to 4000.0)
                ),
                createdAt = earlierThisMonthTs
            ),
            Order(
                id = "o5",
                itemCount = 2,
                totalAmount = 8000.0,
                status = OrderStatus.COMPLETED.wireValue,
                createdAt = lastMonthTs
            )
        )

        val snapshot = SalesReportCalculator.calculate(orders = orders, nowMillis = nowMillis, timeZone = utc)

        // Today: o1 (10k) + o2 (5k) active; o3 cancelled
        assertEquals(2, snapshot.daily.orderCount)
        assertEquals(15000.0, snapshot.daily.revenuePkr, 0.01)
        assertEquals(10000.0, snapshot.daily.completedRevenuePkr, 0.01)
        assertEquals(5L, snapshot.daily.itemCount)
        assertEquals(7500.0, snapshot.daily.averageOrderValue, 0.01)
        assertEquals(1, snapshot.daily.pendingCount)
        assertEquals(1, snapshot.daily.completedCount)
        assertEquals(1, snapshot.daily.cancelledCount)

        // Monthly: o1 (10k) + o2 (5k) + o4 (20k) = 35k
        assertEquals(3, snapshot.monthly.orderCount)
        assertEquals(35000.0, snapshot.monthly.revenuePkr, 0.01)
        assertEquals(30000.0, snapshot.monthly.completedRevenuePkr, 0.01)
        assertEquals(10L, snapshot.monthly.itemCount)
        assertEquals("چاول", snapshot.monthly.topProducts.first().name)
        assertEquals(20000.0, snapshot.monthly.topProducts.first().totalRevenue, 0.01)
        assertTrue(snapshot.recentMonths.size >= 2)
    }
}
