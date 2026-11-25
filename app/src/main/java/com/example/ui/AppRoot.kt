package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.firebase.StarterCatalogData
import com.example.data.firebase.StoreRepository
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.isVerifiedOwner
import com.example.data.firebase.signOut
import com.example.data.model.AccountStatus
import com.example.data.model.UiState
import com.example.ui.screens.AccountStatusGateScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartAndOrdersScreen
import com.example.ui.screens.CompleteProfileScreen
import com.example.ui.screens.DailyRatesScreen
import com.example.ui.screens.HiddenAdminLoginDialog
import com.example.ui.screens.HomeCatalogScreen
import com.example.ui.screens.ShopContactScreen
import com.example.viewmodel.StoreViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

const val ROUTE_HOME = "home"
const val ROUTE_RATES = "rates"
const val ROUTE_CART = "cart"
const val ROUTE_CONTACT = "contact"
const val ROUTE_ADMIN = "admin"

private data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AppRoot() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val currentUser by Firebase.auth.authStateFlow()
            .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

        val user = currentUser
        if (user == null) {
            AuthScreen(onAuthSuccess = {})
        } else {
            key(user.uid) {
                AuthenticatedAppShell(user = user)
            }
        }
    }
}

@Composable
private fun AuthenticatedAppShell(user: FirebaseUser) {
    val isOwnerAdmin = remember(user.uid, user.email, user.isEmailVerified, user.phoneNumber) {
        isVerifiedOwner(user)
    }

    val viewModel: StoreViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StoreViewModel(
                    repository = StoreRepository(db),
                    currentUserId = user.uid,
                    isOwnerAdmin = isOwnerAdmin
                )
            }
        }
    )

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val shopInfoState by viewModel.shopInfoState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val triggerSignOut = {
        signOut(credentialManager = credentialManager, scope = scope)
    }

    if (!isOwnerAdmin) {
        val shopInfo = (shopInfoState as? UiState.Success)?.data ?: StarterCatalogData.defaultShopInfo
        var loadingTimedOut by remember { mutableStateOf(false) }

        LaunchedEffect(userProfileState) {
            if (userProfileState is UiState.Loading) {
                loadingTimedOut = false
                delay(8_000L)
                loadingTimedOut = true
            } else {
                loadingTimedOut = false
            }
        }

        when (val profileState = userProfileState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = if (loadingTimedOut) {
                                "سرور سے رابطہ میں تاخیر ہو رہی ہے۔ براہِ کرم انٹرنیٹ چیک کریں یا دوبارہ کوشش کریں۔"
                            } else {
                                "اکاؤنٹ کی معلومات لوڈ ہو رہی ہیں..."
                            },
                            textAlign = TextAlign.Center
                        )
                        if (loadingTimedOut) {
                            Button(
                                onClick = {
                                    loadingTimedOut = false
                                    viewModel.retryConnection()
                                },
                                modifier = Modifier.testTag("profile_retry_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("دوبارہ لوڈ کریں")
                            }
                        }
                        OutlinedButton(
                            onClick = triggerSignOut,
                            modifier = Modifier.testTag("loading_logout_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("واپس جائیں / لاگ آؤٹ کریں")
                        }
                    }
                }
                return
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = profileState.message,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.retryConnection() },
                            modifier = Modifier.testTag("profile_error_retry_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("دوبارہ کوشش کریں")
                        }
                        OutlinedButton(onClick = triggerSignOut) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("لاگ آؤٹ کریں")
                        }
                    }
                }
                return
            }
            is UiState.Success -> {
                val profile = profileState.data
                if (profile == null) {
                    CompleteProfileScreen(
                        verifiedPhone = user.phoneNumber ?: "",
                        isBusy = isBusy,
                        snackbarHostState = snackbarHostState,
                        onSubmit = { fullName, shopName, city ->
                            viewModel.registerCustomerProfile(
                                phoneNumber = user.phoneNumber ?: "",
                                fullName = fullName,
                                shopName = shopName,
                                city = city
                            )
                        },
                        onSignOut = triggerSignOut
                    )
                    return
                }
                if (profile.parsedStatus() != AccountStatus.APPROVED) {
                    AccountStatusGateScreen(
                        profile = profile,
                        shopInfo = shopInfo,
                        snackbarHostState = snackbarHostState,
                        onSignOut = triggerSignOut
                    )
                    return
                }
            }
        }
    }

    StoreMainNavigationShell(
        viewModel = viewModel,
        isOwnerAdmin = isOwnerAdmin,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun StoreMainNavigationShell(
    viewModel: StoreViewModel,
    isOwnerAdmin: Boolean,
    snackbarHostState: SnackbarHostState
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME
    val cartCount by viewModel.cartTotalCount.collectAsStateWithLifecycle()
    var showHiddenAdminDialog by remember { mutableStateOf(false) }
    var titleTapCount by remember { mutableIntStateOf(0) }

    val history = rememberRouteHistoryState(ROUTE_HOME)
    val canGoBack = history.canGoBack
    val canGoForward = history.canGoForward

    fun applyNavControllerRoute(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToRoute(route: String) {
        if (history.push(route) || currentRoute != route) {
            applyNavControllerRoute(route)
        }
    }

    fun goBack() {
        history.goBack()?.let { targetRoute ->
            applyNavControllerRoute(targetRoute)
        }
    }

    fun goForward() {
        history.goForward()?.let { targetRoute ->
            applyNavControllerRoute(targetRoute)
        }
    }

    // Intercept system Back inside main tabs so Back walks through history cleanly
    BackHandler(enabled = canGoBack && currentRoute != ROUTE_ADMIN) {
        goBack()
    }

    val tabs = remember {
        listOf(
            BottomTab(ROUTE_HOME, "اسٹور", Icons.Filled.Storefront, Icons.Outlined.Storefront),
            BottomTab(ROUTE_RATES, "آج کے ریٹس", Icons.Filled.PriceChange, Icons.Outlined.PriceChange),
            BottomTab(ROUTE_CART, "کارٹ", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
            BottomTab(ROUTE_CONTACT, "رابطہ", Icons.Filled.Call, Icons.Outlined.Call)
        )
    }

    val showChrome = currentRoute != ROUTE_ADMIN

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (showChrome) {
                    TopAppBar(
                        navigationIcon = {
                            if (canGoBack) {
                                IconButton(
                                    onClick = { goBack() },
                                    modifier = Modifier.testTag("nav_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "پیچھے جائیں"
                                    )
                                }
                            }
                        },
                        title = {
                            Text(
                                text = "من سلوٰی ٹریڈر",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .testTag("top_hidden_admin_trigger")
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            titleTapCount += 1
                                            if (titleTapCount >= 5) {
                                                titleTapCount = 0
                                                if (isOwnerAdmin) navigateToRoute(ROUTE_ADMIN)
                                                else showHiddenAdminDialog = true
                                            }
                                        },
                                        onLongClick = {
                                            titleTapCount = 0
                                            if (isOwnerAdmin) navigateToRoute(ROUTE_ADMIN)
                                            else showHiddenAdminDialog = true
                                        }
                                    )
                            )
                        },
                        actions = {
                            if (canGoForward) {
                                IconButton(
                                    onClick = { goForward() },
                                    modifier = Modifier.testTag("nav_forward_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "آگے جائیں"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            bottomBar = {
                if (showChrome && !useRail) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToRoute(tab.route) },
                                icon = {
                                    if (tab.route == ROUTE_CART && cartCount > 0) {
                                        BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChrome && useRail) {
                    NavigationRail {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToRoute(tab.route) },
                                icon = {
                                    if (tab.route == ROUTE_CART && cartCount > 0) {
                                        BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_HOME) {
                        HomeCatalogScreen(
                            viewModel = viewModel,
                            onNavigateToCart = { navigateToRoute(ROUTE_CART) }
                        )
                    }
                    composable(ROUTE_RATES) {
                        DailyRatesScreen(viewModel = viewModel)
                    }
                    composable(ROUTE_CART) {
                        CartAndOrdersScreen(
                            viewModel = viewModel,
                            onBrowseProducts = { navigateToRoute(ROUTE_HOME) }
                        )
                    }
                    composable(ROUTE_CONTACT) {
                        ShopContactScreen(
                            viewModel = viewModel,
                            onOpenAdminDashboard = {
                                if (isOwnerAdmin) {
                                    navigateToRoute(ROUTE_ADMIN)
                                }
                            },
                            onSignedOut = {}
                        )
                    }
                    composable(ROUTE_ADMIN) {
                        if (isOwnerAdmin) {
                            AdminDashboardScreen(
                                viewModel = viewModel,
                                onBack = { goBack() }
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                goBack()
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
                if (isOwnerAdmin) {
                    navigateToRoute(ROUTE_ADMIN)
                }
            }
        )
    }
}
