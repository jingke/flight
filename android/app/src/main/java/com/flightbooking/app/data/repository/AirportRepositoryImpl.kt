package com.flightbooking.app.data.repository

import com.flightbooking.app.data.local.dao.AirportDao
import com.flightbooking.app.data.local.entity.AirportEntity
import com.flightbooking.app.data.remote.api.AirportApi
import com.flightbooking.app.domain.model.Airport
import com.flightbooking.app.domain.repository.AirportRepository
import com.flightbooking.app.domain.util.Result
import javax.inject.Inject

class AirportRepositoryImpl @Inject constructor(
    private val airportApi: AirportApi,
    private val airportDao: AirportDao
) : AirportRepository {

    override suspend fun getAirports(): Result<List<Airport>> {
        return try {
            val response = airportApi.getAirports()
            val airports = response.map { it.toDomain() }
            airportDao.insertAll(airports.map { AirportEntity.fromDomain(it) })
            Result.Success(airports)
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to get airports", exception = e)
        }
    }

    override suspend fun getAirportById(airportId: Int): Result<Airport> {
        return try {
            val response = airportApi.getAirportById(airportId)
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to get airport", exception = e)
        }
    }
}
