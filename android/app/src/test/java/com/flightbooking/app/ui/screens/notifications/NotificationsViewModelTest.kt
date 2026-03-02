package com.flightbooking.app.ui.screens.notifications

import com.flightbooking.app.MainDispatcherRule
import com.flightbooking.app.TestFixtures
import com.flightbooking.app.domain.repository.AuthRepository
import com.flightbooking.app.domain.repository.NotificationRepository
import com.flightbooking.app.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var notificationRepository: NotificationRepository
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        notificationRepository = mockk()
        authRepository = mockk()
        every { notificationRepository.observeNotifications() } returns emptyFlow()
        every { notificationRepository.connectWebSocket(any()) } just runs
        every { notificationRepository.disconnectWebSocket() } just runs
    }

    private fun createViewModel(): NotificationsViewModel {
        return NotificationsViewModel(notificationRepository, authRepository)
    }

    @Test
    fun `init loads notifications successfully`() = runTest {
        val notifications = listOf(
            TestFixtures.createNotification(id = 1, createdAt = "2026-03-10T10:00:00"),
            TestFixtures.createNotification(id = 2, createdAt = "2026-03-11T10:00:00")
        )
        coEvery { notificationRepository.getNotifications() } returns Result.Success(notifications)
        coEvery { authRepository.getStoredToken() } returns "token-abc"
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(2, state.notifications.size)
        assertFalse(state.isLoading)
        assertEquals(2, state.notifications[0].id)
    }

    @Test
    fun `init shows error on load failure`() = runTest {
        coEvery { notificationRepository.getNotifications() } returns Result.Error("Server error")
        coEvery { authRepository.getStoredToken() } returns null
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals("Server error", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `init connects websocket when token exists`() = runTest {
        coEvery { notificationRepository.getNotifications() } returns Result.Success(emptyList())
        coEvery { authRepository.getStoredToken() } returns "valid-token"
        val viewModel = createViewModel()
        advanceUntilIdle()
        verify { notificationRepository.connectWebSocket("valid-token") }
    }

    @Test
    fun `init skips websocket when no token`() = runTest {
        coEvery { notificationRepository.getNotifications() } returns Result.Success(emptyList())
        coEvery { authRepository.getStoredToken() } returns null
        val viewModel = createViewModel()
        advanceUntilIdle()
        verify(exactly = 0) { notificationRepository.connectWebSocket(any()) }
    }

    @Test
    fun `markAsRead updates notification in list`() = runTest {
        val notification = TestFixtures.createNotification(id = 5, isRead = false)
        val readNotification = notification.copy(isRead = true)
        coEvery { notificationRepository.getNotifications() } returns Result.Success(listOf(notification))
        coEvery { authRepository.getStoredToken() } returns null
        coEvery { notificationRepository.markAsRead(5) } returns Result.Success(readNotification)
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.markAsRead(5)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.notifications[0].isRead)
    }

    @Test
    fun `markAsRead shows error on failure`() = runTest {
        val notification = TestFixtures.createNotification(id = 5)
        coEvery { notificationRepository.getNotifications() } returns Result.Success(listOf(notification))
        coEvery { authRepository.getStoredToken() } returns null
        coEvery { notificationRepository.markAsRead(5) } returns Result.Error("Failed to mark")
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.markAsRead(5)
        advanceUntilIdle()
        assertEquals("Failed to mark", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `refresh reloads notifications`() = runTest {
        coEvery { notificationRepository.getNotifications() } returns Result.Success(emptyList())
        coEvery { authRepository.getStoredToken() } returns null
        val viewModel = createViewModel()
        advanceUntilIdle()
        coEvery { notificationRepository.getNotifications() } returns Result.Success(
            listOf(TestFixtures.createNotification())
        )
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.notifications.size)
        coVerify(exactly = 2) { notificationRepository.getNotifications() }
    }

    @Test
    fun `notifications sorted by createdAt descending`() = runTest {
        val notifications = listOf(
            TestFixtures.createNotification(id = 1, createdAt = "2026-03-08T10:00:00"),
            TestFixtures.createNotification(id = 2, createdAt = "2026-03-12T10:00:00"),
            TestFixtures.createNotification(id = 3, createdAt = "2026-03-10T10:00:00")
        )
        coEvery { notificationRepository.getNotifications() } returns Result.Success(notifications)
        coEvery { authRepository.getStoredToken() } returns null
        val viewModel = createViewModel()
        advanceUntilIdle()
        val sorted = viewModel.uiState.value.notifications
        assertEquals(2, sorted[0].id)
        assertEquals(3, sorted[1].id)
        assertEquals(1, sorted[2].id)
    }
}
