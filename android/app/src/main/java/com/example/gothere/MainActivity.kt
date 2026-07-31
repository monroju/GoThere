package com.example.gothere

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gothere.auth.AuthRepository
import com.example.gothere.billing.PurchaseManager
import com.example.gothere.ui.AIAssistantScreen
import com.example.gothere.ui.CalendarScreen
import com.example.gothere.ui.AncestryCheckerScreen
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
private fun getCountryName(id: String): String = when (id) {
    "spain" -> "Spain"
    "portugal" -> "Portugal"
    "mexico" -> "Mexico"
    "canada" -> "Canada"
    "ireland" -> "Ireland"
    "italy" -> "Italy"
    "germany" -> "Germany"
    "poland" -> "Poland"
    "argentina" -> "Argentina"
    "hungary" -> "Hungary"
    "uk_ancestry" -> "UK (Ancestry)"
    else -> "Spain"
}

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
    data object AIAssistant : Route("ai_assistant")
    data object DocumentScan : Route("document_scan")
    data object VisaWizard : Route("visa_wizard/{countryId}") {
        fun create(countryId: String) = "visa_wizard/$countryId"
    }
    data object CityMap : Route("city_map/{countryId}/{cityId}") {
        fun create(countryId: String, cityId: String) = "city_map/$countryId/$cityId"
    }
}

