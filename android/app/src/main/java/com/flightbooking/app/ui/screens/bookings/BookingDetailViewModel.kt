package com.flightbooking.app.ui.screens.bookings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightbooking.app.domain.model.Booking
import com.flightbooking.app.domain.repository.BookingRepository
import com.flightbooking.app.domain.repository.ModificationRepository
import com.flightbooking.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingDetailUiState(
    val booking: Booking? = null,
    val isLoading: Boolean = true,
    val isCancelling: Boolean = false,
    val isSubmittingModification: Boolean = false,
    val showModifyDialog: Boolean = false,
    val showCancelDialog: Boolean = false,
    val modifyType: String = "date_change",
    val modifyDetails: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed class BookingDetailEvent {
    data object LoadBooking : BookingDetailEvent()
    data object ShowCancelDialog : BookingDetailEvent()
    data object DismissCancelDialog : BookingDetailEvent()
    data object ConfirmCancel : BookingDetailEvent()
    data object ShowModifyDialog : BookingDetailEvent()
    data object DismissModifyDialog : BookingDetailEvent()
    data class UpdateModifyType(val type: String) : BookingDetailEvent()
    data class UpdateModifyDetails(val details: String) : BookingDetailEvent()
    data object SubmitModification : BookingDetailEvent()
    data object ClearMessages : BookingDetailEvent()
}

@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingRepository: BookingRepository,
    private val modificationRepository: ModificationRepository
) : ViewModel() {

    private val bookingId: Int = savedStateHandle["bookingId"] ?: 0

    private val _uiState = MutableStateFlow(BookingDetailUiState())
    val uiState: StateFlow<BookingDetailUiState> = _uiState.asStateFlow()

    init {
        loadBooking()
    }

    fun onEvent(event: BookingDetailEvent) {
        when (event) {
            is BookingDetailEvent.LoadBooking -> loadBooking()
            is BookingDetailEvent.ShowCancelDialog -> {
                _uiState.value = _uiState.value.copy(showCancelDialog = true)
            }
            is BookingDetailEvent.DismissCancelDialog -> {
                _uiState.value = _uiState.value.copy(showCancelDialog = false)
            }
            is BookingDetailEvent.ConfirmCancel -> cancelBooking()
            is BookingDetailEvent.ShowModifyDialog -> {
                _uiState.value = _uiState.value.copy(
                    showModifyDialog = true,
                    modifyType = "date_change",
                    modifyDetails = ""
                )
            }
            is BookingDetailEvent.DismissModifyDialog -> {
                _uiState.value = _uiState.value.copy(showModifyDialog = false)
            }
            is BookingDetailEvent.UpdateModifyType -> {
                _uiState.value = _uiState.value.copy(modifyType = event.type)
            }
            is BookingDetailEvent.UpdateModifyDetails -> {
                _uiState.value = _uiState.value.copy(modifyDetails = event.details)
            }
            is BookingDetailEvent.SubmitModification -> submitModification()
            is BookingDetailEvent.ClearMessages -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = null,
                    successMessage = null
                )
            }
        }
    }

    private fun loadBooking() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = bookingRepository.getBookingById(bookingId)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    booking = result.data,
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

    private fun cancelBooking() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCancelling = true,
                showCancelDialog = false,
                errorMessage = null
            )
            when (val result = bookingRepository.cancelBooking(bookingId)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCancelling = false,
                        successMessage = "Booking cancelled successfully"
                    )
                    loadBooking()
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isCancelling = false,
                    errorMessage = result.message
                )
                is Result.Loading -> Unit
            }
        }
    }

    private fun submitModification() {
        val state = _uiState.value
        if (state.modifyDetails.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please provide details for the modification")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(
                isSubmittingModification = true,
                showModifyDialog = false,
                errorMessage = null
            )
            when (val result = modificationRepository.createModification(
                bookingId = bookingId,
                type = state.modifyType,
                details = state.modifyDetails
            )) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSubmittingModification = false,
                    successMessage = "Modification request submitted"
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isSubmittingModification = false,
                    errorMessage = result.message
                )
                is Result.Loading -> Unit
            }
        }
    }
}
