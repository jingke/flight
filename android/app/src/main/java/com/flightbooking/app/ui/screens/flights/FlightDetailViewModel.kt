package com.flightbooking.app.ui.screens.flights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightbooking.app.domain.model.Flight
import com.flightbooking.app.domain.model.Seat
import com.flightbooking.app.domain.repository.BookingRepository
import com.flightbooking.app.domain.repository.FlightRepository
import com.flightbooking.app.domain.repository.PassengerInput
import com.flightbooking.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PassengerForm(
    val name: String = "",
    val email: String = "",
    val selectedSeatId: Int? = null
)

data class FlightDetailUiState(
    val flight: Flight? = null,
    val seats: List<Seat> = emptyList(),
    val passengers: List<PassengerForm> = listOf(PassengerForm()),
    val activePassengerIndex: Int = 0,
    val isLoading: Boolean = true,
    val isBooking: Boolean = false,
    val errorMessage: String? = null,
    val bookingCreatedId: Int? = null
)

sealed class FlightDetailEvent {
    data class UpdatePassengerName(val index: Int, val name: String) : FlightDetailEvent()
    data class UpdatePassengerEmail(val index: Int, val email: String) : FlightDetailEvent()
    data class SetActivePassenger(val index: Int) : FlightDetailEvent()
    data class ToggleSeat(val seatId: Int) : FlightDetailEvent()
    data object AddPassenger : FlightDetailEvent()
    data class RemovePassenger(val index: Int) : FlightDetailEvent()
    data object CreateBooking : FlightDetailEvent()
    data object ClearError : FlightDetailEvent()
}

@HiltViewModel
class FlightDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flightRepository: FlightRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val flightId: Int = savedStateHandle["flightId"] ?: 0

    private val _uiState = MutableStateFlow(FlightDetailUiState())
    val uiState: StateFlow<FlightDetailUiState> = _uiState.asStateFlow()

    init {
        loadFlightDetails()
    }

    fun onEvent(event: FlightDetailEvent) {
        when (event) {
            is FlightDetailEvent.UpdatePassengerName -> updatePassengerField(event.index) {
                it.copy(name = event.name)
            }
            is FlightDetailEvent.UpdatePassengerEmail -> updatePassengerField(event.index) {
                it.copy(email = event.email)
            }
            is FlightDetailEvent.SetActivePassenger -> {
                _uiState.value = _uiState.value.copy(activePassengerIndex = event.index)
            }
            is FlightDetailEvent.ToggleSeat -> toggleSeat(event.seatId)
            is FlightDetailEvent.AddPassenger -> addPassenger()
            is FlightDetailEvent.RemovePassenger -> removePassenger(event.index)
            is FlightDetailEvent.CreateBooking -> createBooking()
            is FlightDetailEvent.ClearError -> {
                _uiState.value = _uiState.value.copy(errorMessage = null)
            }
        }
    }

    private fun loadFlightDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val flightResult = flightRepository.getFlightById(flightId)
            val seatResult = flightRepository.getSeatMap(flightId)
            when {
                flightResult is Result.Success -> _uiState.value = _uiState.value.copy(
                    flight = flightResult.data,
                    seats = (seatResult as? Result.Success)?.data ?: emptyList(),
                    isLoading = false
                )
                flightResult is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = flightResult.message
                )
                else -> Unit
            }
        }
    }

    private fun updatePassengerField(index: Int, transform: (PassengerForm) -> PassengerForm) {
        val passengers = _uiState.value.passengers.toMutableList()
        if (index in passengers.indices) {
            passengers[index] = transform(passengers[index])
            _uiState.value = _uiState.value.copy(passengers = passengers)
        }
    }

    private fun toggleSeat(seatId: Int) {
        val state = _uiState.value
        val activeIndex = state.activePassengerIndex
        val passengers = state.passengers.toMutableList()
        if (activeIndex !in passengers.indices) return
        val currentSeatId = passengers[activeIndex].selectedSeatId
        if (currentSeatId == seatId) {
            passengers[activeIndex] = passengers[activeIndex].copy(selectedSeatId = null)
        } else {
            for (i in passengers.indices) {
                if (passengers[i].selectedSeatId == seatId) {
                    passengers[i] = passengers[i].copy(selectedSeatId = null)
                }
            }
            passengers[activeIndex] = passengers[activeIndex].copy(selectedSeatId = seatId)
        }
        _uiState.value = state.copy(passengers = passengers)
    }

    private fun addPassenger() {
        val passengers = _uiState.value.passengers.toMutableList()
        passengers.add(PassengerForm())
        _uiState.value = _uiState.value.copy(
            passengers = passengers,
            activePassengerIndex = passengers.lastIndex
        )
    }

    private fun removePassenger(index: Int) {
        val passengers = _uiState.value.passengers.toMutableList()
        if (passengers.size <= 1 || index !in passengers.indices) return
        passengers.removeAt(index)
        val newActive = _uiState.value.activePassengerIndex.coerceAtMost(passengers.lastIndex)
        _uiState.value = _uiState.value.copy(
            passengers = passengers,
            activePassengerIndex = newActive
        )
    }

    private fun createBooking() {
        val state = _uiState.value
        val flight = state.flight ?: return
        if (state.passengers.any { it.name.isBlank() || it.email.isBlank() }) {
            _uiState.value = state.copy(errorMessage = "All passenger details are required")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isBooking = true, errorMessage = null)
            val inputs = state.passengers.map {
                PassengerInput(name = it.name, email = it.email, seatId = it.selectedSeatId)
            }
            when (val result = bookingRepository.createBooking(flight.id, inputs)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isBooking = false,
                    bookingCreatedId = result.data.id
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isBooking = false,
                    errorMessage = result.message
                )
                is Result.Loading -> Unit
            }
        }
    }
}
