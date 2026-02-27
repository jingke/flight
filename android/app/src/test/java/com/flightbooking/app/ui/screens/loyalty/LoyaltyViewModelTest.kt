package com.flightbooking.app.ui.screens.loyalty

import com.flightbooking.app.MainDispatcherRule
import com.flightbooking.app.TestFixtures
import com.flightbooking.app.domain.repository.LoyaltyRepository
import com.flightbooking.app.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoyaltyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var loyaltyRepository: LoyaltyRepository

    @Before
    fun setup() {
        loyaltyRepository = mockk()
    }

    private fun createViewModel(): LoyaltyViewModel {
        return LoyaltyViewModel(loyaltyRepository)
    }

    @Test
    fun `init loads loyalty points successfully`() = runTest {
        val points = TestFixtures.createLoyaltyPoints(earned = 500, redeemed = 100, balance = 400)
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(points)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNotNull(state.loyaltyPoints)
        assertEquals(400, state.loyaltyPoints?.balance)
        assertEquals(500, state.loyaltyPoints?.earned)
        assertEquals(100, state.loyaltyPoints?.redeemed)
        assertFalse(state.isLoading)
    }

    @Test
    fun `init shows error on failure`() = runTest {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Error("Failed to load")
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNull(state.loyaltyPoints)
        assertEquals("Failed to load", state.errorMessage)
    }

    @Test
    fun `ShowRedeemDialog shows dialog with empty amount`() = runTest {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(TestFixtures.createLoyaltyPoints())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(LoyaltyEvent.ShowRedeemDialog)
        val state = viewModel.uiState.value
        assertTrue(state.showRedeemDialog)
        assertEquals("", state.redeemAmount)
    }

    @Test
    fun `DismissRedeemDialog hides dialog`() = runTest {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(TestFixtures.createLoyaltyPoints())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(LoyaltyEvent.ShowRedeemDialog)
        viewModel.onEvent(LoyaltyEvent.DismissRedeemDialog)
        assertFalse(viewModel.uiState.value.showRedeemDialog)
    }

    @Test
    fun `UpdateRedeemAmount updates amount`() = runTest {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(TestFixtures.createLoyaltyPoints())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(LoyaltyEvent.UpdateRedeemAmount("150"))
        assertEquals("150", viewModel.uiState.value.redeemAmount)
    }

    @Test
    fun `ConfirmRedeem with invalid amount shows error`() = runTest {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(TestFixtures.createLoyaltyPoints())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(LoyaltyEvent.UpdateRedeemAmount("abc"))
        viewModel.onEvent(LoyaltyEvent.ConfirmRedeem)
        assertEquals("Enter a valid number of points", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `ConfirmRedeem with zero points shows error`() = runTest {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(TestFixtures.createLoyaltyPoints())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(LoyaltyEvent.UpdateRedeemAmount("0"))
        viewModel.onEvent(LoyaltyEvent.ConfirmRedeem)
        assertEquals("Enter a valid number of points", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `ConfirmRedeem with amount exceeding balance shows error`() = runTest {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(
            TestFixtures.createLoyaltyPoints(balance = 100)
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(LoyaltyEvent.UpdateRedeemAmount("200"))
        viewModel.onEvent(LoyaltyEvent.ConfirmRedeem)
        assertEquals("Insufficient points (balance: 100)", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `ConfirmRedeem with valid amount success updates points`() = runTest {
        val initialPoints = TestFixtures.createLoyaltyPoints(balance = 400, earned = 500, redeemed = 100)
        val updatedPoints = TestFixtures.createLoyaltyPoints(balance = 250, earned = 500, redeemed = 250)
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(initialPoints)
        coEvery { loyaltyRepository.redeemPoints(150) } returns Result.Success(updatedPoints)
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(LoyaltyEvent.UpdateRedeemAmount("150"))
        viewModel.onEvent(LoyaltyEvent.ConfirmRedeem)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(250, state.loyaltyPoints?.balance)
        assertEquals("Redeemed 150 points successfully", state.successMessage)
        assertFalse(state.isRedeeming)
        coVerify { loyaltyRepository.redeemPoints(150) }
    }

    @Test
    fun `ConfirmRedeem failure shows error`() = runTest {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(TestFixtures.createLoyaltyPoints())
        coEvery { loyaltyRepository.redeemPoints(any()) } returns Result.Error("Redemption failed")
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(LoyaltyEvent.UpdateRedeemAmount("50"))
        viewModel.onEvent(LoyaltyEvent.ConfirmRedeem)
        advanceUntilIdle()
        assertEquals("Redemption failed", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `ClearMessages clears both error and success`() = runTest {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(TestFixtures.createLoyaltyPoints())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(LoyaltyEvent.ClearMessages)
        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
    }

    @Test
    fun `LoadPoints reloads data`() = runTest {
        coEvery { loyaltyRepository.getLoyaltyPoints() } returns Result.Success(TestFixtures.createLoyaltyPoints())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(LoyaltyEvent.LoadPoints)
        advanceUntilIdle()
        coVerify(exactly = 2) { loyaltyRepository.getLoyaltyPoints() }
    }
}
