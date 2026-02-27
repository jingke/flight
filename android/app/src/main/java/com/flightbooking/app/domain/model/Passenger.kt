package com.flightbooking.app.domain.model

data class Passenger(
    val id: Int,
    val bookingId: Int,
    val name: String,
    val email: String,
    val seatAssignment: String?
)
