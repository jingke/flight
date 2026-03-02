package com.flightbooking.app.domain.model

data class ModificationRequest(
    val id: Int,
    val userId: Int,
    val bookingId: Int,
    val type: ModificationType,
    val details: String,
    val status: ModificationStatus,
    val createdAt: String
)

enum class ModificationType {
    DATE_CHANGE,
    SEAT_CHANGE,
    PASSENGER_CHANGE,
    CANCELLATION
}

enum class ModificationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
