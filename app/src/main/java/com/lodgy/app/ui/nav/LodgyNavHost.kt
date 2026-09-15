package com.lodgy.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.lodgy.app.notify.ROUTE_EXPENSE_FORM
import com.lodgy.app.notify.isSupportedNotificationRoute
import com.lodgy.app.notify.ROUTE_RECORD_PAYMENT
import com.lodgy.app.notify.ROUTE_VACANT_VIEW
import com.lodgy.app.ui.property.AllRoomsScreen
import com.lodgy.app.ui.property.BulkRoomFormScreen
import com.lodgy.app.ui.property.FloorFormScreen
import com.lodgy.app.ui.property.FloorListScreen
import com.lodgy.app.ui.property.HostelFormScreen
import com.lodgy.app.ui.property.HostelListScreen
import com.lodgy.app.ui.property.BedGridScreen
import com.lodgy.app.ui.property.RoomFormScreen
import com.lodgy.app.ui.property.RoomListScreen
import com.lodgy.app.ui.backup.BackupScreen
import com.lodgy.app.ui.backup.DataPacketScreen
import com.lodgy.app.ui.backup.HistoryImportScreen
import com.lodgy.app.ui.backup.StayBackfillScreen
import com.lodgy.app.ui.dashboard.DashboardScreen
import com.lodgy.app.ui.dashboard.MonthlyReportScreen
import com.lodgy.app.ui.expense.ExpenseFormScreen
import com.lodgy.app.ui.expense.ExpenseListScreen
import com.lodgy.app.ui.more.MoreScreen
import com.lodgy.app.ui.more.NotificationSettingsScreen
import com.lodgy.app.ui.note.NoteFormScreen
import com.lodgy.app.ui.note.NotesTimelineScreen
import com.lodgy.app.ui.payment.AcknowledgementScreen
import com.lodgy.app.ui.payment.CreditFormScreen
import com.lodgy.app.ui.payment.InvoiceListScreen
import com.lodgy.app.ui.payment.ManualInvoiceFormScreen
import com.lodgy.app.ui.payment.ManualInvoiceTenantPickerScreen
import com.lodgy.app.ui.payment.MultiPeriodPaymentScreen
import com.lodgy.app.ui.payment.RecordPaymentScreen
import com.lodgy.app.ui.payment.ReminderScreen
import com.lodgy.app.ui.tenant.AgreementFormScreen
import com.lodgy.app.ui.tenant.BedPickerScreen
import com.lodgy.app.ui.tenant.CheckoutScreen
import com.lodgy.app.ui.tenant.ForgoneRentScreen
import com.lodgy.app.ui.tenant.TenantDirectoryScreen
import com.lodgy.app.ui.tenant.TenantFormScreen
import com.lodgy.app.ui.tenant.TenantProfileScreen
import com.lodgy.app.ui.tenant.TransferScreen

private const val HOSTEL_FORM_ROUTE = "hostel_form"
private const val FLOOR_LIST_ROUTE = "floor_list"
private const val FLOOR_FORM_ROUTE = "floor_form"
private const val ROOM_LIST_ROUTE = "room_list"
private const val ROOM_FORM_ROUTE = "room_form"
private const val BULK_ROOM_FORM_ROUTE = "bulk_room_form"
private const val ALL_ROOMS_ROUTE = "all_rooms"
private const val BED_GRID_ROUTE = "bed_grid"
private const val BED_PICKER_ROUTE = "bed_picker"
private const val TENANT_FORM_ROUTE = "tenant_form"
private const val TENANT_FORM_ROUTE_PATTERN =
    "$TENANT_FORM_ROUTE?tenantId={tenantId}&bedId={bedId}"
