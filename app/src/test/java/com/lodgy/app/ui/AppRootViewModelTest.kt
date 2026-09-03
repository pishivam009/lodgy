package com.lodgy.app.ui

import android.os.Looper
import com.lodgy.app.data.entity.Warden
import com.lodgy.app.data.repository.WardenRepository
import com.lodgy.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * AppRootViewModel registers itself with ProcessLifecycleOwner, whose LifecycleRegistry asserts
 * it's being touched from the main thread via Looper.getMainLooper()/myLooper() - both throw
 * "not mocked" on the plain JVM android.jar stub used for unit tests. Mocking them to the same
 * fake Looper makes that main-thread check pass so construction can proceed.
 */
class AppRootViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val wardenRepository: WardenRepository = mockk()

    @Before
    fun mockMainLooper() {
        val mainLooper: Looper = mockk()
        every { mainLooper.thread } returns Thread.currentThread()
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mainLooper
        every { Looper.myLooper() } returns mainLooper
    }

    @After
    fun tearDown() {
        unmockkStatic(Looper::class)
    }

    @Test
    fun `no warden yet means the app needs pin setup`() {
        coEvery { wardenRepository.getWarden() } returns null

        val viewModel = AppRootViewModel(wardenRepository)

        assertEquals(AppStartState.NeedsPinSetup, viewModel.state.value)
    }

    @Test
    fun `an existing warden means the app starts locked`() {
        coEvery { wardenRepository.getWarden() } returns Warden(id = "w1", pinHash = "x", name = "Warden", createdAt = 0L, updatedAt = 0L)

        val viewModel = AppRootViewModel(wardenRepository)

        assertEquals(AppStartState.Locked, viewModel.state.value)
    }

    @Test
    fun `onPinSetupComplete and onUnlocked both unlock the app`() {
        coEvery { wardenRepository.getWarden() } returns null
        val viewModel = AppRootViewModel(wardenRepository)

        viewModel.onPinSetupComplete()
        assertEquals(AppStartState.Unlocked, viewModel.state.value)

        viewModel.onUnlocked()
        assertEquals(AppStartState.Unlocked, viewModel.state.value)
    }

    @Test
    fun `onStop re-locks the app only if it was unlocked`() {
        coEvery { wardenRepository.getWarden() } returns null
        val viewModel = AppRootViewModel(wardenRepository)

        viewModel.onStop(mockk())
        assertEquals(AppStartState.NeedsPinSetup, viewModel.state.value)

        viewModel.onUnlocked()
        viewModel.onStop(mockk())
        assertEquals(AppStartState.Locked, viewModel.state.value)
    }
}
