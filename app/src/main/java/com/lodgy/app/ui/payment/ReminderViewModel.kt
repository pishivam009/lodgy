package com.lodgy.app.ui.payment

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodgy.app.contact.ReminderChannel
import com.lodgy.app.contact.ReminderLanguage
import com.lodgy.app.contact.ReminderMessageBuilder
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.FloorRepository
import com.lodgy.app.data.repository.HostelRepository
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.RoomRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReminderUiState(
    val loading: Boolean = true,
    val tenantPhone: String = "",
    val channel: ReminderChannel = ReminderChannel.WHATSAPP,
    val language: ReminderLanguage = ReminderLanguage.HINDI,
    val message: String = "",
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    invoiceRepository: InvoiceRepository,
    tenancyAgreementRepository: TenancyAgreementRepository,
    tenantRepository: TenantRepository,
    bedRepository: BedRepository,
    roomRepository: RoomRepository,
    floorRepository: FloorRepository,
    hostelRepository: HostelRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val invoiceId: String = checkNotNull(savedStateHandle["invoiceId"])

    private var tenantName: String = ""
    private var amountDue: Double = 0.0
    private var dueDateMillis: Long = System.currentTimeMillis()
    private var hostelName: String = ""

    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val invoice = invoiceRepository.getById(invoiceId) ?: return@launch
            val agreement = tenancyAgreementRepository.getById(invoice.tenancyAgreementId) ?: return@launch
            val tenant = tenantRepository.getById(agreement.tenantId)
            val bed = bedRepository.getById(agreement.bedId)
            val room = bed?.let { roomRepository.getById(it.roomId) }
            val floor = room?.let { floorRepository.getById(it.floorId) }
            val hostel = floor?.let { hostelRepository.getById(it.hostelId) }

            tenantName = tenant?.name.orEmpty()
            amountDue = invoice.amountDue
            dueDateMillis = invoice.dueDate
            hostelName = hostel?.name.orEmpty()

            _uiState.update {
                it.copy(loading = false, tenantPhone = tenant?.phone.orEmpty(), message = buildMessage(it.language))
            }
        }
    }

    fun onChannelChange(channel: ReminderChannel) = _uiState.update { it.copy(channel = channel) }

    fun onLanguageChange(language: ReminderLanguage) =
        _uiState.update { it.copy(language = language, message = buildMessage(language)) }

    private fun buildMessage(language: ReminderLanguage): String =
        ReminderMessageBuilder.build(context, language, tenantName, amountDue, dueDateMillis, hostelName)
}
