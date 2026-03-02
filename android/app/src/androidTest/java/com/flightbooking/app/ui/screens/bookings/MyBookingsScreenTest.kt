package com.flightbooking.app.ui.screens.bookings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.flightbooking.app.domain.model.Airport
import com.flightbooking.app.domain.model.Booking
import com.flightbooking.app.domain.model.BookingStatus
import com.flightbooking.app.domain.model.Flight
import com.flightbooking.app.domain.model.FlightStatus
import com.flightbooking.app.domain.model.Passenger
import com.flightbooking.app.domain.model.PaymentStatus
import com.flightbooking.app.domain.repository.BookingRepository
import com.flightbooking.app.domain.util.Result
import com.flightbooking.app.ui.theme.FlightBookingTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class MyBookingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val bookingRepository: BookingRepository = mockk()

    private val testAirportJFK = Airport(1, "JFK", "JFK International", "New York", "USA", 40.6, -73.7)
    private val testAirportLAX = Airport(2, "LAX", "LAX International", "Los Angeles", "USA", 33.9, -118.4)
    private val testFlight = Flight(
        1, "FL100", testAirportJFK, testAirportLAX,
        "2026-03-15T10:00:00", "2026-03-15T13:00:00",
        299.99, 180, FlightStatus.SCHEDULED
    )
    private val testPassenger = Passenger(1, 1, "John Doe", "john@test.com", "12A")

    private fun setupScreen(
        onNavigateBack: () -> Unit = {},
        onBookingSelected: (Int) -> Unit = {}
    ) {
        val viewModel = MyBookingsViewModel(bookingRepository)
        composeTestRule.setContent {
            FlightBookingTheme {
                MyBookingsScreen(
                    onNavigateBack = onNavigateBack,
                    onBookingSelected = onBookingSelected,
                    viewModel = viewModel
                )
            }
        }
    }

    @Test
    fun myBookingsScreen_displaysTitle() {
        coEvery { bookingRepository.getBookings() } returns Result.Success(emptyList())
        setupScreen()
        composeTestRule.onNodeWithText("My Bookings").assertIsDisplayed()
    }

    @Test
    fun myBookingsScreen_emptyStateShowsMessage() {
        coEvery { bookingRepository.getBookings() } returns Result.Success(emptyList())
        setupScreen()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No bookings yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your booked flights will appear here").assertIsDisplayed()
    }

    @Test
    fun myBookingsScreen_displaysBookingCards() {
        val bookings = listOf(
            Booking(1, 1, testFlight, BookingStatus.CONFIRMED, 299.99, PaymentStatus.PAID, listOf(testPassenger), "2026-03-10")
        )
        coEvery { bookingRepository.getBookings() } returns Result.Success(bookings)
        setupScreen()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("FL100").assertIsDisplayed()
        composeTestRule.onNodeWithText("JFK").assertIsDisplayed()
        composeTestRule.onNodeWithText("LAX").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirmed").assertIsDisplayed()
    }

    @Test
    fun myBookingsScreen_displaysPrice() {
        val bookings = listOf(
            Booking(1, 1, testFlight, BookingStatus.CONFIRMED, 299.99, PaymentStatus.PAID, listOf(testPassenger), "2026-03-10")
        )
        coEvery { bookingRepository.getBookings() } returns Result.Success(bookings)
        setupScreen()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("$299.99").assertIsDisplayed()
    }

    @Test
    fun myBookingsScreen_errorStateDisplaysMessage() {
        coEvery { bookingRepository.getBookings() } returns Result.Error("Network error")
        setupScreen()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Network error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun myBookingsScreen_clickBookingCallsCallback() {
        val bookings = listOf(
            Booking(42, 1, testFlight, BookingStatus.CONFIRMED, 299.99, PaymentStatus.PAID, listOf(testPassenger), "2026-03-10")
        )
        coEvery { bookingRepository.getBookings() } returns Result.Success(bookings)
        var selectedBookingId: Int? = null
        setupScreen(onBookingSelected = { selectedBookingId = it })
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("FL100").performClick()
        assert(selectedBookingId == 42)
    }

    @Test
    fun myBookingsScreen_displaysCancelledStatus() {
        val bookings = listOf(
            Booking(1, 1, testFlight, BookingStatus.CANCELLED, 299.99, PaymentStatus.REFUNDED, listOf(testPassenger), "2026-03-10")
        )
        coEvery { bookingRepository.getBookings() } returns Result.Success(bookings)
        setupScreen()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Cancelled").assertIsDisplayed()
        composeTestRule.onNodeWithText("Refunded").assertIsDisplayed()
    }

    @Test
    fun myBookingsScreen_displaysPassengerCount() {
        val twoPassengers = listOf(
            Passenger(1, 1, "John", "john@test.com", "12A"),
            Passenger(2, 1, "Jane", "jane@test.com", "12B")
        )
        val bookings = listOf(
            Booking(1, 1, testFlight, BookingStatus.CONFIRMED, 599.98, PaymentStatus.PAID, twoPassengers, "2026-03-10")
        )
        coEvery { bookingRepository.getBookings() } returns Result.Success(bookings)
        setupScreen()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("2 passengers").assertIsDisplayed()
    }
}
