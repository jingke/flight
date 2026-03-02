package com.flightbooking.app.data.remote.dto

import com.flightbooking.app.domain.model.Complaint
import com.flightbooking.app.domain.model.ComplaintStatus
import com.google.gson.annotations.SerializedName

data class ComplaintResponse(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("booking_id") val bookingId: Int?,
    val subject: String,
    val description: String,
    val status: String,
    @SerializedName("admin_response") val adminResponse: String?,
    @SerializedName("created_at") val createdAt: String
) {
    fun toDomain(): Complaint = Complaint(
        id = id,
        userId = userId,
        bookingId = bookingId,
        subject = subject,
        description = description,
        status = when (status) {
            "in_progress" -> ComplaintStatus.IN_PROGRESS
            "resolved" -> ComplaintStatus.RESOLVED
            "closed" -> ComplaintStatus.CLOSED
            else -> ComplaintStatus.OPEN
        },
        adminResponse = adminResponse,
        createdAt = createdAt
    )
}

data class CreateComplaintRequest(
    @SerializedName("booking_id") val bookingId: Int?,
    val subject: String,
    val description: String
)
