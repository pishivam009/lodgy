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
import com.lodgy.app.ui.property.FloorFormScreen
import com.lodgy.app.ui.property.FloorListScreen
import com.lodgy.app.ui.property.HostelFormScreen
import com.lodgy.app.ui.property.HostelListScreen
import com.lodgy.app.ui.property.BedGridScreen
import com.lodgy.app.ui.property.RoomFormScreen
import com.lodgy.app.ui.property.RoomListScreen
import com.lodgy.app.ui.dashboard.DashboardScreen
import com.lodgy.app.ui.dashboard.VacantViewScreen
import com.lodgy.app.ui.payment.InvoiceListScreen
import com.lodgy.app.ui.payment.ManualInvoiceFormScreen
import com.lodgy.app.ui.payment.ManualInvoiceTenantPickerScreen
import com.lodgy.app.ui.payment.RecordPaymentScreen
import com.lodgy.app.ui.payment.ReminderScreen
import com.lodgy.app.ui.screens.PlaceholderScreen
import com.lodgy.app.ui.tenant.AgreementFormScreen
import com.lodgy.app.ui.tenant.BedPickerScreen
import com.lodgy.app.ui.tenant.CheckoutScreen
import com.lodgy.app.ui.tenant.TenantDirectoryScreen
import com.lodgy.app.ui.tenant.TenantFormScreen
import com.lodgy.app.ui.tenant.TenantProfileScreen

