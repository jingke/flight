package com.flightbooking.app.data.remote.dto

import com.flightbooking.app.domain.model.Airport

data class AirportResponse(
    val id: Int,
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
) {
    fun toDomain(): Airport = Airport(
        id = id,
        code = code,
        name = name,
        city = city,
        country = country,
        latitude = latitude,
        longitude = longitude
    )
}
