package com.flightbooking.app.ui.screens.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightbooking.app.domain.model.Flight
import com.flightbooking.app.domain.repository.FlightRepository
import com.flightbooking.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchResultsUiState(
    val flights: List<Flight> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val departure: String = "",
    val arrival: String = "",
    val date: String = "",
    val passengers: Int = 1
)

@HiltViewModel
class SearchResultsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flightRepository: FlightRepository
) : ViewModel() {

    private val departure: String = savedStateHandle["departure"] ?: "any"
    private val arrival: String = savedStateHandle["arrival"] ?: "any"
    private val date: String = savedStateHandle["date"] ?: "any"
    private val passengers: Int = savedStateHandle["passengers"] ?: 1

    private val _uiState = MutableStateFlow(
        SearchResultsUiState(
            departure = departure,
            arrival = arrival,
            date = date,
            passengers = passengers
        )
    )
    val uiState: StateFlow<SearchResultsUiState> = _uiState.asStateFlow()

    init {
        searchFlights()
    }

    fun retry() {
        searchFlights()
    }

    private fun searchFlights() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val dep = departure.takeIf { it != "any" }
            val arr = arrival.takeIf { it != "any" }
            val dt = date.takeIf { it != "any" }
            when (val result = flightRepository.searchFlights(dep, arr, dt)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    flights = result.data,
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