private const val TENANT_PROFILE_ROUTE = "tenant_profile"
private const val AGREEMENT_FORM_ROUTE = "agreement_form"
private const val CHECKOUT_ROUTE = "checkout"
private const val TRANSFER_ROUTE = "transfer"
private const val FORGONE_RENT_ROUTE = "forgone_rent"
private const val CREDIT_FORM_ROUTE = "credit_form"
private const val ACKNOWLEDGEMENT_ROUTE = "acknowledgement"
private const val MULTI_PERIOD_PAYMENT_ROUTE = "multi_period_payment"
private const val RECORD_PAYMENT_ROUTE = ROUTE_RECORD_PAYMENT
private const val MANUAL_INVOICE_TENANT_PICKER_ROUTE = "manual_invoice_tenant_picker"
private const val MANUAL_INVOICE_FORM_ROUTE = "manual_invoice_form"
private const val REMINDER_ROUTE = "reminder"
private const val VACANT_VIEW_ROUTE = ROUTE_VACANT_VIEW
private const val INVOICE_LIST_ROUTE = "invoice_list"
private const val MONTHLY_REPORT_ROUTE = "monthly_report"
private const val EXPENSE_LIST_ROUTE = "expense_list"
private const val EXPENSE_FORM_ROUTE = ROUTE_EXPENSE_FORM
private const val NOTES_TIMELINE_ROUTE = "notes_timeline"
private const val NOTE_FORM_ROUTE = "note_form"
private const val BACKUP_ROUTE = "backup"
private const val DATA_PACKET_ROUTE = "data_packet"
private const val HISTORY_IMPORT_ROUTE = "history_import"
private const val STAY_BACKFILL_ROUTE = "stay_backfill"
private const val NOTIFICATION_SETTINGS_ROUTE = "notification_settings"

