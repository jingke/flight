package com.flightbooking.app.ui.screens.map

import androidx.lifecycle.SavedStateHandle
import com.flightbooking.app.MainDispatcherRule
import com.flightbooking.app.TestFixtures
import com.flightbooking.app.domain.repository.AirportRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteMapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var flightRepository: FlightRepository
    private lateinit var airportRepository: AirportRepository

    @Before
    fun setup() {
        flightRepository = mockk()
        airportRepository = mockk()
    }

    private fun createViewModel(flightId: Int = 7): RouteMapViewModel {
        val savedState = SavedStateHandle(mapOf("flightId" to flightId))
        return RouteMapViewModel(savedState, flightRepository, airportRepository)
    }

    @Test
    fun `init loads flight with coordinates`() = runTest {
        val flight = TestFixtures.createFlight(id = 7)
        coEvery { flightRepository.getFlightById(7) } returns Result.Success(flight)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNotNull(state.flight)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `init fetches airport coordinates when missing`() = runTest {
        val depAirport = TestFixtures.createAirport(id = 1, latitude = 0.0, longitude = 0.0)
        val arrAirport = TestFixtures.createAirport(id = 2, latitude = 0.0, longitude = 0.0)
        val flight = TestFixtures.createFlight(
            id = 7, departureAirport = depAirport, arrivalAirport = arrAirport
        )
        val fullDepAirport = TestFixtures.createAirport(id = 1, latitude = 40.6413, longitude = -73.7781)
        val fullArrAirport = TestFixtures.createAirport(id = 2, latitude = 33.9425, longitude = -118.4081)
        coEvery { flightRepository.getFlightById(7) } returns Result.Success(flight)
        coEvery { airportRepository.getAirportById(1) } returns Result.Success(fullDepAirport)
        coEvery { airportRepository.getAirportById(2) } returns Result.Success(fullArrAirport)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val resultFlight = viewModel.uiState.value.flight
        assertNotNull(resultFlight)
        assertEquals(40.6413, resultFlight!!.departureAirport.latitude, 0.001)
        assertEquals(-118.4081, resultFlight.arrivalAirport.longitude, 0.001)
        coVerify { airportRepository.getAirportById(1) }
        coVerify { airportRepository.getAirportById(2) }
    }

    @Test
    fun `init does not fetch airports when coordinates present`() = runTest {
        val flight = TestFixtures.createFlight(id = 7)
        coEvery { flightRepository.getFlightById(7) } returns Result.Success(flight)
        val viewModel = createViewModel()
        advanceUntilIdle()
        coVerify(exactly = 0) { airportRepository.getAirportById(any()) }
    }

    @Test
    fun `init shows error on flight load failure`() = runTest {
        coEvery { flightRepository.getFlightById(7) } returns Result.Error("Not found")
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNull(state.flight)
        assertEquals("Not found", state.errorMessage)
        assertFalse(state.isLoading)
    }
}
