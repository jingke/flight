package com.flightbooking.app.ui.screens.loyalty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightbooking.app.domain.model.LoyaltyPoints
import com.flightbooking.app.domain.repository.LoyaltyRepository
import com.flightbooking.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoyaltyUiState(
    val loyaltyPoints: LoyaltyPoints? = null,
    val isLoading: Boolean = true,
    val isRedeeming: Boolean = false,
    val redeemAmount: String = "",
    val showRedeemDialog: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed class LoyaltyEvent {
    data object LoadPoints : LoyaltyEvent()
    data object ShowRedeemDialog : LoyaltyEvent()
    data object DismissRedeemDialog : LoyaltyEvent()
    data class UpdateRedeemAmount(val amount: String) : LoyaltyEvent()
    data object ConfirmRedeem : LoyaltyEvent()
    data object ClearMessages : LoyaltyEvent()
}

@HiltViewModel
class LoyaltyViewModel @Inject constructor(
    private val loyaltyRepository: LoyaltyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoyaltyUiState())
    val uiState: StateFlow<LoyaltyUiState> = _uiState.asStateFlow()

    init {
        loadPoints()
    }

    fun onEvent(event: LoyaltyEvent) {
        when (event) {
            is LoyaltyEvent.LoadPoints -> loadPoints()
            is LoyaltyEvent.ShowRedeemDialog -> {
                _uiState.value = _uiState.value.copy(showRedeemDialog = true, redeemAmount = "")
            }
            is LoyaltyEvent.DismissRedeemDialog -> {
                _uiState.value = _uiState.value.copy(showRedeemDialog = false)
            }
            is LoyaltyEvent.UpdateRedeemAmount -> {
                _uiState.value = _uiState.value.copy(redeemAmount = event.amount)
            }
            is LoyaltyEvent.ConfirmRedeem -> redeemPoints()
            is LoyaltyEvent.ClearMessages -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = null,
                    successMessage = null
                )
            }
        }
    }

    private fun loadPoints() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = loyaltyRepository.getLoyaltyPoints()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    loyaltyPoints = result.data,
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

    private fun redeemPoints() {
        val state = _uiState.value
        val points = state.redeemAmount.toIntOrNull()
        if (points == null || points <= 0) {
            _uiState.value = state.copy(errorMessage = "Enter a valid number of points")
            return
        }
        val balance = state.loyaltyPoints?.balance ?: 0
        if (points > balance) {
            _uiState.value = state.copy(errorMessage = "Insufficient points (balance: $balance)")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(
                isRedeeming = true,
                showRedeemDialog = false,
                errorMessage = null
            )
            when (val result = loyaltyRepository.redeemPoints(points)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    loyaltyPoints = result.data,
                    isRedeeming = false,
                    successMessage = "Redeemed $points points successfully"
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isRedeeming = false,
                    errorMessage = result.message
                )
                is Result.Loading -> Unit
            }
        }
    }
}