data class BottomTab(
    val route: Route,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val BottomTabs = listOf(
    BottomTab(Route.Tasks, "Tasks", Icons.AutoMirrored.Outlined.ListAlt),
    BottomTab(Route.Calendar, "Calendar", Icons.Outlined.CalendarMonth),
    BottomTab(Route.Documents, "Documents", Icons.Outlined.Folder),
    BottomTab(Route.Resources, "Resources", Icons.Outlined.Description),
    BottomTab(Route.DecisionTree, "Decision Tree", Icons.Outlined.AccountTree),
)

class MainActivity : ComponentActivity() {

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
            )
        )

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
            var isDark by rememberSaveable { mutableStateOf(true) }
            var selectedCountryId by rememberSaveable { mutableStateOf("spain") }
            var onboardingDone by rememberSaveable {
                mutableStateOf(OnboardingPrefs.isCompleted(this@MainActivity))
            }

            // Paywall state
            var showPaywall by remember { mutableStateOf(false) }
            var paywallCountryId by remember { mutableStateOf("") }

            // Get purchased countries from PurchaseManager
            val purchasedCountries by purchaseManager.purchasedCountries.collectAsState()

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
                        onToggleTheme = { isDark = !isDark },
                        onAuthSuccess = {
                            // Restore purchases after login
                            purchaseManager.restorePurchases()
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
                        isDark = isDark,
                        onToggleTheme = { isDark = !isDark },
                        selectedCountryId = selectedCountryId,
                        purchasedCountries = purchasedCountries,
                        onRequestPaywall = { id ->
                            paywallCountryId = id
                            showPaywall = true
                        },
                        onCountryChange = { newId ->
                            // Check if country is unlocked
                            if (purchasedCountries.contains(newId)) {
                                selectedCountryId = newId
                                if (BuildConfig.DEBUG) Log.d("CountrySwitch", "Switched to $newId")
                            } else {
                                // Show paywall
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
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    selectedCountryId: String,
    purchasedCountries: Set<String>,
    onRequestPaywall: (String) -> Unit,
    onCountryChange: (String) -> Unit
) {
    val navController = rememberNavController()
    var showCountryDropdown by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    // Delete Account confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account") },
            text = {
                Text("This will permanently delete your account and all your data (tasks, events, documents). This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        val user = FirebaseAuth.getInstance().currentUser
                        val uid = user?.uid
                        if (uid != null) {
                            val db = FirebaseFirestore.getInstance()
                            val userDoc = db.collection("users").document(uid)
                            // Delete subcollections then user doc then auth account
                            val subcollections = listOf("tasks", "events", "documents", "forms", "profiles", "recommendations")
                            val batch = db.batch()
                            val pending = java.util.concurrent.atomic.AtomicInteger(subcollections.size)
                            for (sub in subcollections) {
                                userDoc.collection(sub).get()
                                    .addOnSuccessListener { snap ->
                                        for (doc in snap.documents) {
                                            doc.reference.delete()
                                        }
                                        if (pending.decrementAndGet() == 0) {
                                            // All subcollections cleared, now delete user doc + auth
                                            userDoc.delete().addOnCompleteListener {
                                                user.delete().addOnCompleteListener { task ->
                                                    if (!task.isSuccessful) {
                                                        deleteError = "Please sign out, sign back in, and try again."
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        if (pending.decrementAndGet() == 0) {
                                            userDoc.delete().addOnCompleteListener {
                                                user.delete().addOnCompleteListener { task ->
                                                    if (!task.isSuccessful) {
                                                        deleteError = "Please sign out, sign back in, and try again."
                                                    }
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error dialog for re-auth required
    if (deleteError != null) {
        AlertDialog(
            onDismissRequest = { deleteError = null },
            title = { Text("Re-authentication Required") },
            text = { Text(deleteError!!) },
            confirmButton = {
                TextButton(onClick = {
                    deleteError = null
                    AuthRepository().signOut()
                }) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteError = null }) {
                    Text("Cancel")
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
                                onClickLabel = "Select country"
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
                            text = getCountryName(selectedCountryId),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Select country",
                            tint = MaterialTheme.colorScheme.onSurface
                        )

                        DropdownMenu(
                            expanded = showCountryDropdown,
                            onDismissRequest = { showCountryDropdown = false }
                        ) {
                            allCountryIds.forEach { countryId ->
                                val isUnlocked = purchasedCountries.contains(countryId)
                                
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = getCountryFlag(countryId),
                                                fontSize = 20.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = getCountryName(countryId),
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
                                                    contentDescription = "Locked",
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
                        }
                    }
                },
                title = { LogoImage() },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "Toggle theme"
                        )
                    }
                    // Settings overflow menu
                    IconButton(onClick = { showSettingsMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    }
                    DropdownMenu(
                        expanded = showSettingsMenu,
                        onDismissRequest = { showSettingsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Where to start (AI)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showSettingsMenu = false
                                navController.navigate(Route.AIAssistant.route)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Scan a Document") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showSettingsMenu = false
                                navController.navigate(Route.DocumentScan.route)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Visa Wizard") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showSettingsMenu = false
                                navController.navigate(Route.VisaWizard.create(selectedCountryId))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ancestry Citizenship") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.AccountTree,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showSettingsMenu = false
                                navController.navigate(Route.Ancestry.route)
                            }
                        )
                        androidx.compose.material3.HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Sign Out") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showSettingsMenu = false
                                AuthRepository().signOut()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete Account",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showSettingsMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavigationBar {
                BottomTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route.route,
                        onClick = {
                            navController.navigate(tab.route.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            OfflineBanner()
            AppNavHost(
                navController = navController,
                selectedCountryId = selectedCountryId,
                onRequestPaywall = onRequestPaywall,
                modifier = Modifier.weight(1f)
            )
        }
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
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Tasks.route,
        modifier = modifier
    ) {
        composable(Route.Tasks.route) {
            TasksScreen(countryId = selectedCountryId, navController = navController)
        }
        composable(Route.Calendar.route) { CalendarScreen() }
        composable(Route.Documents.route) { DocumentsScreen() }
        composable(Route.Resources.route) { ResourcesScreen(countryId = selectedCountryId) }
        composable(Route.DecisionTree.route) {
            DecisionTreeScreen(
                navController = navController,
                countryId = selectedCountryId
            )
        }
        composable(Route.Ancestry.route) {
            AncestryCheckerScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.AIAssistant.route) {
            AIAssistantScreen(
                onRequestUpgrade = { onRequestPaywall(selectedCountryId) }
            )
        }
        composable(Route.DocumentScan.route) {
            DocumentScanScreen(
                countryId = selectedCountryId,
                onRequestUpgrade = { onRequestPaywall(selectedCountryId) }
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
