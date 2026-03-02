package com.flightbooking.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flightbooking.app.domain.model.Notification

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "user_id") val userId: Int,
    val title: String,
    val message: String,
    @ColumnInfo(name = "is_read") val isRead: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String
) {
    fun toDomain(): Notification = Notification(
        id = id,
        userId = userId,
        title = title,
        message = message,
        isRead = isRead,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(notification: Notification): NotificationEntity = NotificationEntity(
            id = notification.id,
            userId = notification.userId,
            title = notification.title,
            message = notification.message,
            isRead = notification.isRead,
            createdAt = notification.createdAt
        )
    }
}
