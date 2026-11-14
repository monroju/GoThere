package com.example.gothere

import androidx.compose.ui.res.stringResource
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.gothere.ui.theme.GoTabBar
import com.example.gothere.ui.theme.GoTabItem
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Shield
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.gothere.BuildConfig
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import com.example.gothere.ui.SettingsScreen
import com.example.gothere.util.AppearancePrefs
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gothere.auth.AuthRepository
import com.example.gothere.billing.PurchaseManager
import com.example.gothere.ui.AIAssistantScreen
import com.example.gothere.ui.CalendarScreen
import com.example.gothere.ui.countryNameRes
import com.example.gothere.ui.AncestryCheckerScreen
import com.example.gothere.ui.PlanBPathDetailScreen
import com.example.gothere.ui.PlanBScreen
import com.example.gothere.ui.DecisionTreeScreen
import com.example.gothere.ui.DocumentScanScreen
import com.example.gothere.ui.DocumentsScreen
import com.example.gothere.ui.OfflineBanner
import com.example.gothere.ui.OnboardingPrefs
import com.example.gothere.ui.OnboardingScreen
import com.example.gothere.ui.PaywallDialog
import com.example.gothere.ui.ResourcesScreen
import com.example.gothere.ui.TasksScreen
import com.example.gothere.ui.CityMapScreen
import com.example.gothere.ui.VisaWizardScreen
import com.example.gothere.ui.theme.AuthScreen
import com.example.gothere.ui.theme.GoThereTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

// Simple helper functions for country data
private fun getCountryFlag(id: String): String = when (id) {
    "spain" -> "🇪🇸"
    "portugal" -> "🇵🇹"
    "mexico" -> "🇲🇽"
    "canada" -> "🇨🇦"
    "ireland" -> "🇮🇪"
    "italy" -> "🇮🇹"
    "germany" -> "🇩🇪"
    "poland" -> "🇵🇱"
    "argentina" -> "🇦🇷"
    "hungary" -> "🇭🇺"
    "uk_ancestry" -> "🇬🇧"
    else -> "🇪🇸"
}

private val allCountryIds = listOf("spain", "portugal", "mexico", "canada", "ireland", "italy", "germany", "poland", "argentina", "hungary", "uk_ancestry")

sealed class Route(val route: String) {
    data object Tasks : Route("tasks")
    data object Calendar : Route("calendar")
    data object Documents : Route("documents")
    data object Resources : Route("resources")
    data object DecisionTree : Route("decision_tree")
    data object Ancestry : Route("ancestry")
    data object PlanB : Route("planb")
    data object PlanBPath : Route("planb_path/{pathId}") {
        fun create(pathId: String) = "planb_path/$pathId"
    }
    data object AIAssistant : Route("ai_assistant")
    data object DocumentScan : Route("document_scan")
    data object Settings : Route("settings")
    data object VisaWizard : Route("visa_wizard/{countryId}") {
        fun create(countryId: String) = "visa_wizard/$countryId"
    }
    data object CityMap : Route("city_map/{countryId}/{cityId}") {
        fun create(countryId: String, cityId: String) = "city_map/$countryId/$cityId"
    }
}

data class BottomTab(
    val route: Route,
    @androidx.annotation.StringRes val label: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector = icon,
    // Five tabs leave ~80dp per label; anything longer wraps to two lines.
    @androidx.annotation.StringRes val shortLabel: Int = label
)

private val BottomTabs = listOf(
    BottomTab(Route.Tasks, R.string.tab_tasks, Icons.AutoMirrored.Outlined.ListAlt, Icons.AutoMirrored.Filled.ListAlt),
    // Plan B (growth/PLAN_B_SPEC.md 4.1). Six tabs: Documents and Calendar use short labels.
    BottomTab(Route.PlanB, R.string.tab_planb, Icons.Outlined.Shield, Icons.Filled.Shield),
    BottomTab(Route.Calendar, R.string.tab_calendar, Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth, shortLabel = R.string.tab_calendar_short),
    BottomTab(Route.Documents, R.string.tab_documents, Icons.Outlined.Folder, Icons.Filled.Folder, shortLabel = R.string.tab_documents_short),
    BottomTab(Route.Resources, R.string.tab_resources, Icons.Outlined.Description, Icons.Filled.Description),
    BottomTab(Route.DecisionTree, R.string.tab_decision_tree, Icons.Outlined.AccountTree, Icons.Filled.AccountTree, shortLabel = R.string.tab_decide),
)

