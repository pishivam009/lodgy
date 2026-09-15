package com.lodgy.app.ui.backup

import android.net.Uri
import com.lodgy.app.backup.HistoryCsvReader
import com.lodgy.app.backup.STAY_CSV_HEADER
import com.lodgy.app.data.dao.BackfilledStayRow
import com.lodgy.app.data.dao.BedChoice
import com.lodgy.app.data.entity.OccupancyPeriod
import com.lodgy.app.data.entity.PropertyType
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.data.repository.BackfillOutcome
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.data.repository.OccupancyPeriodRepository
import com.lodgy.app.data.repository.TenantRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class StayBackfillViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val occupancyPeriodRepository: OccupancyPeriodRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()
    private val bedRepository: BedRepository = mockk()
    private val csvReader: HistoryCsvReader = mockk()
    private val uri: Uri = mockk()

    private val tenant = Tenant(
        id = "t1", name = "Ravi", phone = "9876543210", photoPath = null, idProofPhotoPath = null,
        emergencyContactName = "", emergencyContactPhone = "", status = TenantStatus.ACTIVE,
        createdAt = 0L, updatedAt = 0L,
    )
    private val bed = BedChoice(
        bedId = "b1", bedLabel = "A", roomNumber = "101", hostelId = "h1",
        hostelName = "Sunrise Hostel", propertyType = PropertyType.HOSTEL,
    )

    @Before
    fun setUp() {
        every { tenantRepository.getAll() } returns flowOf(listOf(tenant))
        coEvery { bedRepository.getAllChoices() } returns listOf(bed)
        coEvery { occupancyPeriodRepository.getBackfilledStays() } returns emptyList()
    }

    private fun viewModel() =
        StayBackfillViewModel(occupancyPeriodRepository, tenantRepository, bedRepository, csvReader)

    @Test
    fun `loads tenants, beds and existing backfilled stays on start`() {
        val stay = BackfilledStayRow("p1", "Ravi", "A", "101", "Sunrise Hostel", PropertyType.HOSTEL, 100L, 200L)
        coEvery { occupancyPeriodRepository.getBackfilledStays() } returns listOf(stay)

        val state = viewModel().uiState.value

        assertEquals(listOf(tenant), state.tenants)
        assertEquals(listOf(bed), state.beds)
        assertEquals(listOf(stay), state.stays)
    }

    @Test
    fun `cannot save until tenant, bed and a valid date range are all chosen`() {
        val vm = viewModel()
        assertTrue(!vm.uiState.value.canSave)

        vm.onTenantSelected("t1")
        vm.onBedSelected("b1")
        vm.onStartDateChange(200L)
        vm.onEndDateChange(100L)
        assertTrue(!vm.uiState.value.canSave)

        vm.onEndDateChange(300L)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `save backfills the period and clears the form`() {
        coEvery { occupancyPeriodRepository.backfill("t1", "b1", 100L, 200L) } returns
            BackfillOutcome.Saved(mockk(relaxed = true))

        val vm = viewModel()
        vm.onTenantSelected("t1")
        vm.onBedSelected("b1")
        vm.onStartDateChange(100L)
        vm.onEndDateChange(200L)
        vm.save()

        coVerify { occupancyPeriodRepository.backfill("t1", "b1", 100L, 200L) }
        val state = vm.uiState.value
        assertTrue(state.saved)
        assertNull(state.selectedTenantId)
        assertNull(state.selectedBedId)
    }

    @Test
    fun `save surfaces an overlap without clearing the form`() {
        coEvery { occupancyPeriodRepository.backfill("t1", "b1", 100L, 200L) } returns BackfillOutcome.Overlaps

        val vm = viewModel()
        vm.onTenantSelected("t1")
        vm.onBedSelected("b1")
        vm.onStartDateChange(100L)
        vm.onEndDateChange(200L)
        vm.save()

        val state = vm.uiState.value
        assertTrue(state.overlapError)
        assertEquals("t1", state.selectedTenantId)
        assertEquals(100L, state.startDateMillis)
    }

    @Test
    fun `startEdit loads an existing period back into the form`() {
        val period = OccupancyPeriod(
            id = "p1", tenantId = "t1", bedId = "b1", tenancyAgreementId = null,
            startDate = 100L, endDate = 200L, backfilled = true, createdAt = 0L, updatedAt = 0L,
        )
        coEvery { occupancyPeriodRepository.getById("p1") } returns period

        val vm = viewModel()
        vm.startEdit(BackfilledStayRow("p1", "Ravi", "A", "101", "Sunrise Hostel", PropertyType.HOSTEL, 100L, 200L))

        val state = vm.uiState.value
        assertEquals("p1", state.editingPeriodId)
        assertEquals("t1", state.selectedTenantId)
        assertEquals("b1", state.selectedBedId)
        assertEquals(100L, state.startDateMillis)
        assertEquals(200L, state.endDateMillis)
    }

    @Test
    fun `saving while editing corrects the existing period instead of creating a new one`() {
        val period = OccupancyPeriod(
            id = "p1", tenantId = "t1", bedId = "b1", tenancyAgreementId = null,
            startDate = 100L, endDate = 200L, backfilled = true, createdAt = 0L, updatedAt = 0L,
        )
        coEvery { occupancyPeriodRepository.getById("p1") } returns period
        coEvery { occupancyPeriodRepository.editBackfilled(period, "b1", 110L, 210L) } returns
            BackfillOutcome.Saved(period.copy(startDate = 110L, endDate = 210L))

        val vm = viewModel()
        vm.startEdit(BackfilledStayRow("p1", "Ravi", "A", "101", "Sunrise Hostel", PropertyType.HOSTEL, 100L, 200L))
        vm.onStartDateChange(110L)
        vm.onEndDateChange(210L)
        vm.save()

        coVerify { occupancyPeriodRepository.editBackfilled(period, "b1", 110L, 210L) }
        coVerify(exactly = 0) { occupancyPeriodRepository.backfill(any(), any(), any(), any()) }
        assertNull(vm.uiState.value.editingPeriodId)
    }

    @Test
    fun `delete removes the stay and refreshes the list`() {
        val period = OccupancyPeriod(
            id = "p1", tenantId = "t1", bedId = "b1", tenancyAgreementId = null,
            startDate = 100L, endDate = 200L, backfilled = true, createdAt = 0L, updatedAt = 0L,
        )
        coEvery { occupancyPeriodRepository.getById("p1") } returns period
        coEvery { occupancyPeriodRepository.delete(period) } returns Unit

        viewModel().delete(BackfilledStayRow("p1", "Ravi", "A", "101", "Sunrise Hostel", PropertyType.HOSTEL, 100L, 200L))

        coVerify { occupancyPeriodRepository.delete(period) }
    }

    @Test
    fun `a file that cannot be read is reported rather than looking like an empty import`() {
        coEvery { csvReader.read(uri) } returns null

        val vm = viewModel()
        vm.onFilePicked(uri)

        assertTrue(vm.uiState.value.readFailed)
        assertTrue(!vm.uiState.value.canImportCsv)
    }

    @Test
    fun `csv rows naming an unknown tenant or bed are reported as unmatched, not silently dropped`() {
        coEvery { csvReader.read(uri) } returns
            "$STAY_CSV_HEADER\n9876543210,Sunrise Hostel,101,A,2023-01-15,2023-06-01\n" +
            "5550000000,Sunrise Hostel,101,A,2023-01-15,2023-06-01\n" +
            "9876543210,Unknown Hostel,999,Z,2023-01-15,2023-06-01"

        val vm = viewModel()
        vm.onFilePicked(uri)

        val state = vm.uiState.value
        assertEquals(3, state.csvRows.size)
        assertEquals(2, state.unmatchedRows.size)
        assertEquals(1, state.importableCount)
    }

    @Test
    fun `importCsv writes only matched rows and counts overlaps separately`() {
        coEvery { csvReader.read(uri) } returns
            "9876543210,Sunrise Hostel,101,A,2023-01-15,2023-06-01\n" +
            "5550000000,Sunrise Hostel,101,A,2023-07-01,2023-12-01"
        coEvery { occupancyPeriodRepository.backfill("t1", "b1", any(), any()) } returns
            BackfillOutcome.Saved(mockk(relaxed = true))

        val vm = viewModel()
        vm.onFilePicked(uri)
        vm.importCsv()

        coVerify(exactly = 1) { occupancyPeriodRepository.backfill("t1", "b1", any(), any()) }
        assertEquals(1, vm.uiState.value.imported)
        assertEquals(0, vm.uiState.value.skippedOverlap)
    }

    @Test
    fun `importCsv counts an overlap without failing the whole import`() {
        coEvery { csvReader.read(uri) } returns "9876543210,Sunrise Hostel,101,A,2023-01-15,2023-06-01"
        coEvery { occupancyPeriodRepository.backfill("t1", "b1", any(), any()) } returns BackfillOutcome.Overlaps

        val vm = viewModel()
        vm.onFilePicked(uri)
        vm.importCsv()

        assertEquals(0, vm.uiState.value.imported)
        assertEquals(1, vm.uiState.value.skippedOverlap)
    }
}
