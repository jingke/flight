package com.flightbooking.app.domain.repository

import com.flightbooking.app.domain.model.Flight
import com.flightbooking.app.domain.model.Seat
import com.flightbooking.app.domain.util.Result

interface FlightRepository {
    suspend fun searchFlights(
        origin: String?,
        destination: String?,
        date: String?
    ): Result<List<Flight>>

    suspend fun getFlightById(flightId: Int): Result<Flight>
    suspend fun getSeatMap(flightId: Int): Result<List<Seat>>
}
