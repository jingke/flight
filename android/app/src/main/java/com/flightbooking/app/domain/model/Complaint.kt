package com.flightbooking.app.domain.model

data class Complaint(
    val id: Int,
    val userId: Int,
    val bookingId: Int?,
    val subject: String,
    val description: String,
    val status: ComplaintStatus,
    val adminResponse: String?,
    val createdAt: String
)

enum class ComplaintStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
