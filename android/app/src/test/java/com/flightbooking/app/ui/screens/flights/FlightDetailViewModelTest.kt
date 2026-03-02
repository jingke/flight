package com.flightbooking.app.ui.screens.flights

import androidx.lifecycle.SavedStateHandle
import com.flightbooking.app.MainDispatcherRule
import com.flightbooking.app.TestFixtures
import com.flightbooking.app.domain.repository.BookingRepository
import com.flightbooking.app.domain.repository.FlightRepository
import com.flightbooking.app.domain.repository.PassengerInput
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlightDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var flightRepository: FlightRepository
    private lateinit var bookingRepository: BookingRepository

    @Before
    fun setup() {
        flightRepository = mockk()
        bookingRepository = mockk()
    }

    private fun createViewModel(flightId: Int = 10): FlightDetailViewModel {
        val savedState = SavedStateHandle(mapOf("flightId" to flightId))
        return FlightDetailViewModel(savedState, flightRepository, bookingRepository)
    }

    @Test
    fun `init loads flight and seats successfully`() = runTest {
        val flight = TestFixtures.createFlight(id = 10)
        val seats = listOf(
            TestFixtures.createSeat(id = 1, row = 1, column = "A"),
            TestFixtures.createSeat(id = 2, row = 1, column = "B")
        )
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(flight)
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(seats)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNotNull(state.flight)
        assertEquals(2, state.seats.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `init shows error when flight not found`() = runTest {
        coEvery { flightRepository.getFlightById(10) } returns Result.Error("Flight not found")
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals("Flight not found", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `initial state has one empty passenger form`() = runTest {
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(TestFixtures.createFlight(id = 10))
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.passengers.size)
        assertEquals("", viewModel.uiState.value.passengers[0].name)
    }

    @Test
    fun `UpdatePassengerName updates the correct passenger`() = runTest {
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(TestFixtures.createFlight(id = 10))
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(FlightDetailEvent.UpdatePassengerName(0, "Alice"))
        assertEquals("Alice", viewModel.uiState.value.passengers[0].name)
    }

    @Test
    fun `UpdatePassengerEmail updates the correct passenger`() = runTest {
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(TestFixtures.createFlight(id = 10))
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(FlightDetailEvent.UpdatePassengerEmail(0, "alice@test.com"))
        assertEquals("alice@test.com", viewModel.uiState.value.passengers[0].email)
    }

    @Test
    fun `AddPassenger adds a new passenger and sets it active`() = runTest {
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(TestFixtures.createFlight(id = 10))
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(FlightDetailEvent.AddPassenger)
        val state = viewModel.uiState.value
        assertEquals(2, state.passengers.size)
        assertEquals(1, state.activePassengerIndex)
    }

    @Test
    fun `RemovePassenger removes passenger and adjusts active index`() = runTest {
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(TestFixtures.createFlight(id = 10))
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(FlightDetailEvent.AddPassenger)
        viewModel.onEvent(FlightDetailEvent.UpdatePassengerName(0, "Alice"))
        viewModel.onEvent(FlightDetailEvent.UpdatePassengerName(1, "Bob"))
        viewModel.onEvent(FlightDetailEvent.RemovePassenger(1))
        val state = viewModel.uiState.value
        assertEquals(1, state.passengers.size)
        assertEquals("Alice", state.passengers[0].name)
    }

    @Test
    fun `RemovePassenger does not remove last passenger`() = runTest {
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(TestFixtures.createFlight(id = 10))
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(FlightDetailEvent.RemovePassenger(0))
        assertEquals(1, viewModel.uiState.value.passengers.size)
    }

    @Test
    fun `SetActivePassenger changes active index`() = runTest {
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(TestFixtures.createFlight(id = 10))
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(FlightDetailEvent.AddPassenger)
        viewModel.onEvent(FlightDetailEvent.SetActivePassenger(0))
        assertEquals(0, viewModel.uiState.value.activePassengerIndex)
    }

    @Test
    fun `ToggleSeat assigns seat to active passenger`() = runTest {
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(TestFixtures.createFlight(id = 10))
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(
            listOf(TestFixtures.createSeat(id = 5))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(FlightDetailEvent.ToggleSeat(5))
        assertEquals(5, viewModel.uiState.value.passengers[0].selectedSeatId)
    }

    @Test
    fun `ToggleSeat deselects seat when already selected`() = runTest {
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(TestFixtures.createFlight(id = 10))
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(
            listOf(TestFixtures.createSeat(id = 5))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(FlightDetailEvent.ToggleSeat(5))
        viewModel.onEvent(FlightDetailEvent.ToggleSeat(5))
        assertNull(viewModel.uiState.value.passengers[0].selectedSeatId)
    }

    @Test
    fun `ToggleSeat reassigns seat from another passenger`() = runTest {
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(TestFixtures.createFlight(id = 10))
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(
            listOf(TestFixtures.createSeat(id = 5))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(FlightDetailEvent.ToggleSeat(5))
        viewModel.onEvent(FlightDetailEvent.AddPassenger)
        viewModel.onEvent(FlightDetailEvent.ToggleSeat(5))
        assertNull(viewModel.uiState.value.passengers[0].selectedSeatId)
        assertEquals(5, viewModel.uiState.value.passengers[1].selectedSeatId)
    }

    @Test
    fun `CreateBooking with missing passenger name shows error`() = runTest {
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(TestFixtures.createFlight(id = 10))
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(FlightDetailEvent.UpdatePassengerEmail(0, "test@test.com"))
        viewModel.onEvent(FlightDetailEvent.CreateBooking)
        assertEquals("All passenger details are required", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `CreateBooking success sets bookingCreatedId`() = runTest {
        val flight = TestFixtures.createFlight(id = 10)
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(flight)
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(emptyList())
        coEvery { bookingRepository.createBooking(10, any()) } returns Result.Success(
            TestFixtures.createBooking(id = 99)
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(FlightDetailEvent.UpdatePassengerName(0, "John"))
        viewModel.onEvent(FlightDetailEvent.UpdatePassengerEmail(0, "john@test.com"))
        viewModel.onEvent(FlightDetailEvent.CreateBooking)
        advanceUntilIdle()
        assertEquals(99, viewModel.uiState.value.bookingCreatedId)
        assertFalse(viewModel.uiState.value.isBooking)
    }

    @Test
    fun `CreateBooking failure shows error message`() = runTest {
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(TestFixtures.createFlight(id = 10))
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(emptyList())
        coEvery { bookingRepository.createBooking(10, any()) } returns Result.Error("Payment failed")
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(FlightDetailEvent.UpdatePassengerName(0, "John"))
        viewModel.onEvent(FlightDetailEvent.UpdatePassengerEmail(0, "john@test.com"))
        viewModel.onEvent(FlightDetailEvent.CreateBooking)
        advanceUntilIdle()
        assertEquals("Payment failed", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `ClearError clears error message`() = runTest {
        coEvery { flightRepository.getFlightById(10) } returns Result.Success(TestFixtures.createFlight(id = 10))
        coEvery { flightRepository.getSeatMap(10) } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(FlightDetailEvent.CreateBooking)
        viewModel.onEvent(FlightDetailEvent.ClearError)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
