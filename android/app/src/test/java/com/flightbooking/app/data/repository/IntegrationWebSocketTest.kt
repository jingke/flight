package com.flightbooking.app.data.repository

import com.flightbooking.app.data.remote.WebSocketClient
import com.flightbooking.app.data.remote.api.NotificationApi
import com.flightbooking.app.data.remote.dto.NotificationResponse
import com.flightbooking.app.domain.model.Notification
import com.flightbooking.app.domain.util.Result
import com.flightbooking.app.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Integration tests for WebSocket notification delivery on Android.
 *
 * These tests validate that the NotificationRepository correctly:
 * 1. Delegates WebSocket lifecycle to WebSocketClient
 * 2. Exposes real-time notifications via Flow
 * 3. Falls back to REST API for notification history
 * 4. Handles both WebSocket and REST notifications consistently
 *
 * This mirrors the same notification contract tested in the backend
 * WebSocket integration tests and the web frontend.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationWebSocketTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var notificationApi: NotificationApi
    private lateinit var webSocketClient: WebSocketClient
    private lateinit var notificationRepo: NotificationRepositoryImpl

    @Before
    fun setup() {
        notificationApi = mockk()
        webSocketClient = mockk(relaxed = true)
        notificationRepo = NotificationRepositoryImpl(notificationApi, webSocketClient)
    }

    @Test
    fun `websocket delivers real-time notifications as domain objects`() = runTest {
        val wsNotification = Notification(
            id = 1, userId = 1,
            title = "Booking Confirmed",
            message = "Your booking for flight FB101 has been confirmed.",
            isRead = false, createdAt = "2026-03-10T08:00:00"
        )
        every { webSocketClient.notifications } returns flowOf(wsNotification)

        val received = notificationRepo.observeNotifications().first()
        assertEquals("Booking Confirmed", received.title)
        assertEquals(1, received.userId)
    }

    @Test
    fun `websocket and rest api return consistent notification data`() = runTest {
        val restNotification = NotificationResponse(
            id = 1, userId = 1,
            title = "Booking Confirmed",
            message = "Your booking confirmed",
            isRead = false, createdAt = "2026-03-10T08:00:00"
        )
        coEvery { notificationApi.getNotifications() } returns listOf(restNotification)

        val wsNotification = Notification(
            id = 1, userId = 1,
            title = "Booking Confirmed",
            message = "Your booking confirmed",
            isRead = false, createdAt = "2026-03-10T08:00:00"
        )
        every { webSocketClient.notifications } returns flowOf(wsNotification)

        val restResult = notificationRepo.getNotifications()
        assertTrue(restResult.isSuccess)
        val restData = restResult.getOrNull()!!.first()

        val wsData = notificationRepo.observeNotifications().first()

        assertEquals(restData.id, wsData.id)
        assertEquals(restData.title, wsData.title)
        assertEquals(restData.userId, wsData.userId)
    }

    @Test
    fun `multiple websocket notifications stream in order`() = runTest {
        val flow = MutableSharedFlow<Notification>()
        every { webSocketClient.notifications } returns flow

        val collected = mutableListOf<Notification>()
        val job = launch {
            notificationRepo.observeNotifications().collect { collected.add(it) }
        }

        flow.emit(Notification(1, 1, "First", "First msg", false, "2026-03-10T08:00:00"))
        flow.emit(Notification(2, 1, "Second", "Second msg", false, "2026-03-10T08:01:00"))
        flow.emit(Notification(3, 1, "Third", "Third msg", false, "2026-03-10T08:02:00"))
        advanceUntilIdle()

        assertEquals(3, collected.size)
        assertEquals("First", collected[0].title)
        assertEquals("Second", collected[1].title)
        assertEquals("Third", collected[2].title)

        job.cancel()
    }

    @Test
    fun `connect and disconnect lifecycle is managed correctly`() {
        notificationRepo.connectWebSocket("jwt-token-abc")
        verify { webSocketClient.connect("jwt-token-abc") }

        notificationRepo.disconnectWebSocket()
        verify { webSocketClient.disconnect() }
    }

    @Test
    fun `marking notification read via REST updates read status`() = runTest {
        val readNotif = NotificationResponse(
            id = 5, userId = 1,
            title = "Booking Cancelled",
            message = "Your booking was cancelled",
            isRead = true, createdAt = "2026-03-10T09:00:00"
        )
        coEvery { notificationApi.markAsRead(5) } returns readNotif

        val result = notificationRepo.markAsRead(5)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isRead)
    }

    @Test
    fun `rest api error for notifications returns error result`() = runTest {
        coEvery { notificationApi.getNotifications() } throws RuntimeException("Server error")

        val result = notificationRepo.getNotifications()
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("Server error"))
    }

    @Test
    fun `notifications from multiple events arrive via websocket`() = runTest {
        val flow = MutableSharedFlow<Notification>()
        every { webSocketClient.notifications } returns flow

        val collected = mutableListOf<String>()
        val job = launch {
            notificationRepo.observeNotifications().collect { collected.add(it.title) }
        }

        flow.emit(Notification(1, 1, "Booking Confirmed", "Booked", false, "2026-03-10T08:00:00"))
        flow.emit(Notification(2, 1, "Complaint Updated", "Resolved", false, "2026-03-10T08:05:00"))
        flow.emit(Notification(3, 1, "Modification Request Updated", "Approved", false, "2026-03-10T08:10:00"))
        flow.emit(Notification(4, 1, "Points Redeemed", "Redeemed 100 pts", false, "2026-03-10T08:15:00"))
        advanceUntilIdle()

        assertEquals(4, collected.size)
        assertTrue(collected.contains("Booking Confirmed"))
        assertTrue(collected.contains("Complaint Updated"))
        assertTrue(collected.contains("Modification Request Updated"))
        assertTrue(collected.contains("Points Redeemed"))

        job.cancel()
    }
}
