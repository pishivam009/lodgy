package com.lodgy.app.ui.tenant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgreementFormUiState(
    val agreedRent: String = "",
    val advanceDeposit: String = "",
    val billingCycleDay: String = "",
    val moveInDateMillis: Long = System.currentTimeMillis(),
    /** Optional. What the tenant already owed when the app took over their record - the
     *  alternative to re-typing years of past invoices (LODGY-44). */
    val openingBalance: String = "",
    /** The warden's or a caretaker's own room: real occupancy, billed to nobody (LODGY-82). */
    val nonRevenue: Boolean = false,
    val saved: Boolean = false,
) {
    val openingBalanceAmount: Double get() = openingBalance.toDoubleOrNull() ?: 0.0
    val billingCycleDayValid: Boolean
        get() = billingCycleDay.toIntOrNull()?.let { it in 1..28 } == true

    val canSave: Boolean
        get() = agreedRent.toDoubleOrNull() != null && advanceDeposit.toDoubleOrNull() != null && billingCycleDayValid
}

@HiltViewModel
class AgreementFormViewModel @Inject constructor(
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val bedRepository: BedRepository,
    private val invoiceRepository: InvoiceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tenantId: String = checkNotNull(savedStateHandle["tenantId"])
    private val bedId: String = checkNotNull(savedStateHandle["bedId"])

    private val _uiState = MutableStateFlow(AgreementFormUiState())
    val uiState: StateFlow<AgreementFormUiState> = _uiState.asStateFlow()

    fun onAgreedRentChange(value: String) = _uiState.update { it.copy(agreedRent = value) }
    fun onAdvanceDepositChange(value: String) = _uiState.update { it.copy(advanceDeposit = value) }
    fun onBillingCycleDayChange(value: String) = _uiState.update { it.copy(billingCycleDay = value) }
    fun onMoveInDateChange(millis: Long) = _uiState.update { it.copy(moveInDateMillis = millis) }
    fun onOpeningBalanceChange(value: String) = _uiState.update { it.copy(openingBalance = value) }

    fun onNonRevenueChange(value: Boolean) = _uiState.update {
        // Rent and an opening balance are meaningless on a room that bills nobody, so clear them
        // rather than storing figures that will never be charged.
        if (value) it.copy(nonRevenue = true, agreedRent = "0", openingBalance = "") else it.copy(nonRevenue = false)
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val rent = state.agreedRent.toDoubleOrNull() ?: return
        val deposit = state.advanceDeposit.toDoubleOrNull() ?: return
        val billingDay = state.billingCycleDay.toIntOrNull() ?: return
        viewModelScope.launch {
            val agreement = tenancyAgreementRepository.create(
                tenantId, bedId, rent, deposit, billingDay, state.moveInDateMillis,
                state.nonRevenue,
            )
            bedRepository.setOccupied(bedId)
            if (state.openingBalanceAmount > 0.0) {
                createOpeningInvoice(agreement.id, state.openingBalanceAmount, state.moveInDateMillis)
            }
            _uiState.update { it.copy(saved = true) }
        }
    }

    /**
     * Carried-forward dues become one real, payable invoice rather than a number stashed on the
     * agreement, so they flow through the invoice list, the reports and the reminders like any
     * other due. It is dated the month BEFORE move-in: the generator only ever creates invoices
     * for the current month forward, so that period can never collide with one it produces.
     */
    private suspend fun createOpeningInvoice(agreementId: String, amount: Double, moveInMillis: Long) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = moveInMillis
            add(Calendar.MONTH, -1)
        }
        invoiceRepository.create(
            tenancyAgreementId = agreementId,
            periodMonth = calendar.get(Calendar.MONTH) + 1,
            periodYear = calendar.get(Calendar.YEAR),
            amountDue = amount,
            dueDate = moveInMillis,
        )
    }
}