class MainActivity : AppCompatActivity() {

    private lateinit var purchaseManager: PurchaseManager

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize purchase manager
        purchaseManager = PurchaseManager.getInstance(this)

        PostHogAndroid.setup(
            applicationContext,
            PostHogAndroidConfig(
                apiKey = "phc_zdWSqHah9LiyNqn38H2i3E48XPv5acrWsXedfUWVGSLb",
                host = "https://eu.i.posthog.com"
            ).apply { captureApplicationLifecycleEvents = true }
        )
        com.example.gothere.util.Analytics.init(applicationContext)
        com.example.gothere.util.InstallAttribution.resolveOnce(applicationContext)
        runCatching {
            com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { fa ->
                fa.currentUser?.uid?.let { com.example.gothere.util.Analytics.identify(it) }
            }
        }

        if (BuildConfig.DEBUG) {
            runCatching {
                val app = FirebaseApp.getInstance()
                val options = app.options
                Log.d("FirebaseDiag", "projectId=${options.projectId}")
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snap ->
                        Log.d("FirebaseDiag", "Firestore reachable. users sample size=${snap.size()}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseDiag", "Firestore reachable FAILED", e)
                    }
            }.onFailure { e ->
                Log.e("FirebaseDiag", "Firebase diagnostics block failed", e)
            }
        }

        // Create notification channel + schedule weekly digest
        com.example.gothere.util.NotificationHelper.createNotificationChannel(this)
        com.example.gothere.util.WeeklyDigestScheduler.schedule(this)

        // Subscribe to US policy alerts FCM topic (no permission needed for topics;
        // POST_NOTIFICATIONS will be requested separately via the notify-toggle UX).
        com.example.gothere.notify.FcmTopicManager.subscribeToUSPolicyAlertsIfNeeded(this)

        // 7-day sampler: unlock one paid country (Portugal) for the first week post-install.
        com.example.gothere.billing.FirstWeekTrialService.bootstrap(this)

        setContent {
            var appearance by remember { mutableStateOf(AppearancePrefs.get(this@MainActivity)) }
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDark = when (appearance) {
                AppearancePrefs.DARK -> true
                AppearancePrefs.LIGHT -> false
                else -> systemDark
            }
            // Status/nav bar icons follow the in-app theme, not the system one,
            // so they stay readable when the user forces Dark or Light.
            LaunchedEffect(isDark) {
                val style = if (isDark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            val setAppearance: (String) -> Unit = { value ->
                appearance = value
                AppearancePrefs.set(this@MainActivity, value)
            }
            var selectedCountryId by rememberSaveable { mutableStateOf("spain") }
            LaunchedEffect(selectedCountryId) {
                com.example.gothere.notify.FcmTopicManager.syncRuleAlertTopic(this@MainActivity, selectedCountryId)
            }
            var onboardingDone by rememberSaveable {
                mutableStateOf(OnboardingPrefs.isCompleted(this@MainActivity))
            }

            // Paywall state
            var showPaywall by remember { mutableStateOf(false) }
            var paywallCountryId by remember { mutableStateOf("") }

            // Get purchased countries from PurchaseManager
            val purchasedCountries by purchaseManager.purchasedCountries.collectAsState()
            // Collected so the Upgrade button and Settings plan react to purchases.
            val subscriptionStatus by purchaseManager.subscriptionStatus.collectAsState()
            val ownedSKUs by purchaseManager.ownedSKUs.collectAsState()
            val isLegacy by purchaseManager.isLegacyPaidInstall.collectAsState()
            val promoUntil by purchaseManager.promoAccessUntil.collectAsState()
            val hasAllAccess = remember(purchasedCountries, subscriptionStatus, ownedSKUs, isLegacy, promoUntil) {
                purchaseManager.hasAllAccess()
            }

            val authRepo = AuthRepository()
            val user by authRepo.authStateFlow().collectAsState(initial = authRepo.currentUser())

            GoThereTheme(darkTheme = isDark) {
                if (!onboardingDone) {
                    OnboardingScreen(onFinish = {
                        OnboardingPrefs.markCompleted(this@MainActivity)
                        onboardingDone = true
                    })
                } else if (user == null) {
                    AuthScreen(
                        isDark = isDark,
                        onToggleTheme = { setAppearance(if (isDark) AppearancePrefs.LIGHT else AppearancePrefs.DARK) },
                        onAuthSuccess = {
                            // Restore purchases after login, and attach the Firestore
                            // entitlement listener (init only does it when already signed in).
                            purchaseManager.restorePurchases(userInitiated = false)
                            purchaseManager.onSignedIn()
                        }
                    )
                } else {
                    // Show paywall dialog if needed
                    if (showPaywall && paywallCountryId.isNotEmpty()) {
                        PaywallDialog(
                            countryId = paywallCountryId,
                            onDismiss = { 
                                showPaywall = false
                                paywallCountryId = ""
                            },
                            purchaseManager = purchaseManager
                        )
                    }
                    
                    MainAppContent(
                        appearance = appearance,
                        onAppearanceChange = setAppearance,
                        hasAllAccess = hasAllAccess,
                        accessReason = if (hasAllAccess) purchaseManager.accessReason() else null,
                        onRestorePurchases = { purchaseManager.restorePurchases() },
                        selectedCountryId = selectedCountryId,
                        purchasedCountries = purchasedCountries,
                        isCountryUnlocked = { purchaseManager.isCountryUnlocked(it) },
                        onRequestPaywall = { id ->
                            paywallCountryId = id
                            showPaywall = true
                        },
                        onCountryChange = { newId ->
                            // Check if country is unlocked
                            // isCountryUnlocked also honours the Portugal first-week trial.
                            if (purchaseManager.isCountryUnlocked(newId)) {
                                selectedCountryId = newId
                                if (BuildConfig.DEBUG) Log.d("CountrySwitch", "Switched to $newId")
                            } else {
                                // Show paywall
                                com.example.gothere.util.Analytics.paywallSource = "country_switch"
                                paywallCountryId = newId
                                showPaywall = true
                                if (BuildConfig.DEBUG) Log.d("CountrySwitch", "Country $newId is locked, showing paywall")
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppContent(
    appearance: String,
    onAppearanceChange: (String) -> Unit,
    hasAllAccess: Boolean,
    accessReason: Int?,
    onRestorePurchases: () -> Unit,
    selectedCountryId: String,
    purchasedCountries: Set<String>,
    isCountryUnlocked: (String) -> Boolean,
    onRequestPaywall: (String) -> Unit,
    onCountryChange: (String) -> Unit
) {
    val navController = rememberNavController()
    // Compose is one Activity, so PostHog's automatic $screen is always "MainActivity".
    // Report the route template instead (e.g. "planb_path/{pathId}", never the filled-in id).
    androidx.compose.runtime.DisposableEffect(navController) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
            destination.route?.let { com.example.gothere.util.Analytics.screen(it) }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
    var showCountryDropdown by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    val reauthMsg = stringResource(R.string.reauth_body)

    // Delete Account confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_account)) },
            text = {
                Text(stringResource(R.string.delete_account_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        val user = FirebaseAuth.getInstance().currentUser
                        // Delete the Auth account first. If Firebase needs a recent login this
                        // fails and nothing is lost. Once the account is gone, the onUserDeleted
                        // Cloud Function removes users/{uid} with every subcollection and Storage files.
                        user?.delete()?.addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                deleteError = reauthMsg
                            }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete_forever))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Error dialog for re-auth required
    if (deleteError != null) {
        AlertDialog(
            onDismissRequest = { deleteError = null },
            title = { Text(stringResource(R.string.reauth_required)) },
            text = { Text(deleteError!!) },
            confirmButton = {
                TextButton(onClick = {
                    deleteError = null
                    AuthRepository().signOut()
                }) {
                    Text(stringResource(R.string.sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteError = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Row(
                        modifier = Modifier
                            .clickable(
                                onClickLabel = stringResource(R.string.select_country)
                            ) { showCountryDropdown = true }
                            .padding(start = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getCountryFlag(selectedCountryId),
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(countryNameRes(selectedCountryId)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = stringResource(R.string.select_country),
                            tint = MaterialTheme.colorScheme.onSurface
                        )

                        DropdownMenu(
                            expanded = showCountryDropdown,
                            onDismissRequest = { showCountryDropdown = false }
                        ) {
                            allCountryIds.forEach { countryId ->
                                val isUnlocked = isCountryUnlocked(countryId)
                                
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = getCountryFlag(countryId),
                                                fontSize = 20.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(countryNameRes(countryId)),
                                                fontWeight = if (countryId == selectedCountryId)
                                                    FontWeight.Bold else FontWeight.Normal,
                                                color = if (isUnlocked) 
                                                    MaterialTheme.colorScheme.onSurface
                                                else 
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                            // Show lock icon for locked countries
                                            if (!isUnlocked) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = stringResource(R.string.locked),
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onCountryChange(countryId)
                                        showCountryDropdown = false
                                    }
                                )
                            }
                            if (!hasAllAccess) {
                                androidx.compose.material3.HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.unlock_all_countries), fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    onClick = {
                                        showCountryDropdown = false
                                        com.example.gothere.util.Analytics.paywallSource = "country_dropdown"
                                        onRequestPaywall("all")
                                    }
                                )
                            }
                        }
                    }
                },
                title = { LogoImage() },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    if (!hasAllAccess) {
                        TextButton(onClick = {
                            com.example.gothere.util.Analytics.paywallSource = "top_bar"
                            onRequestPaywall("all")
                        }) {
                            Icon(Icons.Outlined.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.upgrade), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(onClick = {
                        navController.navigate(Route.Settings.route) { launchSingleTop = true }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val tabItems = BottomTabs.map {
                GoTabItem(stringResource(it.label), stringResource(it.shortLabel), it.icon, it.selectedIcon)
            }
            GoTabBar(
                items = tabItems,
                selectedIndex = BottomTabs.indexOfFirst { it.route.route == currentRoute },
                onSelect = { index ->
                    // Settings is global, not part of any tab. Close it before
                    // switching so it is not saved into that tab's history and
                    // reopened the next time the tab is tapped.
                    if (currentRoute == Route.Settings.route) {
                        navController.popBackStack(Route.Settings.route, inclusive = true)
                    }
                    navController.navigate(BottomTabs[index].route.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        // Tab screens carry their own Scaffold/TopAppBar; consuming the insets here
        // stops them from adding the status-bar height a second time.
        // imePadding: with edge-to-edge, adjustResize no longer lifts content
        // above the keyboard, so text inputs near the bottom were hidden.
        Column(Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).imePadding()) {
            OfflineBanner()
            AppNavHost(
                navController = navController,
                selectedCountryId = selectedCountryId,
                onRequestPaywall = onRequestPaywall,
                modifier = Modifier.weight(1f),
                settings = {
                    SettingsScreen(
                        appearance = appearance,
                        onAppearanceChange = onAppearanceChange,
                        hasAllAccess = hasAllAccess,
                        accessReason = accessReason,
                        unlockedCount = allCountryIds.count { isCountryUnlocked(it) },
                        totalCountries = allCountryIds.size,
                        onSeePlans = {
                            com.example.gothere.util.Analytics.paywallSource = "settings"
                            onRequestPaywall("all")
                        },
                        onRestorePurchases = onRestorePurchases,
                        onOpenAI = { navController.navigate(Route.AIAssistant.route) },
                        onOpenScan = { navController.navigate(Route.DocumentScan.route) },
                        onOpenVisaWizard = { navController.navigate(Route.VisaWizard.create(selectedCountryId)) },
                        onOpenAncestry = { navController.navigate(Route.Ancestry.route) },
                        onSignOut = { AuthRepository().signOut() },
                        onDeleteAccount = { showDeleteConfirm = true },
                        appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                    )
                }
            )
        }
    }
}

/** Show the Plan B tab: pop back to it when it is already under us (checker opened from Plan B),
 *  otherwise switch tabs the same way the tab bar does. */
fun NavHostController.openPlanBTab() {
    if (popBackStack(Route.PlanB.route, inclusive = false)) return
    navigate(Route.PlanB.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun LogoImage() {
    Image(
        painter = painterResource(id = R.drawable.ic_logo),
        contentDescription = "GoThere",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
    )
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    selectedCountryId: String,
    onRequestPaywall: (String) -> Unit,
    modifier: Modifier = Modifier,
    settings: @Composable () -> Unit
) {
    // "Just keeping my options open" users land on Plan B. Read once: the start must not change.
    val context = LocalContext.current
    val startRoute = remember {
        if (com.example.gothere.planb.PlanBStore.get(context).isPlanBMode) Route.PlanB.route else Route.Tasks.route
    }
    NavHost(
        navController = navController,
        startDestination = startRoute,
        modifier = modifier
    ) {
        composable(Route.Tasks.route) {
            TasksScreen(countryId = selectedCountryId, navController = navController)
        }
        composable(Route.Calendar.route) { CalendarScreen() }
        composable(Route.Documents.route) { DocumentsScreen() }
        composable(Route.Resources.route) {
            ResourcesScreen(countryId = selectedCountryId, onOpenPlanB = { navController.openPlanBTab() })
        }
        composable(Route.PlanB.route) {
            PlanBScreen(
                onOpenAncestry = { navController.navigate(Route.Ancestry.route) },
                onOpenPath = { navController.navigate(Route.PlanBPath.create(it)) }
            )
        }
        composable(Route.PlanBPath.route) { backStackEntry ->
            PlanBPathDetailScreen(
                pathId = backStackEntry.arguments?.getString("pathId").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
        composable(Route.DecisionTree.route) {
            DecisionTreeScreen(
                navController = navController,
                countryId = selectedCountryId
            )
        }
        composable(Route.Settings.route) { settings() }
        composable(Route.Ancestry.route) {
            AncestryCheckerScreen(onBack = { navController.popBackStack() }, onOpenPlanB = { navController.openPlanBTab() })
        }
        composable(Route.AIAssistant.route) {
            AIAssistantScreen(
                onRequestUpgrade = {
                    com.example.gothere.util.Analytics.paywallSource = "ai_limit"
                    onRequestPaywall(selectedCountryId)
                }
            )
        }
        composable(Route.DocumentScan.route) {
            DocumentScanScreen(
                countryId = selectedCountryId,
                onRequestUpgrade = {
                    com.example.gothere.util.Analytics.paywallSource = "document_scan_limit"
                    onRequestPaywall(selectedCountryId)
                }
            )
        }
        composable(Route.VisaWizard.route) { backStackEntry ->
            VisaWizardScreen(
                navController = navController,
                countryId = backStackEntry.arguments?.getString("countryId") ?: selectedCountryId
            )
        }
        composable(Route.CityMap.route) { backStackEntry ->
            CityMapScreen(
                navController = navController,
                cityId = backStackEntry.arguments?.getString("cityId") ?: "madrid",
                countryId = backStackEntry.arguments?.getString("countryId") ?: selectedCountryId
            )
        }
    }
}
