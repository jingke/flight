package com.flightbooking.app.data.remote.dto

import com.flightbooking.app.domain.model.Notification
import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    val title: String,
    val message: String,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String
) {
    fun toDomain(): Notification = Notification(
        id = id,
        userId = userId,
        title = title,
        message = message,
        isRead = isRead,
        createdAt = createdAt
    )
}
