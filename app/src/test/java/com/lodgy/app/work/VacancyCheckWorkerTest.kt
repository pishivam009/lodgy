package com.lodgy.app.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.lodgy.app.data.dao.VacantBedDetail
import com.lodgy.app.data.prefs.NotificationPreferences
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.notify.CHANNEL_VACANCY
import com.lodgy.app.notify.LodgyNotifications
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VacancyCheckWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val params: WorkerParameters = mockk(relaxed = true)
    private val bedRepository: BedRepository = mockk()
    private val preferences: NotificationPreferences = mockk(relaxed = true)
    private val notifications: LodgyNotifications = mockk(relaxed = true)

    private fun bed(id: String) = VacantBedDetail(
        bedId = id,
        bedLabel = "A",
        roomNumber = "101",
        floorLabel = "Ground",
        hostelName = "Sunrise",
        vacantSince = 0L,
    )

    @Before
    fun setUp() {
        every { preferences.vacancyEnabled } returns flowOf(true)
        every { preferences.vacancyThresholdDays } returns flowOf(7)
        every { preferences.notifiedBedIds } returns flowOf(emptySet())
    }

    private fun worker() = VacancyCheckWorker(context, params, bedRepository, preferences, notifications)

    @Test
    fun `a long-vacant bed produces one notification on the vacancy channel`() = runTest {
        coEvery { bedRepository.getLongVacantBeds(any()) } returns listOf(bed("b1"))
        coEvery { bedRepository.getVacantBedIds() } returns listOf("b1")

        assertEquals(ListenableWorker.Result.success(), worker().doWork())

        coVerify { notifications.post(CHANNEL_VACANCY, any(), any(), any(), any()) }
        coVerify { preferences.setNotifiedBedIds(setOf("b1")) }
    }

    @Test
    fun `the switch being off skips the check entirely`() = runTest {
        every { preferences.vacancyEnabled } returns flowOf(false)

        assertEquals(ListenableWorker.Result.success(), worker().doWork())

        coVerify(exactly = 0) { notifications.post(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { preferences.setNotifiedBedIds(any()) }
    }

    @Test
    fun `a bed already nudged about is not nudged again`() = runTest {
        every { preferences.notifiedBedIds } returns flowOf(setOf("b1"))
        coEvery { bedRepository.getLongVacantBeds(any()) } returns listOf(bed("b1"))
        coEvery { bedRepository.getVacantBedIds() } returns listOf("b1")

        worker().doWork()

        coVerify(exactly = 0) { notifications.post(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a bed that has since been filled is forgotten even though nothing is posted`() = runTest {
        every { preferences.notifiedBedIds } returns flowOf(setOf("b1"))
        coEvery { bedRepository.getLongVacantBeds(any()) } returns emptyList()
        coEvery { bedRepository.getVacantBedIds() } returns emptyList()

        worker().doWork()

        coVerify { preferences.setNotifiedBedIds(emptySet()) }
        coVerify(exactly = 0) { notifications.post(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `the warden's threshold decides the cutoff, not a constant`() = runTest {
        every { preferences.vacancyThresholdDays } returns flowOf(30)
        val cutoff = slot<Long>()
        coEvery { bedRepository.getLongVacantBeds(capture(cutoff)) } returns emptyList()
        coEvery { bedRepository.getVacantBedIds() } returns emptyList()

        worker().doWork()

        val daysAgo = (System.currentTimeMillis() - cutoff.captured) / (24 * 60 * 60 * 1000)
        assertEquals(30L, daysAgo)
    }
}
