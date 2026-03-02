package com.flightbooking.app.data.remote.dto

import com.flightbooking.app.domain.model.Flight
import com.flightbooking.app.domain.model.FlightStatus
import com.google.gson.annotations.SerializedName

data class FlightResponse(
    val id: Int,
    @SerializedName("flight_number") val flightNumber: String,
    @SerializedName("departure_airport") val departureAirport: AirportResponse,
    @SerializedName("arrival_airport") val arrivalAirport: AirportResponse,
    @SerializedName("departure_time") val departureTime: String,
    @SerializedName("arrival_time") val arrivalTime: String,
    val price: Double,
    @SerializedName("total_seats") val totalSeats: Int,
    val status: String
) {
    fun toDomain(): Flight = Flight(
        id = id,
        flightNumber = flightNumber,
        departureAirport = departureAirport.toDomain(),
        arrivalAirport = arrivalAirport.toDomain(),
        departureTime = departureTime,
        arrivalTime = arrivalTime,
        price = price,
        totalSeats = totalSeats,
        status = when (status) {
            "delayed" -> FlightStatus.DELAYED
            "cancelled" -> FlightStatus.CANCELLED
            "completed" -> FlightStatus.COMPLETED
            else -> FlightStatus.SCHEDULED
        }
    )
}

data class FlightSearchParams(
    @SerializedName("departure_airport") val departureAirport: String?,
    @SerializedName("arrival_airport") val arrivalAirport: String?,
    val date: String?,
    val passengers: Int?
)
