package com.flightbooking.app.data.remote.api

import com.flightbooking.app.data.remote.dto.BookingResponse
import com.flightbooking.app.data.remote.dto.CreateBookingRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BookingApi {

    @POST("bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): BookingResponse

    @GET("bookings")
    suspend fun getBookings(): List<BookingResponse>

    @GET("bookings/{id}")
    suspend fun getBookingById(@Path("id") bookingId: Int): BookingResponse

    @POST("bookings/{id}/cancel")
    suspend fun cancelBooking(@Path("id") bookingId: Int): BookingResponse
}
