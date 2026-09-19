package com.lodgy.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lodgy.app.R

/**
 * Why an update is being confirmed. DESIGN.md 4.12 draws the line: a destructive action always
 * confirms, an update only when it silently changes money, occupancy or history the warden cannot
 * see on the screen they are on (LODGY-65). Each case carries the before and after so the dialog
 * can show the actual figures - "this changes the rent" is dismissable, "₹4000 becomes ₹4500" is
 * the thing the warden is being asked to check.
 */
sealed interface UpdateChange {
    data class RoomPrice(val from: Double, val to: Double) : UpdateChange
    data object RoomTypeWithOccupiedBed : UpdateChange
    data class UnitRent(val from: Double, val to: Double) : UpdateChange
    data class HostelRename(val to: String, val reconciledPeriods: Int) : UpdateChange
    data class TenancyRent(val from: Double, val to: Double) : UpdateChange
    /** Moving an expense to a different property's ledger (LODGY-110). */
    data class ExpenseProperty(val from: String, val to: String) : UpdateChange
}

@Composable
fun UpdateChange.message(): String = when (this) {
    is UpdateChange.RoomPrice -> stringResource(R.string.update_confirm_room_price, from, to)
    UpdateChange.RoomTypeWithOccupiedBed -> stringResource(R.string.room_type_change_confirm_body)
    is UpdateChange.UnitRent -> stringResource(R.string.update_confirm_unit_rent, from, to)
    is UpdateChange.HostelRename ->
        stringResource(R.string.update_confirm_hostel_rename, to, reconciledPeriods)
    is UpdateChange.TenancyRent -> stringResource(R.string.update_confirm_tenancy_rent, from, to)
    is UpdateChange.ExpenseProperty -> stringResource(R.string.update_confirm_expense_property, from, to)
}

/**
 * One dialog for every confirmed update, so the four sites read identically and a fifth has
 * somewhere obvious to go. Several changes in one save produce one dialog listing them all rather
 * than a queue of dialogs - a warden clicking through three in a row reads none of them.
 */
@Composable
fun UpdateConfirmDialog(
    changes: List<UpdateChange>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (changes.isEmpty()) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                changes.forEach { Text(it.message()) }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.update_confirm_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
