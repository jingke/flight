package com.flightbooking.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightbooking.app.domain.repository.AuthRepository
import com.flightbooking.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistrationSuccess: Boolean = false
)

sealed class RegisterEvent {
    data class UpdateName(val name: String) : RegisterEvent()
    data class UpdateEmail(val email: String) : RegisterEvent()
    data class UpdatePassword(val password: String) : RegisterEvent()
    data class UpdateConfirmPassword(val confirmPassword: String) : RegisterEvent()
    data object Submit : RegisterEvent()
    data object ClearError : RegisterEvent()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.UpdateName -> _uiState.value = _uiState.value.copy(name = event.name)
            is RegisterEvent.UpdateEmail -> _uiState.value = _uiState.value.copy(email = event.email)
            is RegisterEvent.UpdatePassword -> _uiState.value = _uiState.value.copy(password = event.password)
            is RegisterEvent.UpdateConfirmPassword -> _uiState.value = _uiState.value.copy(confirmPassword = event.confirmPassword)
            is RegisterEvent.Submit -> performRegistration()
            is RegisterEvent.ClearError -> _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }

    private fun performRegistration() {
        val state = _uiState.value
        if (state.name.isBlank() || state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "All fields are required")
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(errorMessage = "Passwords do not match")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.register(state.email, state.password, state.name)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRegistrationSuccess = true
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
