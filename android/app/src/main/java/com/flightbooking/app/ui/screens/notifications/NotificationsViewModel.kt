package com.flightbooking.app.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightbooking.app.domain.model.Notification
import com.flightbooking.app.domain.repository.AuthRepository
import com.flightbooking.app.domain.repository.NotificationRepository
import com.flightbooking.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
        connectWebSocket()
    }

    fun refresh() {
        loadNotifications()
    }

    fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            when (val result = notificationRepository.markAsRead(notificationId)) {
                is Result.Success -> {
                    val updated = _uiState.value.notifications.map { notification ->
                        if (notification.id == notificationId) result.data else notification
                    }
                    _uiState.value = _uiState.value.copy(notifications = updated)
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    errorMessage = result.message
                )
                is Result.Loading -> Unit
            }
        }
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = notificationRepository.getNotifications()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    notifications = result.data.sortedByDescending { it.createdAt },
                    isLoading = false
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
                is Result.Loading -> Unit
            }
        }
    }

    private fun connectWebSocket() {
        viewModelScope.launch {
            val token = authRepository.getStoredToken() ?: return@launch
            notificationRepository.connectWebSocket(token)
            notificationRepository.observeNotifications().collect { notification ->
                val current = _uiState.value.notifications.toMutableList()
                current.add(0, notification)
                _uiState.value = _uiState.value.copy(notifications = current)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        notificationRepository.disconnectWebSocket()
    }
}
