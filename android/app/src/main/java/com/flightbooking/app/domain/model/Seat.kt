package com.flightbooking.app.domain.model

data class Seat(
    val id: Int,
    val flightId: Int,
    val row: Int,
    val column: String,
    val seatClass: SeatClass,
    val isAvailable: Boolean
)

enum class SeatClass {
    ECONOMY,
    BUSINESS,
    FIRST
}
