package com.flightbooking.app.ui.screens.bookings

import app.cash.turbine.test
import com.flightbooking.app.MainDispatcherRule
import com.flightbooking.app.TestFixtures
import com.flightbooking.app.domain.repository.BookingRepository
import com.flightbooking.app.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyBookingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bookingRepository: BookingRepository

    @Before
    fun setup() {
        bookingRepository = mockk()
    }

    private fun createViewModel(): MyBookingsViewModel {
        return MyBookingsViewModel(bookingRepository)
    }

    @Test
    fun `init loads bookings successfully`() = runTest {
        val bookings = listOf(TestFixtures.createBooking(id = 1), TestFixtures.createBooking(id = 2))
        coEvery { bookingRepository.getBookings() } returns Result.Success(bookings)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.bookings.size)
        assertNull(state.errorMessage)
    }

    @Test
    fun `init shows error on failure`() = runTest {
        coEvery { bookingRepository.getBookings() } returns Result.Error("Network error")
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.bookings.isEmpty())
        assertEquals("Network error", state.errorMessage)
    }

    @Test
    fun `refresh reloads bookings`() = runTest {
        coEvery { bookingRepository.getBookings() } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        val updatedBookings = listOf(TestFixtures.createBooking())
        coEvery { bookingRepository.getBookings() } returns Result.Success(updatedBookings)
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.bookings.size)
        coVerify(exactly = 2) { bookingRepository.getBookings() }
    }

    @Test
    fun `loading state is set during fetch`() = runTest {
        coEvery { bookingRepository.getBookings() } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
        }
    }
}
