package com.lodgy.app.ui.backup

import android.net.Uri
import com.lodgy.app.backup.HISTORY_CSV_HEADER
import com.lodgy.app.backup.HistoryCsvReader
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.PaymentMode
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.PaymentRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryImportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val csvReader: HistoryCsvReader = mockk()
    private val tenantRepository: TenantRepository = mockk()
    private val agreementRepository: TenancyAgreementRepository = mockk()
    private val invoiceRepository: InvoiceRepository = mockk()
    private val paymentRepository: PaymentRepository = mockk()
    private val uri: Uri = mockk()

    private val tenant = Tenant(id = "t1", name = "Ravi", phone = "9876543210", photoPath = null, idProofPhotoPath = null, emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)
    private val agreement = TenancyAgreement(id = "a1", tenantId = "t1", bedId = "b1", agreedRent = 5000.0, advanceDeposit = 0.0, billingCycleDay = 5, moveInDate = 0L, moveOutDate = null, depositRefundAmount = null, status = AgreementStatus.ACTIVE, createdAt = 0L, updatedAt = 0L)

    @Before
    fun setUp() {
        every { tenantRepository.getAll() } returns flowOf(listOf(tenant))
        coEvery { agreementRepository.getLatestByTenantId("t1") } returns agreement
        coEvery { invoiceRepository.existsForPeriod(any(), any(), any()) } returns false
        coEvery { invoiceRepository.create(any(), any(), any(), any(), any()) } answers {
            mockk(relaxed = true)
        }
        coEvery { invoiceRepository.updateStatus(any(), any()) } returns Unit
        coEvery { paymentRepository.create(any(), any(), any(), any(), any()) } returns mockk()
    }

    private fun viewModel(csv: String?): HistoryImportViewModel {
        coEvery { csvReader.read(uri) } returns csv
        return HistoryImportViewModel(csvReader, tenantRepository, agreementRepository, invoiceRepository, paymentRepository)
    }

    @Test
    fun `parsing splits rows into importable and unmatched by phone number`() {
        val viewModel = viewModel("$HISTORY_CSV_HEADER\n9876543210,8,2026,5000,5000\n5550000000,8,2026,4000,0")
        viewModel.onFilePicked(uri)

        val state = viewModel.uiState.value
        assertEquals(2, state.rows.size)
        assertEquals(1, state.unmatchedRows.size)
        assertEquals(1, state.importableCount)
        assertTrue(state.canImport)
    }

    @Test
    fun `a file that cannot be read is reported rather than looking like an empty import`() {
        val viewModel = viewModel(null)
        viewModel.onFilePicked(uri)

        assertTrue(viewModel.uiState.value.readFailed)
        assertFalse(viewModel.uiState.value.canImport)
    }

    @Test
    fun `a fully paid row becomes an invoice plus a payment, marked PAID`() {
        val viewModel = viewModel("9876543210,8,2026,5000,5000")
        viewModel.onFilePicked(uri)
        viewModel.import()

        coVerify { invoiceRepository.create("a1", 8, 2026, 5000.0, any()) }
        coVerify { paymentRepository.create(any(), 5000.0, PaymentMode.OTHER, any(), null) }
        coVerify { invoiceRepository.updateStatus(any(), InvoiceStatus.PAID) }
        assertEquals(1, viewModel.uiState.value.imported)
    }

    @Test
    fun `a partly paid row is marked PARTIAL and an unpaid one records no payment`() {
        viewModel("9876543210,8,2026,5000,2000").also {
            it.onFilePicked(uri)
            it.import()
        }
        coVerify { invoiceRepository.updateStatus(any(), InvoiceStatus.PARTIAL) }

        viewModel("9876543210,9,2026,5000,0").also {
            it.onFilePicked(uri)
            it.import()
        }
        coVerify { invoiceRepository.updateStatus(any(), InvoiceStatus.UNPAID) }
        coVerify(exactly = 0) { paymentRepository.create(any(), 0.0, any(), any(), any()) }
    }

    @Test
    fun `a period the app already has is skipped, so re-running an import cannot double-bill`() {
        coEvery { invoiceRepository.existsForPeriod("a1", 8, 2026) } returns true

        val viewModel = viewModel("9876543210,8,2026,5000,5000")
        viewModel.onFilePicked(uri)
        viewModel.import()

        coVerify(exactly = 0) { invoiceRepository.create(any(), any(), any(), any(), any()) }
        assertEquals(0, viewModel.uiState.value.imported)
    }

    @Test
    fun `rows whose tenant is unknown are never written`() {
        val viewModel = viewModel("5550000000,8,2026,4000,0")
        viewModel.onFilePicked(uri)
        viewModel.import()

        coVerify(exactly = 0) { invoiceRepository.create(any(), any(), any(), any(), any()) }
    }
}
