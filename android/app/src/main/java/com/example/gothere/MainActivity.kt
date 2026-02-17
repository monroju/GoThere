package com.example.gothere

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.gothere.BuildConfig
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LightMode
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
import com.example.gothere.ui.CalendarScreen
import com.example.gothere.ui.DecisionTreeScreen
import com.example.gothere.ui.DocumentsScreen
import com.example.gothere.ui.PaywallDialog
import com.example.gothere.ui.ResourcesScreen
import com.example.gothere.ui.TasksScreen
import com.example.gothere.ui.theme.AuthScreen
import com.example.gothere.ui.theme.GoThereTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Simple helper functions for country data
private fun getCountryName(id: String): String = when (id) {
    "spain" -> "Spain"
    "portugal" -> "Portugal"
    "mexico" -> "Mexico"
    else -> "Spain"
}

private fun getCountryFlag(id: String): String = when (id) {
    "spain" -> "🇪🇸"
    "portugal" -> "🇵🇹"
    "mexico" -> "🇲🇽"
    else -> "🇪🇸"
}

private val allCountryIds = listOf("spain", "portugal", "mexico")

sealed class Route(val route: String) {
    data object Tasks : Route("tasks")
    data object Calendar : Route("calendar")
    data object Documents : Route("documents")
    data object Resources : Route("resources")
    data object DecisionTree : Route("decision_tree")
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
        super.onCreate(savedInstanceState)

        // Initialize purchase manager
        purchaseManager = PurchaseManager.getInstance(this)

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

        setContent {
            var isDark by rememberSaveable { mutableStateOf(true) }
            var selectedCountryId by rememberSaveable { mutableStateOf("spain") }
            
            // Paywall state
            var showPaywall by remember { mutableStateOf(false) }
            var paywallCountryId by remember { mutableStateOf("") }
            
            // Get purchased countries from PurchaseManager
            val purchasedCountries by purchaseManager.purchasedCountries.collectAsState()

            val authRepo = AuthRepository()
            val user by authRepo.authStateFlow().collectAsState(initial = authRepo.currentUser())

            GoThereTheme(darkTheme = isDark) {
                if (user == null) {
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
                        onCountryChange = { newId ->
                            // Check if country is unlocked
                            if (purchasedCountries.contains(newId)) {
                                selectedCountryId = newId
                                Log.d("CountrySwitch", "Switched to $newId")
                            } else {
                                // Show paywall
                                paywallCountryId = newId
                                showPaywall = true
                                Log.d("CountrySwitch", "Country $newId is locked, showing paywall")
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
    onCountryChange: (String) -> Unit
) {
    val navController = rememberNavController()
    var showCountryDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Row(
                        modifier = Modifier
                            .clickable { showCountryDropdown = true }
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
        AppNavHost(
            navController = navController,
            selectedCountryId = selectedCountryId,
            modifier = Modifier.padding(innerPadding)
        )
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
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Tasks.route,
        modifier = modifier
    ) {
        composable(Route.Tasks.route) { 
            TasksScreen(countryId = selectedCountryId) 
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
    }
}
