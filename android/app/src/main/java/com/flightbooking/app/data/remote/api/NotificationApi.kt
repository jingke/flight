package com.flightbooking.app.data.remote.api

import com.flightbooking.app.data.remote.dto.NotificationResponse
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface NotificationApi {

    @GET("notifications")
    suspend fun getNotifications(): List<NotificationResponse>

    @PUT("notifications/{id}/read")
    suspend fun markAsRead(@Path("id") notificationId: Int): NotificationResponse
}
