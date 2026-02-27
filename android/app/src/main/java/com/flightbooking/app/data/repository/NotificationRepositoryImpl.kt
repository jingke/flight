package com.flightbooking.app.data.repository

import com.flightbooking.app.data.remote.WebSocketClient
import com.flightbooking.app.data.remote.api.NotificationApi
import com.flightbooking.app.domain.model.Notification
import com.flightbooking.app.domain.repository.NotificationRepository
import com.flightbooking.app.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val notificationApi: NotificationApi,
    private val webSocketClient: WebSocketClient
) : NotificationRepository {

    override suspend fun getNotifications(): Result<List<Notification>> {
        return try {
            val response = notificationApi.getNotifications()
            Result.Success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to get notifications", exception = e)
        }
    }

    override suspend fun markAsRead(notificationId: Int): Result<Notification> {
        return try {
            val response = notificationApi.markAsRead(notificationId)
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to mark notification as read", exception = e)
        }
    }

    override fun observeNotifications(): Flow<Notification> = webSocketClient.notifications

    override fun connectWebSocket(token: String) = webSocketClient.connect(token)

    override fun disconnectWebSocket() = webSocketClient.disconnect()
}
