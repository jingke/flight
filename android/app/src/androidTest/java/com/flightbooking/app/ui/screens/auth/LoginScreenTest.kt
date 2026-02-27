package com.flightbooking.app.ui.screens.auth

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.flightbooking.app.domain.repository.AuthRepository
import com.flightbooking.app.domain.util.Result
import com.flightbooking.app.ui.theme.FlightBookingTheme
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val authRepository: AuthRepository = mockk()

    private fun setupScreen(
        onNavigateToRegister: () -> Unit = {},
        onNavigateToHome: () -> Unit = {}
    ) {
        val viewModel = LoginViewModel(authRepository)
        composeTestRule.setContent {
            FlightBookingTheme {
                LoginScreen(
                    onNavigateToRegister = onNavigateToRegister,
                    onNavigateToHome = onNavigateToHome,
                    viewModel = viewModel
                )
            }
        }
    }

    @Test
    fun loginScreen_displaysHeaderAndFields() {
        coEvery { authRepository.login(any(), any()) } returns Result.Error("not called")
        setupScreen()
        composeTestRule.onNodeWithText("Flight Booking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in to continue").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
    }

    @Test
    fun loginScreen_displaysRegisterLink() {
        coEvery { authRepository.login(any(), any()) } returns Result.Error("not called")
        setupScreen()
        composeTestRule.onNodeWithText("Don't have an account? Register").assertIsDisplayed()
    }

    @Test
    fun loginScreen_registerLinkCallsNavigation() {
        coEvery { authRepository.login(any(), any()) } returns Result.Error("not called")
        var navigatedToRegister = false
        setupScreen(onNavigateToRegister = { navigatedToRegister = true })
        composeTestRule.onNodeWithText("Don't have an account? Register").performClick()
        assert(navigatedToRegister)
    }

    @Test
    fun loginScreen_emptyFieldsShowsError() {
        coEvery { authRepository.login(any(), any()) } returns Result.Error("not called")
        setupScreen()
        composeTestRule.onNodeWithText("Sign In").performClick()
        composeTestRule.onNodeWithText("Email and password are required").assertIsDisplayed()
    }

    @Test
    fun loginScreen_invalidCredentialsShowsError() {
        coEvery { authRepository.login("bad@test.com", "wrong") } returns Result.Error("Invalid credentials")
        setupScreen()
        composeTestRule.onNodeWithText("Email").performTextInput("bad@test.com")
        composeTestRule.onNodeWithText("Password").performTextInput("wrong")
        composeTestRule.onNodeWithText("Sign In").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Invalid credentials").assertIsDisplayed()
    }

    @Test
    fun loginScreen_successfulLoginNavigatesToHome() {
        coEvery { authRepository.login("user@test.com", "pass123") } returns Result.Success("token")
        var navigatedToHome = false
        setupScreen(onNavigateToHome = { navigatedToHome = true })
        composeTestRule.onNodeWithText("Email").performTextInput("user@test.com")
        composeTestRule.onNodeWithText("Password").performTextInput("pass123")
        composeTestRule.onNodeWithText("Sign In").performClick()
        composeTestRule.waitForIdle()
        assert(navigatedToHome)
    }

    @Test
    fun loginScreen_signInButtonDisabledWhileLoading() {
        coEvery { authRepository.login(any(), any()) } coAnswers {
            delay(5000)
            Result.Success("token")
        }
        setupScreen()
        composeTestRule.onNodeWithText("Email").performTextInput("user@test.com")
        composeTestRule.onNodeWithText("Password").performTextInput("pass123")
        composeTestRule.onNodeWithText("Sign In").performClick()
        composeTestRule.onNodeWithText("Sign In").assertDoesNotExist()
    }
}
