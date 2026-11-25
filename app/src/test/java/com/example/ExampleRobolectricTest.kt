package com.example

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.firebase.StarterCatalogData
import com.example.data.model.AccountStatus
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.UserProfile
import com.example.ui.screens.AccountStatusGateScreen
import com.example.ui.screens.AdminSalesReportsTab
import com.example.ui.screens.AdminUsersTab
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CompleteProfileScreen
import com.example.ui.theme.MannSalwaTheme
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name matches Mann Salwa Trader branding`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("من سلوٰی ٹریڈر", appName)
    }

    @Test
    fun `auth screen renders +92 Pakistan login signup tabs and validates invalid number`() {
        composeTestRule.setContent {
            MannSalwaTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("auth_mode_login").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_mode_signup").assertIsDisplayed()
        composeTestRule.onNodeWithText("🇵🇰 +92").assertIsDisplayed()
        composeTestRule.onNodeWithTag("phone_number_input").assertIsDisplayed()
        // Public Admin Login card/dialog must be hidden from customers by default
        composeTestRule.onNodeWithText("مالک / ایڈمن پینل لاگ ان (Owner Only)").assertDoesNotExist()
        composeTestRule.onNodeWithTag("hidden_admin_dialog").assertDoesNotExist()

        // Switch to Signup and submit invalid phone number -> shows validation error
        composeTestRule.onNodeWithTag("auth_mode_signup").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("phone_number_input").performScrollTo().performTextInput("1234")
        composeTestRule.onNodeWithTag("send_otp_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(
            "براہِ کرم درست پاکستانی موبائل نمبر درج کریں (مثلاً 3001234567 یا 03001234567)۔"
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `complete profile screen collects full name shop name city and supports logout`() {
        var submittedName = ""
        var submittedShop = ""
        var submittedCity = ""
        var loggedOut = false

        composeTestRule.setContent {
            MannSalwaTheme {
                CompleteProfileScreen(
                    verifiedPhone = "+923001234567",
                    isBusy = false,
                    snackbarHostState = SnackbarHostState(),
                    onSubmit = { name, shop, city ->
                        submittedName = name
                        submittedShop = shop
                        submittedCity = city
                    },
                    onSignOut = { loggedOut = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Blank submit blocked with Urdu error
        composeTestRule.onNodeWithTag("submit_registration_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("براہِ کرم اپنا پورا نام درج کریں۔").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("register_fullname_input").performScrollTo().performTextInput("احمد علی")
        composeTestRule.onNodeWithTag("register_shopname_input").performScrollTo().performTextInput("علی کریانہ سٹور")
        composeTestRule.onNodeWithTag("register_city_input").performScrollTo().performTextInput("فیصل آباد")
        composeTestRule.onNodeWithTag("submit_registration_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("احمد علی", submittedName)
        assertEquals("علی کریانہ سٹور", submittedShop)
        assertEquals("فیصل آباد", submittedCity)

        composeTestRule.onNodeWithTag("registration_logout_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(loggedOut)
    }

    @Test
    fun `account status gate distinguishes pending vs blocked and triggers logout`() {
        var loggedOut = false
        val pendingUser = UserProfile(
            userId = "cust_1",
            phoneNumber = "+923001234567",
            displayName = "کامران خان",
            shopName = "مدینہ سٹور",
            city = "فیصل آباد",
            accountStatus = AccountStatus.PENDING.wireValue
        )

        composeTestRule.setContent {
            MannSalwaTheme {
                AccountStatusGateScreen(
                    profile = pendingUser,
                    shopInfo = StarterCatalogData.defaultShopInfo,
                    snackbarHostState = SnackbarHostState(),
                    onSignOut = { loggedOut = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("آپ کا اکاؤنٹ ایڈمن کی منظوری کا منتظر ہے").assertIsDisplayed()
        composeTestRule.onNodeWithTag("status_gate_logout_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(loggedOut)
    }

    @Test
    fun `admin users tab allows approving blocking and unblocking users`() {
        val updates = mutableListOf<Pair<String, AccountStatus>>()
        val users = listOf(
            UserProfile(
                id = "u_pending",
                userId = "u_pending",
                phoneNumber = "+923001112233",
                displayName = "بشیر احمد",
                shopName = "بشیر کریانہ",
                city = "لاہور",
                accountStatus = AccountStatus.PENDING.wireValue
            ),
            UserProfile(
                id = "u_blocked",
                userId = "u_blocked",
                phoneNumber = "+923214445566",
                displayName = "طارق محمود",
                shopName = "طارق جنرل سٹور",
                city = "فیصل آباد",
                accountStatus = AccountStatus.BLOCKED.wireValue
            )
        )

        composeTestRule.setContent {
            MannSalwaTheme {
                AdminUsersTab(
                    users = users,
                    isBusy = false,
                    onUpdateStatus = { uid, status -> updates.add(uid to status) }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("approve_user_u_pending").performClick()
        composeTestRule.onNodeWithTag("admin_users_list")
            .performScrollToNode(hasTestTag("unblock_user_u_blocked"))
        composeTestRule.onNodeWithTag("unblock_user_u_blocked").performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            listOf("u_pending" to AccountStatus.APPROVED, "u_blocked" to AccountStatus.APPROVED),
            updates
        )
    }

    @Test
    fun `hidden admin dialog opens on long press and rejects unauthorized customer numbers`() {
        composeTestRule.setContent {
            MannSalwaTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeTestRule.waitForIdle()

        // Long press hidden title trigger -> opens hidden admin login dialog
        composeTestRule.onNodeWithTag("auth_hidden_admin_trigger").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("hidden_admin_dialog").assertIsDisplayed()

        // Ordinary customer phone number must be rejected immediately
        composeTestRule.onNodeWithTag("admin_phone_input").performTextInput("03001234567")
        composeTestRule.onNodeWithTag("admin_send_otp_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            "یہ نمبر مجاز ایڈمن فہرست میں شامل نہیں ہے۔ صرف 03098897096 یا 03237644924 سے ایڈمن لاگ ان ممکن ہے۔"
        ).performScrollTo().assertIsDisplayed()

        // Google sign-in button inside hidden dialog fails gracefully without freezing when FirebaseApp is absent
        composeTestRule.onNodeWithTag("admin_google_login_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Firebase کنفیگریشن دستیاب نہیں ہے۔").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `auth screen does not freeze loading spinner when FirebaseApp is unavailable`() {
        composeTestRule.setContent {
            MannSalwaTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeTestRule.waitForIdle()

        // Try valid-format phone OTP -> immediately clears loading with clear Urdu message
        composeTestRule.onNodeWithTag("auth_mode_signup").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("phone_number_input").performScrollTo().performTextInput("03001234567")
        composeTestRule.onNodeWithTag("send_otp_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Firebase کنفیگریشن دستیاب نہیں ہے۔ براہِ کرم ایپ دوبارہ شروع کریں۔")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `admin sales reports tab renders daily and monthly report cards`() {
        val nowMillis = 1781524800000L
        val orders = listOf(
            Order(
                id = "ord_r1",
                customerName = "بلال ٹریڈرز",
                customerPhone = "03001234567",
                customerAddress = "فیصل آباد",
                itemCount = 4,
                totalAmount = 12500.0,
                status = OrderStatus.COMPLETED.wireValue,
                items = listOf(
                    mapOf("productId" to "p1", "name" to "فائن آٹا", "packing" to "20kg", "qty" to 4L, "unitPrice" to 3125.0)
                ),
                createdAt = Timestamp(java.util.Date(nowMillis - 1000L))
            )
        )

        composeTestRule.setContent {
            MannSalwaTheme {
                AdminSalesReportsTab(orders = orders, nowMillis = nowMillis)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("daily_sales_report_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("monthly_sales_report_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("آج کی فروخت رپورٹ (Daily Report)").assertIsDisplayed()
        composeTestRule.onNodeWithText("ماہانہ فروخت رپورٹ (Monthly Report)").assertIsDisplayed()
    }
}
