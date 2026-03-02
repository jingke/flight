package com.flightbooking.app.ui.screens.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightbooking.app.domain.model.Booking
import com.flightbooking.app.domain.repository.BookingRepository
import com.flightbooking.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyBookingsUiState(
    val bookings: List<Booking> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class MyBookingsViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyBookingsUiState())
    val uiState: StateFlow<MyBookingsUiState> = _uiState.asStateFlow()

    init {
        loadBookings()
    }

    fun refresh() {
        loadBookings()
    }

    private fun loadBookings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = bookingRepository.getBookings()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    bookings = result.data,
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
}
