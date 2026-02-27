package com.flightbooking.app.domain.repository

import com.flightbooking.app.domain.model.Booking
import com.flightbooking.app.domain.util.Result

interface BookingRepository {
    suspend fun createBooking(
        flightId: Int,
        passengers: List<PassengerInput>
    ): Result<Booking>

    suspend fun getBookings(): Result<List<Booking>>
    suspend fun getBookingById(bookingId: Int): Result<Booking>
    suspend fun cancelBooking(bookingId: Int): Result<Booking>
}

data class PassengerInput(
    val name: String,
    val email: String,
    val seatId: Int?
)
