package com.flightbooking.app.domain.model

data class Booking(
    val id: Int,
    val userId: Int,
    val flight: Flight,
    val status: BookingStatus,
    val totalPrice: Double,
    val paymentStatus: PaymentStatus,
    val passengers: List<Passenger>,
    val createdAt: String
)

enum class BookingStatus {
    CONFIRMED,
    CANCELLED,
    PENDING
}

enum class PaymentStatus {
    PAID,
    PENDING,
    REFUNDED
}
