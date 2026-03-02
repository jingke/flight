package com.flightbooking.app.data.repository

import com.flightbooking.app.data.remote.api.FlightApi
import com.flightbooking.app.domain.model.Flight
import com.flightbooking.app.domain.model.Seat
import com.flightbooking.app.domain.repository.FlightRepository
import com.flightbooking.app.domain.util.Result
import javax.inject.Inject

class FlightRepositoryImpl @Inject constructor(
    private val flightApi: FlightApi
) : FlightRepository {

    override suspend fun searchFlights(
        origin: String?,
        destination: String?,
        date: String?
    ): Result<List<Flight>> {
        return try {
            val response = flightApi.searchFlights(origin, destination, date)
            Result.Success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to search flights", exception = e)
        }
    }

    override suspend fun getFlightById(flightId: Int): Result<Flight> {
        return try {
            val response = flightApi.getFlightById(flightId)
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to get flight", exception = e)
        }
    }

    override suspend fun getSeatMap(flightId: Int): Result<List<Seat>> {
        return try {
            val response = flightApi.getSeatMap(flightId)
            Result.Success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to get seat map", exception = e)
        }
    }
}
