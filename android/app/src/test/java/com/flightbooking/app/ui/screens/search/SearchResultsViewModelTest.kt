package com.flightbooking.app.ui.screens.search

import androidx.lifecycle.SavedStateHandle
import com.flightbooking.app.MainDispatcherRule
import com.flightbooking.app.TestFixtures
import com.flightbooking.app.domain.repository.FlightRepository
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
class SearchResultsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var flightRepository: FlightRepository

    @Before
    fun setup() {
        flightRepository = mockk()
    }

    private fun createViewModel(
        departure: String = "JFK",
        arrival: String = "LAX",
        date: String = "2026-03-15",
        passengers: Int = 1
    ): SearchResultsViewModel {
        val savedState = SavedStateHandle(
            mapOf(
                "departure" to departure,
                "arrival" to arrival,
                "date" to date,
                "passengers" to passengers
            )
        )
        return SearchResultsViewModel(savedState, flightRepository)
    }

    @Test
    fun `init populates search parameters from saved state`() = runTest {
        coEvery { flightRepository.searchFlights("JFK", "LAX", "2026-03-15") } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("JFK", state.departure)
        assertEquals("LAX", state.arrival)
        assertEquals("2026-03-15", state.date)
        assertEquals(1, state.passengers)
    }

    @Test
    fun `init searches flights successfully`() = runTest {
        val flights = listOf(
            TestFixtures.createFlight(id = 1, flightNumber = "FL100"),
            TestFixtures.createFlight(id = 2, flightNumber = "FL200")
        )
        coEvery { flightRepository.searchFlights("JFK", "LAX", "2026-03-15") } returns Result.Success(flights)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(2, state.flights.size)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `init shows error on search failure`() = runTest {
        coEvery { flightRepository.searchFlights(any(), any(), any()) } returns Result.Error("Service unavailable")
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.flights.isEmpty())
        assertEquals("Service unavailable", state.errorMessage)
    }

    @Test
    fun `any value converts to null for search parameters`() = runTest {
        coEvery { flightRepository.searchFlights(null, null, null) } returns Result.Success(emptyList())
        val viewModel = createViewModel(departure = "any", arrival = "any", date = "any")
        advanceUntilIdle()
        coVerify { flightRepository.searchFlights(null, null, null) }
    }

    @Test
    fun `retry re-executes search`() = runTest {
        coEvery { flightRepository.searchFlights("JFK", "LAX", "2026-03-15") } returns Result.Error("Timeout")
        val viewModel = createViewModel()
        advanceUntilIdle()
        val flights = listOf(TestFixtures.createFlight())
        coEvery { flightRepository.searchFlights("JFK", "LAX", "2026-03-15") } returns Result.Success(flights)
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.flights.size)
        assertNull(viewModel.uiState.value.errorMessage)
        coVerify(exactly = 2) { flightRepository.searchFlights("JFK", "LAX", "2026-03-15") }
    }
}
