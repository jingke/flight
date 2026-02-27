package com.flightbooking.app.data.repository

import com.flightbooking.app.data.local.TokenManager
import com.flightbooking.app.data.remote.api.AuthApi
import com.flightbooking.app.data.remote.api.BookingApi
import com.flightbooking.app.data.remote.api.FlightApi
import com.flightbooking.app.data.remote.dto.AirportResponse
import com.flightbooking.app.data.remote.dto.BookingResponse
import com.flightbooking.app.data.remote.dto.CreateBookingRequest
import com.flightbooking.app.data.remote.dto.FlightResponse
import com.flightbooking.app.data.remote.dto.PassengerResponse
import com.flightbooking.app.data.remote.dto.SeatResponse
import com.flightbooking.app.data.remote.dto.TokenResponse
import com.flightbooking.app.domain.model.BookingStatus
import com.flightbooking.app.domain.model.PaymentStatus
import com.flightbooking.app.domain.util.Result
import com.flightbooking.app.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Integration tests that simulate the full customer booking journey
 * through the Android repository layer, mirroring the same REST API
 * contract used by the web frontend and verified by backend E2E tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationBookingFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authApi: AuthApi
    private lateinit var bookingApi: BookingApi
    private lateinit var flightApi: FlightApi
    private lateinit var tokenManager: TokenManager

    private lateinit var authRepo: AuthRepositoryImpl
    private lateinit var bookingRepo: BookingRepositoryImpl
    private lateinit var flightRepo: FlightRepositoryImpl

    private val mockAirport = AirportResponse(
        id = 1, code = "JFK", name = "JFK International",
        city = "New York", country = "US",
        latitude = 40.6413, longitude = -73.7781
    )

    private val mockFlightResponse = FlightResponse(
        id = 1, flightNumber = "FB101",
        departureTime = "2026-04-01T08:00:00",
        arrivalTime = "2026-04-01T13:30:00",
        price = 320.0, totalSeats = 180, status = "scheduled",
        departureAirport = mockAirport,
        arrivalAirport = mockAirport.copy(id = 2, code = "LAX", city = "Los Angeles")
    )

    private fun mockBookingResponse(
        id: Int = 1,
        status: String = "confirmed",
        paymentStatus: String = "paid"
    ): BookingResponse = BookingResponse(
        id = id, userId = 1, flightId = 1,
        status = status, totalPrice = 320.0,
        paymentStatus = paymentStatus,
        passengers = listOf(
            PassengerResponse(id = 1, bookingId = id, name = "John Doe", email = "john@test.com", seatAssignment = "1A")
        ),
        createdAt = "2026-03-10T08:00:00",
        flightNumber = "FB101",
        departureAirport = "JFK - New York",
        arrivalAirport = "LAX - Los Angeles",
        departureTime = "2026-04-01T08:00:00",
        arrivalTime = "2026-04-01T13:30:00"
    )

    @Before
    fun setup() {
        authApi = mockk()
        bookingApi = mockk()
        flightApi = mockk()
        tokenManager = mockk(relaxed = true)

        authRepo = AuthRepositoryImpl(authApi, tokenManager)
        bookingRepo = BookingRepositoryImpl(bookingApi)
        flightRepo = FlightRepositoryImpl(flightApi)
    }

    @Test
    fun `complete booking journey - login, search, book, view, cancel`() = runTest {
        // --- Login ---
        val tokenResponse = TokenResponse(accessToken = "jwt-token-123", tokenType = "bearer")
        coEvery { authApi.login(any()) } returns tokenResponse

        val loginResult = authRepo.login("demo@flightbooking.com", "demo123")
        assertTrue(loginResult.isSuccess)
        assertEquals("jwt-token-123", loginResult.getOrNull())
        coVerify { tokenManager.saveToken("jwt-token-123") }

        // --- Search flights ---
        coEvery { flightApi.searchFlights("JFK", null, null) } returns listOf(mockFlightResponse)

        val searchResult = flightRepo.searchFlights("JFK", null, null)
        assertTrue(searchResult.isSuccess)
        val flights = searchResult.getOrNull()!!
        assertEquals(1, flights.size)
        assertEquals("FB101", flights[0].flightNumber)
        assertEquals("JFK", flights[0].departureAirport.code)

        // --- Get flight detail ---
        coEvery { flightApi.getFlightById(1) } returns mockFlightResponse

        val flightResult = flightRepo.getFlightById(1)
        assertTrue(flightResult.isSuccess)
        assertEquals(320.0, flightResult.getOrNull()!!.price, 0.01)

        // --- Get seat map ---
        val mockSeats = listOf(
            SeatResponse(id = 10, flightId = 1, row = 1, column = "A", seatClass = "economy", isAvailable = true),
            SeatResponse(id = 11, flightId = 1, row = 1, column = "B", seatClass = "economy", isAvailable = true)
        )
        coEvery { flightApi.getSeatMap(1) } returns mockSeats

        val seatResult = flightRepo.getSeatMap(1)
        assertTrue(seatResult.isSuccess)
        assertEquals(2, seatResult.getOrNull()!!.size)

        // --- Create booking ---
        val requestSlot = slot<CreateBookingRequest>()
        coEvery { bookingApi.createBooking(capture(requestSlot)) } returns mockBookingResponse()

        val bookResult = bookingRepo.createBooking(
            flightId = 1,
            passengers = listOf(
                com.flightbooking.app.domain.repository.PassengerInput("John Doe", "john@test.com", 10)
            )
        )
        assertTrue(bookResult.isSuccess)
        val booking = bookResult.getOrNull()!!
        assertEquals(BookingStatus.CONFIRMED, booking.status)
        assertEquals(PaymentStatus.PAID, booking.paymentStatus)
        assertEquals("1A", booking.passengers[0].seatAssignment)
        assertEquals(1, requestSlot.captured.flightId)
        assertEquals("John Doe", requestSlot.captured.passengers[0].name)
        assertEquals(10, requestSlot.captured.passengers[0].seatId)

        // --- View booking ---
        coEvery { bookingApi.getBookingById(1) } returns mockBookingResponse()

        val viewResult = bookingRepo.getBookingById(1)
        assertTrue(viewResult.isSuccess)
        assertEquals("FB101", viewResult.getOrNull()!!.flight.flightNumber)

        // --- List bookings ---
        coEvery { bookingApi.getBookings() } returns listOf(mockBookingResponse())

        val listResult = bookingRepo.getBookings()
        assertTrue(listResult.isSuccess)
        assertEquals(1, listResult.getOrNull()!!.size)

        // --- Cancel booking ---
        coEvery { bookingApi.cancelBooking(1) } returns mockBookingResponse(
            status = "cancelled", paymentStatus = "refunded"
        )

        val cancelResult = bookingRepo.cancelBooking(1)
        assertTrue(cancelResult.isSuccess)
        assertEquals(BookingStatus.CANCELLED, cancelResult.getOrNull()!!.status)
        assertEquals(PaymentStatus.REFUNDED, cancelResult.getOrNull()!!.paymentStatus)
    }

    @Test
    fun `booking with multiple passengers calculates correct total`() = runTest {
        val multiPassengerResponse = BookingResponse(
            id = 2, userId = 1, flightId = 1,
            status = "confirmed", totalPrice = 960.0,
            paymentStatus = "paid",
            passengers = listOf(
                PassengerResponse(1, 2, "Alice", "alice@test.com", "1A"),
                PassengerResponse(2, 2, "Bob", "bob@test.com", "1B"),
                PassengerResponse(3, 2, "Carol", "carol@test.com", "1C")
            ),
            createdAt = "2026-03-10T08:00:00",
            flightNumber = "FB101",
            departureAirport = "JFK - New York",
            arrivalAirport = "LAX - Los Angeles",
            departureTime = "2026-04-01T08:00:00",
            arrivalTime = "2026-04-01T13:30:00"
        )
        coEvery { bookingApi.createBooking(any()) } returns multiPassengerResponse

        val result = bookingRepo.createBooking(
            flightId = 1,
            passengers = listOf(
                com.flightbooking.app.domain.repository.PassengerInput("Alice", "alice@test.com", 10),
                com.flightbooking.app.domain.repository.PassengerInput("Bob", "bob@test.com", 11),
                com.flightbooking.app.domain.repository.PassengerInput("Carol", "carol@test.com", 12),
            )
        )
        assertTrue(result.isSuccess)
        val booking = result.getOrNull()!!
        assertEquals(960.0, booking.totalPrice, 0.01)
        assertEquals(3, booking.passengers.size)
        val seats = booking.passengers.map { it.seatAssignment }.filterNotNull().toSet()
        assertEquals(3, seats.size)
    }

    @Test
    fun `network error during booking returns error result`() = runTest {
        coEvery { bookingApi.createBooking(any()) } throws RuntimeException("Network unreachable")

        val result = bookingRepo.createBooking(
            flightId = 1,
            passengers = listOf(
                com.flightbooking.app.domain.repository.PassengerInput("Test", "test@test.com", null)
            )
        )
        assertTrue(result.isError)
        val error = result as Result.Error
        assertTrue(error.message.contains("Network unreachable"))
    }

    @Test
    fun `login failure returns error and does not store token`() = runTest {
        coEvery { authApi.login(any()) } throws RuntimeException("401 Unauthorized")

        val result = authRepo.login("bad@test.com", "wrong")
        assertTrue(result.isError)
        coVerify(exactly = 0) { tokenManager.saveToken(any()) }
    }
}
