package com.flightbooking.app.domain.model

data class Flight(
    val id: Int,
    val flightNumber: String,
    val departureAirport: Airport,
    val arrivalAirport: Airport,
    val departureTime: String,
    val arrivalTime: String,
    val price: Double,
    val totalSeats: Int,
    val status: FlightStatus
)

enum class FlightStatus {
    SCHEDULED,
    DELAYED,
    CANCELLED,
    COMPLETED
}
