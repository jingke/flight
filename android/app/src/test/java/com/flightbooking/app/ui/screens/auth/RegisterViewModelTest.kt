package com.flightbooking.app.ui.screens.auth

import com.flightbooking.app.MainDispatcherRule
import com.flightbooking.app.TestFixtures
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
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setup() {
        authRepository = mockk()
        viewModel = RegisterViewModel(authRepository)
    }

    @Test
    fun `initial state has empty fields`() {
        val state = viewModel.uiState.value
        assertEquals("", state.name)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.isRegistrationSuccess)
    }

    @Test
    fun `UpdateName event updates name in state`() {
        viewModel.onEvent(RegisterEvent.UpdateName("John"))
        assertEquals("John", viewModel.uiState.value.name)
    }

    @Test
    fun `UpdateEmail event updates email in state`() {
        viewModel.onEvent(RegisterEvent.UpdateEmail("john@test.com"))
        assertEquals("john@test.com", viewModel.uiState.value.email)
    }

    @Test
    fun `UpdatePassword event updates password in state`() {
        viewModel.onEvent(RegisterEvent.UpdatePassword("pass123"))
        assertEquals("pass123", viewModel.uiState.value.password)
    }

    @Test
    fun `UpdateConfirmPassword event updates confirmPassword in state`() {
        viewModel.onEvent(RegisterEvent.UpdateConfirmPassword("pass123"))
        assertEquals("pass123", viewModel.uiState.value.confirmPassword)
    }

    @Test
    fun `Submit with blank name shows validation error`() {
        viewModel.onEvent(RegisterEvent.UpdateEmail("john@test.com"))
        viewModel.onEvent(RegisterEvent.UpdatePassword("pass123"))
        viewModel.onEvent(RegisterEvent.UpdateConfirmPassword("pass123"))
        viewModel.onEvent(RegisterEvent.Submit)
        assertEquals("All fields are required", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `Submit with blank email shows validation error`() {
        viewModel.onEvent(RegisterEvent.UpdateName("John"))
        viewModel.onEvent(RegisterEvent.UpdatePassword("pass123"))
        viewModel.onEvent(RegisterEvent.UpdateConfirmPassword("pass123"))
        viewModel.onEvent(RegisterEvent.Submit)
        assertEquals("All fields are required", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `Submit with mismatched passwords shows error`() {
        viewModel.onEvent(RegisterEvent.UpdateName("John"))
        viewModel.onEvent(RegisterEvent.UpdateEmail("john@test.com"))
        viewModel.onEvent(RegisterEvent.UpdatePassword("pass123"))
        viewModel.onEvent(RegisterEvent.UpdateConfirmPassword("different"))
        viewModel.onEvent(RegisterEvent.Submit)
        assertEquals("Passwords do not match", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `Submit with valid data sets isRegistrationSuccess on success`() = runTest {
        val expectedUser = TestFixtures.createUser()
        coEvery { authRepository.register("john@test.com", "pass123", "John") } returns Result.Success(expectedUser)
        viewModel.onEvent(RegisterEvent.UpdateName("John"))
        viewModel.onEvent(RegisterEvent.UpdateEmail("john@test.com"))
        viewModel.onEvent(RegisterEvent.UpdatePassword("pass123"))
        viewModel.onEvent(RegisterEvent.UpdateConfirmPassword("pass123"))
        viewModel.onEvent(RegisterEvent.Submit)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.isRegistrationSuccess)
        assertFalse(state.isLoading)
    }

    @Test
    fun `Submit with valid data shows error on failure`() = runTest {
        coEvery { authRepository.register(any(), any(), any()) } returns Result.Error("Email already registered")
        viewModel.onEvent(RegisterEvent.UpdateName("John"))
        viewModel.onEvent(RegisterEvent.UpdateEmail("john@test.com"))
        viewModel.onEvent(RegisterEvent.UpdatePassword("pass123"))
        viewModel.onEvent(RegisterEvent.UpdateConfirmPassword("pass123"))
        viewModel.onEvent(RegisterEvent.Submit)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isRegistrationSuccess)
        assertEquals("Email already registered", state.errorMessage)
    }

    @Test
    fun `ClearError event clears error message`() {
        viewModel.onEvent(RegisterEvent.Submit)
        viewModel.onEvent(RegisterEvent.ClearError)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
