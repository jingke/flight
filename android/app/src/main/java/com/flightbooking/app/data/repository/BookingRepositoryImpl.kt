package com.flightbooking.app.data.repository

import com.flightbooking.app.data.remote.api.BookingApi
import com.flightbooking.app.data.remote.dto.CreateBookingRequest
import com.flightbooking.app.data.remote.dto.PassengerRequest
import com.flightbooking.app.domain.model.Booking
import com.flightbooking.app.domain.repository.BookingRepository
import com.flightbooking.app.domain.repository.PassengerInput
import com.flightbooking.app.domain.util.Result
import javax.inject.Inject

class BookingRepositoryImpl @Inject constructor(
    private val bookingApi: BookingApi
) : BookingRepository {

    override suspend fun createBooking(
        flightId: Int,
        passengers: List<PassengerInput>
    ): Result<Booking> {
        return try {
            val request = CreateBookingRequest(
                flightId = flightId,
                passengers = passengers.map { PassengerRequest(it.name, it.email, it.seatId) }
            )
            val response = bookingApi.createBooking(request)
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to create booking", exception = e)
        }
    }

    override suspend fun getBookings(): Result<List<Booking>> {
        return try {
            val response = bookingApi.getBookings()
            Result.Success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to get bookings", exception = e)
        }
    }

    override suspend fun getBookingById(bookingId: Int): Result<Booking> {
        return try {
            val response = bookingApi.getBookingById(bookingId)
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to get booking", exception = e)
        }
    }

    override suspend fun cancelBooking(bookingId: Int): Result<Booking> {
        return try {
            val response = bookingApi.cancelBooking(bookingId)
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to cancel booking", exception = e)
        }
    }
}
