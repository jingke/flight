package com.flightbooking.app.data.remote.api

import com.flightbooking.app.data.remote.dto.FlightResponse
import com.flightbooking.app.data.remote.dto.SeatResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FlightApi {

    @GET("flights/search")
    suspend fun searchFlights(
        @Query("origin") origin: String?,
        @Query("destination") destination: String?,
        @Query("date") date: String?
    ): List<FlightResponse>

    @GET("flights/{id}")
    suspend fun getFlightById(@Path("id") flightId: Int): FlightResponse

    @GET("seats/flight/{id}")
    suspend fun getSeatMap(@Path("id") flightId: Int): List<SeatResponse>
}
