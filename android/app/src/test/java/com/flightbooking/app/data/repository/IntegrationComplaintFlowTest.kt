package com.flightbooking.app.data.repository

import com.flightbooking.app.data.remote.api.ComplaintApi
import com.flightbooking.app.data.remote.api.LoyaltyApi
import com.flightbooking.app.data.remote.api.ModificationApi
import com.flightbooking.app.data.remote.api.NotificationApi
import com.flightbooking.app.data.remote.WebSocketClient
import com.flightbooking.app.data.remote.dto.ComplaintResponse
import com.flightbooking.app.data.remote.dto.CreateComplaintRequest
import com.flightbooking.app.data.remote.dto.CreateModificationRequest
import com.flightbooking.app.data.remote.dto.LoyaltyResponse
import com.flightbooking.app.data.remote.dto.ModificationResponse
import com.flightbooking.app.data.remote.dto.NotificationResponse
import com.flightbooking.app.data.remote.dto.RedeemPointsRequest
import com.flightbooking.app.domain.model.ComplaintStatus
import com.flightbooking.app.domain.model.ModificationStatus
import com.flightbooking.app.domain.model.Notification
import com.flightbooking.app.domain.util.Result
import com.flightbooking.app.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Integration tests for complaint, modification, loyalty, and notification
 * workflows through the Android repository layer. These workflows mirror
 * the same API contract validated by the backend and web frontend tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationComplaintFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var complaintApi: ComplaintApi
    private lateinit var modificationApi: ModificationApi
    private lateinit var loyaltyApi: LoyaltyApi
    private lateinit var notificationApi: NotificationApi
    private lateinit var webSocketClient: WebSocketClient

    private lateinit var complaintRepo: ComplaintRepositoryImpl
    private lateinit var modificationRepo: ModificationRepositoryImpl
    private lateinit var loyaltyRepo: LoyaltyRepositoryImpl
    private lateinit var notificationRepo: NotificationRepositoryImpl

    @Before
    fun setup() {
        complaintApi = mockk()
        modificationApi = mockk()
        loyaltyApi = mockk()
        notificationApi = mockk()
        webSocketClient = mockk(relaxed = true)

        complaintRepo = ComplaintRepositoryImpl(complaintApi)
        modificationRepo = ModificationRepositoryImpl(modificationApi)
        loyaltyRepo = LoyaltyRepositoryImpl(loyaltyApi)
        notificationRepo = NotificationRepositoryImpl(notificationApi, webSocketClient)
    }

    // -----------------------------------------------------------------------
    // Complaint lifecycle
    // -----------------------------------------------------------------------

    @Test
    fun `complaint lifecycle - submit, list, view`() = runTest {
        val complaintResponse = ComplaintResponse(
            id = 1, userId = 1, bookingId = 1,
            subject = "Delayed flight", description = "2 hour delay",
            status = "open", adminResponse = null,
            createdAt = "2026-03-10T10:00:00"
        )
        val requestSlot = slot<CreateComplaintRequest>()
        coEvery { complaintApi.submitComplaint(capture(requestSlot)) } returns complaintResponse

        val submitResult = complaintRepo.submitComplaint(1, "Delayed flight", "2 hour delay")
        assertTrue(submitResult.isSuccess)
        val complaint = submitResult.getOrNull()!!
        assertEquals(ComplaintStatus.OPEN, complaint.status)
        assertEquals("Delayed flight", complaint.subject)
        assertEquals(1, requestSlot.captured.bookingId)

        coEvery { complaintApi.getComplaints() } returns listOf(complaintResponse)
        val listResult = complaintRepo.getComplaints()
        assertTrue(listResult.isSuccess)
        assertEquals(1, listResult.getOrNull()!!.size)

        val resolvedResponse = complaintResponse.copy(
            status = "resolved", adminResponse = "We apologize. Compensation issued."
        )
        coEvery { complaintApi.getComplaintById(1) } returns resolvedResponse
        val viewResult = complaintRepo.getComplaintById(1)
        assertTrue(viewResult.isSuccess)
        assertEquals(ComplaintStatus.RESOLVED, viewResult.getOrNull()!!.status)
        assertEquals("We apologize. Compensation issued.", viewResult.getOrNull()!!.adminResponse)
    }

    @Test
    fun `complaint submission fails gracefully on network error`() = runTest {
        coEvery { complaintApi.submitComplaint(any()) } throws RuntimeException("Connection timeout")

        val result = complaintRepo.submitComplaint(1, "Test", "Test")
        assertTrue(result.isError)
    }

    // -----------------------------------------------------------------------
    // Modification request lifecycle
    // -----------------------------------------------------------------------

    @Test
    fun `modification lifecycle - request, list, check result`() = runTest {
        val modResponse = ModificationResponse(
            id = 1, userId = 1, bookingId = 1,
            type = "date_change", details = "Move to April 20",
            status = "pending", createdAt = "2026-03-10T10:00:00"
        )
        val requestSlot = slot<CreateModificationRequest>()
        coEvery { modificationApi.createModification(capture(requestSlot)) } returns modResponse

        val createResult = modificationRepo.createModification(1, "date_change", "Move to April 20")
        assertTrue(createResult.isSuccess)
        assertEquals(ModificationStatus.PENDING, createResult.getOrNull()!!.status)
        assertEquals(1, requestSlot.captured.bookingId)
        assertEquals("date_change", requestSlot.captured.type)

        val approvedMod = modResponse.copy(status = "approved")
        coEvery { modificationApi.getModifications() } returns listOf(approvedMod)
        val listResult = modificationRepo.getModifications()
        assertTrue(listResult.isSuccess)
        assertEquals(ModificationStatus.APPROVED, listResult.getOrNull()!![0].status)
    }

    @Test
    fun `modification request rejected shows correct status`() = runTest {
        val rejectedMod = ModificationResponse(
            id = 1, userId = 1, bookingId = 1,
            type = "seat_change", details = "Want first class",
            status = "rejected", createdAt = "2026-03-10T10:00:00"
        )
        coEvery { modificationApi.getModifications() } returns listOf(rejectedMod)

        val result = modificationRepo.getModifications()
        assertTrue(result.isSuccess)
        assertEquals(ModificationStatus.REJECTED, result.getOrNull()!![0].status)
    }

    // -----------------------------------------------------------------------
    // Loyalty points journey
    // -----------------------------------------------------------------------

    @Test
    fun `loyalty lifecycle - check balance, redeem, verify`() = runTest {
        val initialLoyalty = LoyaltyResponse(
            id = 1, userId = 1, earned = 500, redeemed = 0, balance = 500
        )
        coEvery { loyaltyApi.getLoyaltyPoints() } returns initialLoyalty

        val getResult = loyaltyRepo.getLoyaltyPoints()
        assertTrue(getResult.isSuccess)
        assertEquals(500, getResult.getOrNull()!!.balance)
        assertEquals(500, getResult.getOrNull()!!.earned)

        val afterRedeem = LoyaltyResponse(
            id = 1, userId = 1, earned = 500, redeemed = 100, balance = 400
        )
        val redeemSlot = slot<RedeemPointsRequest>()
        coEvery { loyaltyApi.redeemPoints(capture(redeemSlot)) } returns afterRedeem

        val redeemResult = loyaltyRepo.redeemPoints(100)
        assertTrue(redeemResult.isSuccess)
        assertEquals(400, redeemResult.getOrNull()!!.balance)
        assertEquals(100, redeemResult.getOrNull()!!.redeemed)
        assertEquals(100, redeemSlot.captured.points)
    }

    @Test
    fun `redeem more points than balance returns API error`() = runTest {
        coEvery { loyaltyApi.redeemPoints(any()) } throws
            RuntimeException("Insufficient loyalty points")

        val result = loyaltyRepo.redeemPoints(9999)
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("Insufficient"))
    }

    // -----------------------------------------------------------------------
    // Notification flow (REST + WebSocket)
    // -----------------------------------------------------------------------

    @Test
    fun `notification lifecycle - list, mark read`() = runTest {
        val notifs = listOf(
            NotificationResponse(
                id = 1, userId = 1, title = "Booking Confirmed",
                message = "Your booking was confirmed", isRead = false,
                createdAt = "2026-03-10T10:00:00"
            ),
            NotificationResponse(
                id = 2, userId = 1, title = "Booking Cancelled",
                message = "Your booking was cancelled", isRead = false,
                createdAt = "2026-03-10T11:00:00"
            )
        )
        coEvery { notificationApi.getNotifications() } returns notifs

        val listResult = notificationRepo.getNotifications()
        assertTrue(listResult.isSuccess)
        assertEquals(2, listResult.getOrNull()!!.size)
        assertEquals("Booking Confirmed", listResult.getOrNull()!![0].title)

        val readNotif = notifs[0].copy(isRead = true)
        coEvery { notificationApi.markAsRead(1) } returns readNotif

        val readResult = notificationRepo.markAsRead(1)
        assertTrue(readResult.isSuccess)
        assertTrue(readResult.getOrNull()!!.isRead)
    }

    @Test
    fun `websocket connect and disconnect are delegated to client`() {
        notificationRepo.connectWebSocket("jwt-token-123")
        verify { webSocketClient.connect("jwt-token-123") }

        notificationRepo.disconnectWebSocket()
        verify { webSocketClient.disconnect() }
    }

    @Test
    fun `websocket notifications flow is exposed from client`() = runTest {
        val testNotification = Notification(
            id = 99, userId = 1, title = "Real-time Update",
            message = "Your flight has been updated",
            isRead = false, createdAt = "2026-03-10T12:00:00"
        )
        every { webSocketClient.notifications } returns flowOf(testNotification)

        val flow = notificationRepo.observeNotifications()
        flow.collect { notification ->
            assertEquals("Real-time Update", notification.title)
            assertEquals(99, notification.id)
        }
    }
}
