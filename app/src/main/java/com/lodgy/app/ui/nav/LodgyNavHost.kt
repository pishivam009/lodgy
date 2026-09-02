package com.lodgy.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lodgy.app.ui.property.HostelFormScreen
import com.lodgy.app.ui.property.HostelListScreen
import com.lodgy.app.ui.screens.PlaceholderScreen

private const val HOSTEL_FORM_ROUTE = "hostel_form"

@Composable
fun LodgyNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevelDestination = LodgyDestination.entries.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar {
                    LodgyDestination.entries.forEach { destination ->
                        val label = stringResource(destination.labelRes)
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LodgyDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            LodgyDestination.entries.forEach { destination ->
                composable(destination.route) {
                    if (destination == LodgyDestination.Property) {
                        HostelListScreen(
                            onAddHostel = { navController.navigate(HOSTEL_FORM_ROUTE) },
                            onEditHostel = { id -> navController.navigate("$HOSTEL_FORM_ROUTE?hostelId=$id") },
                        )
                    } else {
                        PlaceholderScreen(title = stringResource(destination.labelRes))
                    }
                }
            }

            composable(
                route = "$HOSTEL_FORM_ROUTE?hostelId={hostelId}",
                arguments = listOf(navArgument("hostelId") { type = NavType.StringType; nullable = true }),
            ) {
                HostelFormScreen(
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