@Composable
fun LodgyNavHost(pendingRoute: String? = null) {
    val navController = rememberNavController()

    // A notification's destination, opened once the warden is through the lock screen. Keyed on
    // the route so a second notification during the same session still lands.
    LaunchedEffect(pendingRoute) {
        pendingRoute?.takeIf(::isSupportedNotificationRoute)?.let(navController::navigate)
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        // The bottom navigation stays on every screen, not just the five top-level tabs, so Home is
        // always one tap away and the one persistent landmark never vanishes when a warden is deep
        // in the app and lost. Older wardens could not find the bare back chevron or a way home
        // (LODGY-80). It lives here on the Scaffold, outside the NavHost, so it stays put while inner
        // content slides (LODGY-61); the chevron and system back are untouched - this adds a route
        // home, it does not replace going back.
        bottomBar = {
            NavigationBar {
                LodgyDestination.entries.forEach { destination ->
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                        // On an inner screen no tab is the current one, so nothing is highlighted -
                        // the bar is a way out, not a claim about where you are.
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            // Land on the tab's ROOT, clearing any inner screens in the way - so
                            // Home always shows the dashboard, even from a Home-section inner screen
                            // like the monthly report or the vacant-beds view (LODGY-80, AC2). The
                            // multi-back-stack saveState/restoreState was dropped deliberately: it
                            // restored the current section's saved sub-position, which stranded a
                            // warden on that inner screen instead of taking them out. "Tap Home = the
                            // home screen" is what these users expect, and reaching the section root
                            // matters more than remembering a scroll position.
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LodgyDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = pushEnter,
            exitTransition = pushExit,
            popEnterTransition = popEnter,
            popExitTransition = popExit,
        ) {
            LodgyDestination.entries.forEach { destination ->
                composable(
                    route = destination.route,
                    enterTransition = tabEnter,
                    exitTransition = tabExit,
                    popEnterTransition = tabEnter,
                    popExitTransition = tabExit,
                ) {
                    when (destination) {
                        LodgyDestination.Property -> HostelListScreen(
                            onAddHostel = { navController.navigate(HOSTEL_FORM_ROUTE) },
                            onEditHostel = { id -> navController.navigate("$HOSTEL_FORM_ROUTE?hostelId=$id") },
                            onOpenFloors = { hostelId -> navController.navigate("$FLOOR_LIST_ROUTE/$hostelId") },
                            onOpenUnit = { roomId -> navController.navigate("$BED_GRID_ROUTE/$roomId") },
                            onOpenAllRooms = { navController.navigate(ALL_ROOMS_ROUTE) },
                        )
                        LodgyDestination.Tenants -> TenantDirectoryScreen(
                            onAddTenant = { navController.navigate(BED_PICKER_ROUTE) },
                            onOpenTenant = { tenant -> navController.navigate("$TENANT_PROFILE_ROUTE/${tenant.id}") },
                        )
                        LodgyDestination.Payments -> InvoiceListScreen(
                            onRecordPayment = { invoice -> navController.navigate("$RECORD_PAYMENT_ROUTE/${invoice.id}") },
                            onSendReminder = { invoice -> navController.navigate("$REMINDER_ROUTE/${invoice.id}") },
                            onOpenReceipt = { invoice -> navController.navigate("$ACKNOWLEDGEMENT_ROUTE/${invoice.id}") },
                            onAddManualInvoice = { navController.navigate(MANUAL_INVOICE_TENANT_PICKER_ROUTE) },
                        )
                        LodgyDestination.Home -> DashboardScreen(
                            onOpenVacantBeds = { hostelId ->
                                // Carries Home's hostel filter, so the tile's number and the list
                                // it opens describe the same properties (LODGY-89, AC6).
                                val scope = hostelId?.let { "&hostelId=$it" }.orEmpty()
                                navController.navigate("$ALL_ROOMS_ROUTE?hasSpace=true$scope")
                            },
                            onOpenOverdue = { hostelId ->
                                val scope = hostelId?.let { "&hostelId=$it" }.orEmpty()
                                navController.navigate("$INVOICE_LIST_ROUTE?filter=OVERDUE$scope")
                            },
                            onOpenMonthlyReport = { navController.navigate(MONTHLY_REPORT_ROUTE) },
                            onOpenBackup = { navController.navigate(BACKUP_ROUTE) },
                        )
                        LodgyDestination.More -> MoreScreen(
                            onOpenExpenses = { navController.navigate(EXPENSE_LIST_ROUTE) },
                            onOpenBackup = { navController.navigate(BACKUP_ROUTE) },
                            onOpenPrintableRecords = { navController.navigate(DATA_PACKET_ROUTE) },
                            onOpenHistoryImport = { navController.navigate(HISTORY_IMPORT_ROUTE) },
                            onOpenStayBackfill = { navController.navigate(STAY_BACKFILL_ROUTE) },
                            onOpenNotificationSettings = { navController.navigate(NOTIFICATION_SETTINGS_ROUTE) },
                        )
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
                    onOpenAllRooms = { navController.navigate("$ALL_ROOMS_ROUTE?hostelId=$hostelId") },
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
                    onBulkAddRooms = { navController.navigate("$BULK_ROOM_FORM_ROUTE/$floorId") },
                    onEditRoom = { room -> navController.navigate("$ROOM_FORM_ROUTE/$floorId?roomId=${room.id}") },
                    onOpenBeds = { room -> navController.navigate("$BED_GRID_ROUTE/${room.id}") },
                )
            }

            composable(
                route = "$ALL_ROOMS_ROUTE?hostelId={hostelId}&hasSpace={hasSpace}",
                arguments = listOf(
                    navArgument("hostelId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("hasSpace") { type = NavType.BoolType; defaultValue = false },
                ),
            ) {
                AllRoomsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRoom = { roomId -> navController.navigate("$BED_GRID_ROUTE/$roomId") },
                )
            }

            composable(
                route = "$BULK_ROOM_FORM_ROUTE/{floorId}",
                arguments = listOf(navArgument("floorId") { type = NavType.StringType }),
            ) {
                BulkRoomFormScreen(
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
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
                BedGridScreen(
                    onBack = { navController.popBackStack() },
                    // Skips the bed picker: the warden already chose the bed by tapping it.
                    onAssignTenant = { bedId -> navController.navigate("$TENANT_FORM_ROUTE?bedId=$bedId") },
                    onViewTenant = { tenantId -> navController.navigate("$TENANT_PROFILE_ROUTE/$tenantId") },
                )
            }

            composable(
                route = TENANT_FORM_ROUTE_PATTERN,
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
                    onTransfer = { tenantId -> navController.navigate("$TRANSFER_ROUTE/$tenantId") },
                    onForgoneRent = { tenantId -> navController.navigate("$FORGONE_RENT_ROUTE/$tenantId") },
                    onRecordCredit = { tenantId -> navController.navigate("$CREDIT_FORM_ROUTE/$tenantId") },
                    onPaySeveralMonths = { tenantId -> navController.navigate("$MULTI_PERIOD_PAYMENT_ROUTE/$tenantId") },
                    onOpenNotes = { tenantId -> navController.navigate("$NOTES_TIMELINE_ROUTE/$tenantId") },
                )
            }

            composable(
                route = "$NOTES_TIMELINE_ROUTE/{tenantId}",
                arguments = listOf(navArgument("tenantId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val tenantId = checkNotNull(backStackEntry.arguments?.getString("tenantId"))
                NotesTimelineScreen(
                    onBack = { navController.popBackStack() },
                    onAddNote = { navController.navigate("$NOTE_FORM_ROUTE?tenantId=$tenantId") },
                    onOpenNote = { note -> navController.navigate("$NOTE_FORM_ROUTE?tenantId=$tenantId&noteId=${note.id}") },
                )
            }

            composable(
                route = "$NOTE_FORM_ROUTE?tenantId={tenantId}&noteId={noteId}",
                arguments = listOf(
                    navArgument("tenantId") { type = NavType.StringType },
                    navArgument("noteId") { type = NavType.StringType; nullable = true },
                ),
            ) {
                NoteFormScreen(
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "$MULTI_PERIOD_PAYMENT_ROUTE/{tenantId}",
                arguments = listOf(navArgument("tenantId") { type = NavType.StringType }),
            ) {
                MultiPeriodPaymentScreen(
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }

            composable(
                route = "$ACKNOWLEDGEMENT_ROUTE/{invoiceId}",
                arguments = listOf(navArgument("invoiceId") { type = NavType.StringType }),
            ) {
                AcknowledgementScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = "$CREDIT_FORM_ROUTE/{tenantId}",
                arguments = listOf(navArgument("tenantId") { type = NavType.StringType }),
            ) {
                CreditFormScreen(
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }

            composable(
                route = "$TRANSFER_ROUTE/{tenantId}",
                arguments = listOf(navArgument("tenantId") { type = NavType.StringType }),
            ) {
                TransferScreen(
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }

            composable(
                route = "$FORGONE_RENT_ROUTE/{tenantId}",
                arguments = listOf(navArgument("tenantId") { type = NavType.StringType }),
            ) {
                ForgoneRentScreen(
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
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
                    onBedSelected = { choice -> navController.navigate("$TENANT_FORM_ROUTE?bedId=${choice.bedId}") },
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
                    // Unwind the whole onboarding chain, back to wherever the warden started it:
                    // the Tenants tab via the bed picker, or the bed grid via a bed tap. Popping
                    // to a hardcoded Tenants destination stranded the second route, whose back
                    // stack holds no Tenants entry at all - the pop silently did nothing, leaving
                    // the warden on a form whose Save looked dead but wrote a fresh tenancy on
                    // every press (LODGY-69). The second pop is a no-op on the bed-tap route.
                    onDone = {
                        navController.popBackStack(TENANT_FORM_ROUTE_PATTERN, inclusive = true)
                        navController.popBackStack(BED_PICKER_ROUTE, inclusive = true)
                    },
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

            // Kept as a redirect rather than deleted: LODGY-59's vacancy notification carries
            // ROUTE_VACANT_VIEW in its intent, and an already-delivered notification on a warden's
            // phone would otherwise open nothing after they update.
            composable(
                route = "$INVOICE_LIST_ROUTE?filter={filter}&hostelId={hostelId}",
                arguments = listOf(
                    navArgument("filter") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("hostelId") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                InvoiceListScreen(
                    onRecordPayment = { invoice -> navController.navigate("$RECORD_PAYMENT_ROUTE/${invoice.id}") },
                    onSendReminder = { invoice -> navController.navigate("$REMINDER_ROUTE/${invoice.id}") },
                    onOpenReceipt = { invoice -> navController.navigate("$ACKNOWLEDGEMENT_ROUTE/${invoice.id}") },
                    onAddManualInvoice = { navController.navigate(MANUAL_INVOICE_TENANT_PICKER_ROUTE) },
                )
            }

            composable(VACANT_VIEW_ROUTE) {
                AllRoomsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRoom = { roomId -> navController.navigate("$BED_GRID_ROUTE/$roomId") },
                )
            }

            composable(MONTHLY_REPORT_ROUTE) {
                MonthlyReportScreen(onBack = { navController.popBackStack() })
            }

            composable(EXPENSE_LIST_ROUTE) {
                ExpenseListScreen(
                    onBack = { navController.popBackStack() },
                    onAddExpense = { navController.navigate(EXPENSE_FORM_ROUTE) },
                    onEditExpense = { expense -> navController.navigate("$EXPENSE_FORM_ROUTE?expenseId=${expense.id}") },
                )
            }

            composable(
                route = "$EXPENSE_FORM_ROUTE?expenseId={expenseId}",
                arguments = listOf(navArgument("expenseId") { type = NavType.StringType; nullable = true }),
            ) {
                ExpenseFormScreen(
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(BACKUP_ROUTE) {
                BackupScreen(onBack = { navController.popBackStack() })
            }

            composable(DATA_PACKET_ROUTE) {
                DataPacketScreen(onBack = { navController.popBackStack() })
            }

            composable(HISTORY_IMPORT_ROUTE) {
                HistoryImportScreen(onBack = { navController.popBackStack() })
            }

            composable(STAY_BACKFILL_ROUTE) {
                StayBackfillScreen(onBack = { navController.popBackStack() })
            }

            composable(NOTIFICATION_SETTINGS_ROUTE) {
                NotificationSettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
