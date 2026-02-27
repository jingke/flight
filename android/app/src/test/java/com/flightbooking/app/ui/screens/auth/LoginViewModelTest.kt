package com.flightbooking.app.ui.screens.auth

import app.cash.turbine.test
import com.flightbooking.app.MainDispatcherRule
import com.flightbooking.app.domain.repository.AuthRepository
import com.flightbooking.app.domain.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        authRepository = mockk()
        viewModel = LoginViewModel(authRepository)
    }

    @Test
    fun `initial state has empty fields and no loading`() {
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.isLoginSuccess)
    }

    @Test
    fun `UpdateEmail event updates email in state`() {
        viewModel.onEvent(LoginEvent.UpdateEmail("user@test.com"))
        assertEquals("user@test.com", viewModel.uiState.value.email)
    }

    @Test
    fun `UpdatePassword event updates password in state`() {
        viewModel.onEvent(LoginEvent.UpdatePassword("secret123"))
        assertEquals("secret123", viewModel.uiState.value.password)
    }

    @Test
    fun `ClearError event clears error message`() {
        viewModel.onEvent(LoginEvent.UpdateEmail(""))
        viewModel.onEvent(LoginEvent.Submit)
        viewModel.onEvent(LoginEvent.ClearError)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `Submit with blank email shows validation error`() {
        viewModel.onEvent(LoginEvent.UpdateEmail(""))
        viewModel.onEvent(LoginEvent.UpdatePassword("password"))
        viewModel.onEvent(LoginEvent.Submit)
        assertEquals("Email and password are required", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `Submit with blank password shows validation error`() {
        viewModel.onEvent(LoginEvent.UpdateEmail("user@test.com"))
        viewModel.onEvent(LoginEvent.UpdatePassword(""))
        viewModel.onEvent(LoginEvent.Submit)
        assertEquals("Email and password are required", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `Submit with valid credentials sets isLoginSuccess on success`() = runTest {
        coEvery { authRepository.login("user@test.com", "password123") } returns Result.Success("token-abc")
        viewModel.onEvent(LoginEvent.UpdateEmail("user@test.com"))
        viewModel.onEvent(LoginEvent.UpdatePassword("password123"))
        viewModel.onEvent(LoginEvent.Submit)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.isLoginSuccess)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `Submit with valid credentials shows error on failure`() = runTest {
        coEvery { authRepository.login("user@test.com", "wrong") } returns Result.Error("Invalid credentials")
        viewModel.onEvent(LoginEvent.UpdateEmail("user@test.com"))
        viewModel.onEvent(LoginEvent.UpdatePassword("wrong"))
        viewModel.onEvent(LoginEvent.Submit)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoginSuccess)
        assertFalse(state.isLoading)
        assertEquals("Invalid credentials", state.errorMessage)
    }

    @Test
    fun `Submit shows loading state during login`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns Result.Success("token")
        viewModel.onEvent(LoginEvent.UpdateEmail("user@test.com"))
        viewModel.onEvent(LoginEvent.UpdatePassword("password"))
        viewModel.uiState.test {
            skipItems(1)
            viewModel.onEvent(LoginEvent.Submit)
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertNull(loadingState.errorMessage)
            val finalState = awaitItem()
            assertFalse(finalState.isLoading)
            assertTrue(finalState.isLoginSuccess)
        }
    }
}
