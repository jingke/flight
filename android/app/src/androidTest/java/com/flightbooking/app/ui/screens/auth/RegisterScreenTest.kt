package com.flightbooking.app.ui.screens.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.flightbooking.app.domain.model.User
import com.flightbooking.app.domain.model.UserRole
import com.flightbooking.app.domain.repository.AuthRepository
import com.flightbooking.app.domain.util.Result
import com.flightbooking.app.ui.theme.FlightBookingTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val authRepository: AuthRepository = mockk()

    private fun setupScreen(
        onNavigateToLogin: () -> Unit = {},
        onRegistrationSuccess: () -> Unit = {}
    ) {
        val viewModel = RegisterViewModel(authRepository)
        composeTestRule.setContent {
            FlightBookingTheme {
                RegisterScreen(
                    onNavigateToLogin = onNavigateToLogin,
                    onRegistrationSuccess = onRegistrationSuccess,
                    viewModel = viewModel
                )
            }
        }
    }

    @Test
    fun registerScreen_displaysAllFields() {
        setupScreen()
        composeTestRule.onNodeWithText("Create Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Full Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Account", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun registerScreen_displaysLoginLink() {
        setupScreen()
        composeTestRule.onNodeWithText("Already have an account? Sign In").assertIsDisplayed()
    }

    @Test
    fun registerScreen_loginLinkCallsNavigation() {
        var navigatedToLogin = false
        setupScreen(onNavigateToLogin = { navigatedToLogin = true })
        composeTestRule.onNodeWithText("Already have an account? Sign In").performClick()
        assert(navigatedToLogin)
    }

    @Test
    fun registerScreen_emptyFieldsShowsError() {
        setupScreen()
        composeTestRule.onAllNodesWithText("Create Account")[1].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("All fields are required").assertIsDisplayed()
    }

    @Test
    fun registerScreen_mismatchedPasswordsShowsError() {
        setupScreen()
        composeTestRule.onNodeWithText("Full Name").performTextInput("John")
        composeTestRule.onNodeWithText("Email").performTextInput("john@test.com")
        composeTestRule.onNodeWithText("Password").performTextInput("pass123")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("different")
        composeTestRule.onAllNodesWithText("Create Account")[1].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Passwords do not match").assertIsDisplayed()
    }

    @Test
    fun registerScreen_successfulRegistrationNavigates() {
        val mockUser = User(1, "john@test.com", "John", UserRole.CUSTOMER, "2026-01-01")
        coEvery { authRepository.register("john@test.com", "pass123", "John") } returns Result.Success(mockUser)
        var registrationSuccess = false
        setupScreen(onRegistrationSuccess = { registrationSuccess = true })
        composeTestRule.onNodeWithText("Full Name").performTextInput("John")
        composeTestRule.onNodeWithText("Email").performTextInput("john@test.com")
        composeTestRule.onNodeWithText("Password").performTextInput("pass123")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("pass123")
        composeTestRule.onAllNodesWithText("Create Account")[1].performClick()
        composeTestRule.waitForIdle()
        assert(registrationSuccess)
    }

    @Test
    fun registerScreen_failedRegistrationShowsError() {
        coEvery { authRepository.register(any(), any(), any()) } returns Result.Error("Email taken")
        setupScreen()
        composeTestRule.onNodeWithText("Full Name").performTextInput("John")
        composeTestRule.onNodeWithText("Email").performTextInput("john@test.com")
        composeTestRule.onNodeWithText("Password").performTextInput("pass123")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("pass123")
        composeTestRule.onAllNodesWithText("Create Account")[1].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Email taken").assertIsDisplayed()
    }
}
