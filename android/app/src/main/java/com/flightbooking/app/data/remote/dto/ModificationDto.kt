package com.flightbooking.app.data.remote.dto

import com.flightbooking.app.domain.model.ModificationRequest
import com.flightbooking.app.domain.model.ModificationStatus
import com.flightbooking.app.domain.model.ModificationType
import com.google.gson.annotations.SerializedName

data class ModificationResponse(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("booking_id") val bookingId: Int,
    val type: String,
    val details: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String
) {
    fun toDomain(): ModificationRequest = ModificationRequest(
        id = id,
        userId = userId,
        bookingId = bookingId,
        type = when (type) {
            "seat_change" -> ModificationType.SEAT_CHANGE
            "passenger_change" -> ModificationType.PASSENGER_CHANGE
            "cancellation" -> ModificationType.CANCELLATION
            else -> ModificationType.DATE_CHANGE
        },
        details = details,
        status = when (status) {
            "approved" -> ModificationStatus.APPROVED
            "rejected" -> ModificationStatus.REJECTED
            else -> ModificationStatus.PENDING
        },
        createdAt = createdAt
    )
}

data class CreateModificationRequest(
    @SerializedName("booking_id") val bookingId: Int,
    val type: String,
    val details: String
)
