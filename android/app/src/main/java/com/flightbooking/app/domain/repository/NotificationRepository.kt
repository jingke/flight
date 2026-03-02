package com.flightbooking.app.domain.repository

import com.flightbooking.app.domain.model.Notification
import com.flightbooking.app.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun getNotifications(): Result<List<Notification>>
    suspend fun markAsRead(notificationId: Int): Result<Notification>
    fun observeNotifications(): Flow<Notification>
    fun connectWebSocket(token: String)
    fun disconnectWebSocket()
}
