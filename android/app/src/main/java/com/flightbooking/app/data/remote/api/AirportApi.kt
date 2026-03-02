package com.flightbooking.app.data.remote.api

import com.flightbooking.app.data.remote.dto.AirportResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface AirportApi {

    @GET("airports")
    suspend fun getAirports(): List<AirportResponse>

    @GET("airports/{id}")
    suspend fun getAirportById(@Path("id") airportId: Int): AirportResponse
}