private const val HOSTEL_FORM_ROUTE = "hostel_form"
private const val FLOOR_LIST_ROUTE = "floor_list"
private const val FLOOR_FORM_ROUTE = "floor_form"
private const val ROOM_LIST_ROUTE = "room_list"
private const val ROOM_FORM_ROUTE = "room_form"
private const val BED_GRID_ROUTE = "bed_grid"
private const val BED_PICKER_ROUTE = "bed_picker"
private const val TENANT_FORM_ROUTE = "tenant_form"
private const val TENANT_PROFILE_ROUTE = "tenant_profile"
private const val AGREEMENT_FORM_ROUTE = "agreement_form"
private const val CHECKOUT_ROUTE = "checkout"
private const val RECORD_PAYMENT_ROUTE = "record_payment"
private const val MANUAL_INVOICE_TENANT_PICKER_ROUTE = "manual_invoice_tenant_picker"
private const val MANUAL_INVOICE_FORM_ROUTE = "manual_invoice_form"
private const val REMINDER_ROUTE = "reminder"
private const val VACANT_VIEW_ROUTE = "vacant_view"

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
                    when (destination) {
                        LodgyDestination.Property -> HostelListScreen(
                            onAddHostel = { navController.navigate(HOSTEL_FORM_ROUTE) },
                            onEditHostel = { id -> navController.navigate("$HOSTEL_FORM_ROUTE?hostelId=$id") },
                            onOpenFloors = { hostelId -> navController.navigate("$FLOOR_LIST_ROUTE/$hostelId") },
                        )
                        LodgyDestination.Tenants -> TenantDirectoryScreen(
                            onAddTenant = { navController.navigate(BED_PICKER_ROUTE) },
                            onOpenTenant = { tenant -> navController.navigate("$TENANT_PROFILE_ROUTE/${tenant.id}") },
                        )
                        LodgyDestination.Payments -> InvoiceListScreen(
                            onRecordPayment = { invoice -> navController.navigate("$RECORD_PAYMENT_ROUTE/${invoice.id}") },
                            onSendReminder = { invoice -> navController.navigate("$REMINDER_ROUTE/${invoice.id}") },
                            onAddManualInvoice = { navController.navigate(MANUAL_INVOICE_TENANT_PICKER_ROUTE) },
                        )
                        LodgyDestination.Home -> DashboardScreen(
                            onOpenVacantBeds = { navController.navigate(VACANT_VIEW_ROUTE) },
                        )
                        else -> PlaceholderScreen(title = stringResource(destination.labelRes))
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

            composable(
                route = "$FLOOR_LIST_ROUTE/{hostelId}",
                arguments = listOf(navArgument("hostelId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val hostelId = checkNotNull(backStackEntry.arguments?.getString("hostelId"))
                FloorListScreen(
                    onBack = { navController.popBackStack() },
                    onAddFloor = { navController.navigate("$FLOOR_FORM_ROUTE/$hostelId") },
                    onEditFloor = { floor -> navController.navigate("$FLOOR_FORM_ROUTE/$hostelId?floorId=${floor.id}") },
                    onOpenRooms = { floor -> navController.navigate("$ROOM_LIST_ROUTE/${floor.id}") },
                )
            }

            composable(
                route = "$FLOOR_FORM_ROUTE/{hostelId}?floorId={floorId}",
                arguments = listOf(
                    navArgument("hostelId") { type = NavType.StringType },
                    navArgument("floorId") { type = NavType.StringType; nullable = true },
                ),
            ) {
                FloorFormScreen(
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "$ROOM_LIST_ROUTE/{floorId}",
                arguments = listOf(navArgument("floorId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val floorId = checkNotNull(backStackEntry.arguments?.getString("floorId"))
                RoomListScreen(
                    onBack = { navController.popBackStack() },
                    onAddRoom = { navController.navigate("$ROOM_FORM_ROUTE/$floorId") },
                    onEditRoom = { room -> navController.navigate("$ROOM_FORM_ROUTE/$floorId?roomId=${room.id}") },
                    onOpenBeds = { room -> navController.navigate("$BED_GRID_ROUTE/${room.id}") },
                )
            }

            composable(
                route = "$ROOM_FORM_ROUTE/{floorId}?roomId={roomId}",
                arguments = listOf(
                    navArgument("floorId") { type = NavType.StringType },
                    navArgument("roomId") { type = NavType.StringType; nullable = true },
                ),
            ) {
                RoomFormScreen(
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "$BED_GRID_ROUTE/{roomId}",
                arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
            ) {
                BedGridScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = "$TENANT_FORM_ROUTE?tenantId={tenantId}&bedId={bedId}",
                arguments = listOf(
                    navArgument("tenantId") { type = NavType.StringType; nullable = true },
                    navArgument("bedId") { type = NavType.StringType; nullable = true },
                ),
            ) { backStackEntry ->
                val bedId = backStackEntry.arguments?.getString("bedId")
                TenantFormScreen(
                    onDone = { tenantId ->
                        if (bedId != null) {
                            navController.navigate("$AGREEMENT_FORM_ROUTE/$bedId/$tenantId")
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "$TENANT_PROFILE_ROUTE/{tenantId}",
                arguments = listOf(navArgument("tenantId") { type = NavType.StringType }),
            ) {
                TenantProfileScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { tenantId -> navController.navigate("$TENANT_FORM_ROUTE?tenantId=$tenantId") },
                    onCheckout = { tenantId -> navController.navigate("$CHECKOUT_ROUTE/$tenantId") },
                )
            }

            composable(
                route = "$CHECKOUT_ROUTE/{tenantId}",
                arguments = listOf(navArgument("tenantId") { type = NavType.StringType }),
            ) {
                CheckoutScreen(
                    onDone = { navController.popBackStack(LodgyDestination.Tenants.route, false) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(BED_PICKER_ROUTE) {
                BedPickerScreen(
                    onBack = { navController.popBackStack() },
                    onBedSelected = { option -> navController.navigate("$TENANT_FORM_ROUTE?bedId=${option.bed.id}") },
                )
            }

            composable(
                route = "$AGREEMENT_FORM_ROUTE/{bedId}/{tenantId}",
                arguments = listOf(
                    navArgument("bedId") { type = NavType.StringType },
                    navArgument("tenantId") { type = NavType.StringType },
                ),
            ) {
                AgreementFormScreen(
                    onDone = { navController.popBackStack(LodgyDestination.Tenants.route, false) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "$RECORD_PAYMENT_ROUTE/{invoiceId}",
                arguments = listOf(navArgument("invoiceId") { type = NavType.StringType }),
            ) {
                RecordPaymentScreen(
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(MANUAL_INVOICE_TENANT_PICKER_ROUTE) {
                ManualInvoiceTenantPickerScreen(
                    onBack = { navController.popBackStack() },
                    onTenantSelected = { tenant -> navController.navigate("$MANUAL_INVOICE_FORM_ROUTE/${tenant.id}") },
                )
            }

            composable(
                route = "$MANUAL_INVOICE_FORM_ROUTE/{tenantId}",
                arguments = listOf(navArgument("tenantId") { type = NavType.StringType }),
            ) {
                ManualInvoiceFormScreen(
                    onDone = { navController.popBackStack(LodgyDestination.Payments.route, false) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "$REMINDER_ROUTE/{invoiceId}",
                arguments = listOf(navArgument("invoiceId") { type = NavType.StringType }),
            ) {
                ReminderScreen(onBack = { navController.popBackStack() })
            }

            composable(VACANT_VIEW_ROUTE) {
                VacantViewScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
