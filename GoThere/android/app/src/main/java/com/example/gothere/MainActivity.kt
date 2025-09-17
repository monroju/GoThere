package com.example.gothere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gothere.ui.CalendarScreen
import com.example.gothere.ui.DocumentsScreen
import com.example.gothere.ui.NotesScreen
import com.example.gothere.ui.ResourcesScreen
import com.example.gothere.ui.TasksScreen
import com.example.gothere.ui.theme.GoThereTheme

sealed class Route(val route: String) {
    data object Tasks     : Route("tasks")
    data object Calendar  : Route("calendar")
    data object Documents : Route("documents")
    data object Resources : Route("resources")
    data object Notes     : Route("notes")
}

data class BottomTab(
    val route: Route,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val BottomTabs = listOf(
    BottomTab(Route.Tasks,     "Tasks",     Icons.Outlined.ListAlt),
    BottomTab(Route.Calendar,  "Calendar",  Icons.Outlined.CalendarMonth),
    BottomTab(Route.Documents, "Documents", Icons.Outlined.Folder),
    BottomTab(Route.Resources, "Resources", Icons.Outlined.Description),
    BottomTab(Route.Notes,     "Notes",     Icons.Outlined.EditNote)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoThereTheme {
                val navController = rememberNavController()

                Scaffold(
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        NavigationBar {
                            BottomTabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab.route.route,
                                    onClick = {
                                        navController.navigate(tab.route.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
                    NavHost(
                        navController = navController,
                        startDestination = Route.Tasks.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Route.Tasks.route)     { TasksScreen() }
                        composable(Route.Calendar.route)  { CalendarScreen() }
                        composable(Route.Documents.route) { DocumentsScreen() }
                        composable(Route.Resources.route) { ResourcesScreen() }
                        composable(Route.Notes.route)     { NotesScreen() }
                    }
                }
            }
        }
    }
}
