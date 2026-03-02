package com.flightbooking.app.ui.screens.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightbooking.app.domain.model.Flight
import com.flightbooking.app.domain.repository.AirportRepository
import com.flightbooking.app.domain.repository.FlightRepository
import com.flightbooking.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteMapUiState(
    val flight: Flight? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class RouteMapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flightRepository: FlightRepository,
    private val airportRepository: AirportRepository
) : ViewModel() {

    private val flightId: Int = savedStateHandle["flightId"] ?: 0

    private val _uiState = MutableStateFlow(RouteMapUiState())
    val uiState: StateFlow<RouteMapUiState> = _uiState.asStateFlow()

    init {
        loadFlight()
    }

    private fun loadFlight() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = flightRepository.getFlightById(flightId)) {
                is Result.Success -> {
                    var flight = result.data
                    val needsDepCoords = flight.departureAirport.latitude == 0.0
                            && flight.departureAirport.longitude == 0.0
                    val needsArrCoords = flight.arrivalAirport.latitude == 0.0
                            && flight.arrivalAirport.longitude == 0.0
                    if (needsDepCoords || needsArrCoords) {
                        val depResult = airportRepository.getAirportById(
                            flight.departureAirport.id
                        )
                        val arrResult = airportRepository.getAirportById(
                            flight.arrivalAirport.id
                        )
                        flight = flight.copy(
                            departureAirport = (depResult as? Result.Success)?.data
                                ?: flight.departureAirport,
                            arrivalAirport = (arrResult as? Result.Success)?.data
                                ?: flight.arrivalAirport
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        flight = flight,
                        isLoading = false
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
                is Result.Loading -> Unit
            }
        }
    }
}
