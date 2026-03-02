package com.flightbooking.app.domain.repository

import com.flightbooking.app.domain.model.Airport
import com.flightbooking.app.domain.util.Result

interface AirportRepository {
    suspend fun getAirports(): Result<List<Airport>>
    suspend fun getAirportById(airportId: Int): Result<Airport>
}
