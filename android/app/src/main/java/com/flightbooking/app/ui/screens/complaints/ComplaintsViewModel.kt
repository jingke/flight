package com.flightbooking.app.ui.screens.complaints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightbooking.app.domain.model.Booking
import com.flightbooking.app.domain.model.Complaint
import com.flightbooking.app.domain.repository.BookingRepository
import com.flightbooking.app.domain.repository.ComplaintRepository
import com.flightbooking.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComplaintsUiState(
    val complaints: List<Complaint> = emptyList(),
    val bookings: List<Booking> = emptyList(),
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val showNewComplaintDialog: Boolean = false,
    val selectedBookingId: Int? = null,
    val subject: String = "",
    val description: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed class ComplaintsEvent {
    data object LoadComplaints : ComplaintsEvent()
    data object ShowNewComplaintDialog : ComplaintsEvent()
    data object DismissNewComplaintDialog : ComplaintsEvent()
    data class UpdateSelectedBooking(val bookingId: Int?) : ComplaintsEvent()
    data class UpdateSubject(val subject: String) : ComplaintsEvent()
    data class UpdateDescription(val description: String) : ComplaintsEvent()
    data object SubmitComplaint : ComplaintsEvent()
    data object ClearMessages : ComplaintsEvent()
}

@HiltViewModel
class ComplaintsViewModel @Inject constructor(
    private val complaintRepository: ComplaintRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComplaintsUiState())
    val uiState: StateFlow<ComplaintsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun onEvent(event: ComplaintsEvent) {
        when (event) {
            is ComplaintsEvent.LoadComplaints -> loadData()
            is ComplaintsEvent.ShowNewComplaintDialog -> {
                _uiState.value = _uiState.value.copy(
                    showNewComplaintDialog = true,
                    subject = "",
                    description = "",
                    selectedBookingId = null
                )
            }
            is ComplaintsEvent.DismissNewComplaintDialog -> {
                _uiState.value = _uiState.value.copy(showNewComplaintDialog = false)
            }
            is ComplaintsEvent.UpdateSelectedBooking -> {
                _uiState.value = _uiState.value.copy(selectedBookingId = event.bookingId)
            }
            is ComplaintsEvent.UpdateSubject -> {
                _uiState.value = _uiState.value.copy(subject = event.subject)
            }
            is ComplaintsEvent.UpdateDescription -> {
                _uiState.value = _uiState.value.copy(description = event.description)
            }
            is ComplaintsEvent.SubmitComplaint -> submitComplaint()
            is ComplaintsEvent.ClearMessages -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = null,
                    successMessage = null
                )
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val complaintsResult = complaintRepository.getComplaints()
            val bookingsResult = bookingRepository.getBookings()
            _uiState.value = _uiState.value.copy(
                complaints = (complaintsResult as? Result.Success)?.data ?: emptyList(),
                bookings = (bookingsResult as? Result.Success)?.data ?: emptyList(),
                isLoading = false,
                errorMessage = (complaintsResult as? Result.Error)?.message
            )
        }
    }

    private fun submitComplaint() {
        val state = _uiState.value
        if (state.subject.isBlank() || state.description.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Subject and description are required")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, errorMessage = null)
            when (val result = complaintRepository.submitComplaint(
                bookingId = state.selectedBookingId,
                subject = state.subject,
                description = state.description
            )) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        showNewComplaintDialog = false,
                        successMessage = "Complaint submitted successfully"
                    )
                    loadData()
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = result.message
                )
                is Result.Loading -> Unit
            }
        }
    }
}
