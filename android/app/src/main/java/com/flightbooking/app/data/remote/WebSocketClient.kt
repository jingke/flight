package com.flightbooking.app.data.remote

import com.flightbooking.app.BuildConfig
import com.flightbooking.app.data.remote.dto.NotificationResponse
import com.flightbooking.app.domain.model.Notification
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private val notificationChannel = Channel<Notification>(Channel.BUFFERED)

    val notifications: Flow<Notification> = notificationChannel.receiveAsFlow()

    fun connect(token: String) {
        disconnect()
        val request = Request.Builder()
            .url("${BuildConfig.WS_URL}api/notifications/ws?token=$token")
            .build()
        webSocket = okHttpClient.newWebSocket(request, createListener())
    }

    fun disconnect() {
        webSocket?.close(NORMAL_CLOSURE_STATUS, "Client disconnect")
        webSocket = null
    }

    private fun createListener(): WebSocketListener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            val response = gson.fromJson(text, NotificationResponse::class.java)
            notificationChannel.trySend(response.toDomain())
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            // Will be handled by reconnection logic in the repository layer
        }
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }
}
