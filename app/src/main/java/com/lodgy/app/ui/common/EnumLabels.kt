package com.lodgy.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lodgy.app.R
import com.lodgy.app.data.entity.ExpenseCategory
import com.lodgy.app.data.entity.NoteType
import com.lodgy.app.data.entity.PaymentMode
import com.lodgy.app.data.entity.RoomType

@Composable
fun RoomType.label(): String = stringResource(
    when (this) {
        RoomType.SINGLE -> R.string.room_type_single
        RoomType.DOUBLE -> R.string.room_type_double
        RoomType.TRIPLE -> R.string.room_type_triple
    },
)

/** Split out of [label] so non-composable callers - the notification workers - resolve the same
 *  string through a plain Context instead of duplicating the mapping. */
val ExpenseCategory.labelRes: Int
    get() = when (this) {
        ExpenseCategory.WIFI -> R.string.expense_category_wifi
        ExpenseCategory.WATER -> R.string.expense_category_water
        ExpenseCategory.ELECTRICITY -> R.string.expense_category_electricity
        ExpenseCategory.TAX -> R.string.expense_category_tax
        ExpenseCategory.MAINTENANCE -> R.string.expense_category_maintenance
        ExpenseCategory.REPAIR -> R.string.expense_category_repair
        ExpenseCategory.OTHER -> R.string.expense_category_other
    }

@Composable
fun ExpenseCategory.label(): String = stringResource(labelRes)

@Composable
fun NoteType.label(): String = stringResource(
    when (this) {
        NoteType.COMPLAINT -> R.string.note_type_complaint
        NoteType.DAMAGE -> R.string.note_type_damage
        NoteType.GENERAL -> R.string.note_type_general
    },
)

@Composable
fun PaymentMode.label(): String = stringResource(
    when (this) {
        PaymentMode.CASH -> R.string.payment_mode_cash
        PaymentMode.UPI -> R.string.payment_mode_upi
        PaymentMode.BANK_TRANSFER -> R.string.payment_mode_bank_transfer
        PaymentMode.OTHER -> R.string.payment_mode_other
    },
)

@Composable
fun RoomFill.label(): String = stringResource(
    when (this) {
        RoomFill.EMPTY -> R.string.room_occupancy_empty
        RoomFill.PARTIAL -> R.string.room_occupancy_partial
        RoomFill.FULL -> R.string.room_occupancy_full
    },
)
