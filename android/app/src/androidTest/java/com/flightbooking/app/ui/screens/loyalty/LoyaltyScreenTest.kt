package com.flightbooking.app.ui.screens.loyalty

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.flightbooking.app.domain.model.LoyaltyPoints
import com.flightbooking.app.domain.repository.LoyaltyRepository
import com.flightbooking.app.domain.util.Result
import com.flightbooking.app.ui.theme.FlightBookingTheme
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class LoyaltyScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val loyaltyRepository: LoyaltyRepository = mockk()

    private fun setupScreen(onNavigateBack: () -> Unit = {}) {
        val viewModel = LoyaltyViewModel(loyaltyRepository)
        composeTestRule.setContent {
            FlightBookingTheme {
                LoyaltyScreen(
                    onNavigateBack = onNavigateBack,
                    viewModel = viewModel
                )
            }
        }
    }

    @Test
    fun loyaltyScreen_displaysTitle() {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(
            LoyaltyPoints(1, 1, 500, 100, 400)
        )
        setupScreen()
        composeTestRule.onNodeWithText("Loyalty Points").assertIsDisplayed()
    }

    @Test
    fun loyaltyScreen_displaysBalanceCard() {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(
            LoyaltyPoints(1, 1, 500, 100, 400)
        )
        setupScreen()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Available Balance").assertIsDisplayed()
        composeTestRule.onNodeWithText("400").assertIsDisplayed()
        composeTestRule.onNodeWithText("points").assertIsDisplayed()
    }

    @Test
    fun loyaltyScreen_displaysSummaryCards() {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(
            LoyaltyPoints(1, 1, 500, 100, 400)
        )
        setupScreen()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Earned")[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("500").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Redeemed")[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("100").assertIsDisplayed()
    }

    @Test
    fun loyaltyScreen_displaysRedeemButton() {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(
            LoyaltyPoints(1, 1, 500, 100, 400)
        )
        setupScreen()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Redeem Points").assertIsDisplayed()
        composeTestRule.onNodeWithText("Redeem Points").assertIsEnabled()
    }

    @Test
    fun loyaltyScreen_redeemButtonOpensDialog() {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(
            LoyaltyPoints(1, 1, 500, 100, 400)
        )
        setupScreen()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Redeem Points").performClick()
        composeTestRule.onNodeWithText("Points to Redeem").assertIsDisplayed()
        composeTestRule.onNodeWithText("Available balance: 400 points").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun loyaltyScreen_noDataShowsRetry() {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Error("Failed")
        setupScreen()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No loyalty data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun loyaltyScreen_displaysChartSection() {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(
            LoyaltyPoints(1, 1, 500, 100, 400)
        )
        setupScreen()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Points Overview").assertIsDisplayed()
    }

    @Test
    fun loyaltyScreen_navigateBackCallsCallback() {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(
            LoyaltyPoints(1, 1, 500, 100, 400)
        )
        var navigatedBack = false
        setupScreen(onNavigateBack = { navigatedBack = true })
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(navigatedBack)
    }
}
