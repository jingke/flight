package com.flightbooking.app.ui.screens.bookings

import androidx.lifecycle.SavedStateHandle
import com.flightbooking.app.MainDispatcherRule
import com.flightbooking.app.TestFixtures
import com.flightbooking.app.domain.repository.BookingRepository
import com.flightbooking.app.domain.repository.ModificationRepository
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
class BookingDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bookingRepository: BookingRepository
    private lateinit var modificationRepository: ModificationRepository
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setup() {
        bookingRepository = mockk()
        modificationRepository = mockk()
        savedStateHandle = SavedStateHandle(mapOf("bookingId" to 42))
    }

    private fun createViewModel(): BookingDetailViewModel {
        return BookingDetailViewModel(savedStateHandle, bookingRepository, modificationRepository)
    }

    @Test
    fun `init loads booking detail successfully`() = runTest {
        val booking = TestFixtures.createBooking(id = 42)
        coEvery { bookingRepository.getBookingById(42) } returns Result.Success(booking)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNotNull(state.booking)
        assertEquals(42, state.booking?.id)
        assertFalse(state.isLoading)
    }

    @Test
    fun `init shows error when loading fails`() = runTest {
        coEvery { bookingRepository.getBookingById(42) } returns Result.Error("Not found")
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNull(state.booking)
        assertEquals("Not found", state.errorMessage)
    }

    @Test
    fun `ShowCancelDialog sets showCancelDialog true`() = runTest {
        coEvery { bookingRepository.getBookingById(42) } returns Result.Success(TestFixtures.createBooking(id = 42))
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(BookingDetailEvent.ShowCancelDialog)
        assertTrue(viewModel.uiState.value.showCancelDialog)
    }

    @Test
    fun `DismissCancelDialog sets showCancelDialog false`() = runTest {
        coEvery { bookingRepository.getBookingById(42) } returns Result.Success(TestFixtures.createBooking(id = 42))
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(BookingDetailEvent.ShowCancelDialog)
        viewModel.onEvent(BookingDetailEvent.DismissCancelDialog)
        assertFalse(viewModel.uiState.value.showCancelDialog)
    }

    @Test
    fun `ConfirmCancel cancels booking successfully`() = runTest {
        val booking = TestFixtures.createBooking(id = 42)
        coEvery { bookingRepository.getBookingById(42) } returns Result.Success(booking)
        coEvery { bookingRepository.cancelBooking(42) } returns Result.Success(booking)
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(BookingDetailEvent.ConfirmCancel)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isCancelling)
        assertEquals("Booking cancelled successfully", state.successMessage)
        coVerify { bookingRepository.cancelBooking(42) }
    }

    @Test
    fun `ConfirmCancel shows error on failure`() = runTest {
        val booking = TestFixtures.createBooking(id = 42)
        coEvery { bookingRepository.getBookingById(42) } returns Result.Success(booking)
        coEvery { bookingRepository.cancelBooking(42) } returns Result.Error("Cannot cancel")
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(BookingDetailEvent.ConfirmCancel)
        advanceUntilIdle()
        assertEquals("Cannot cancel", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `ShowModifyDialog resets modify fields`() = runTest {
        coEvery { bookingRepository.getBookingById(42) } returns Result.Success(TestFixtures.createBooking(id = 42))
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(BookingDetailEvent.ShowModifyDialog)
        val state = viewModel.uiState.value
        assertTrue(state.showModifyDialog)
        assertEquals("date_change", state.modifyType)
        assertEquals("", state.modifyDetails)
    }

    @Test
    fun `UpdateModifyType updates type`() = runTest {
        coEvery { bookingRepository.getBookingById(42) } returns Result.Success(TestFixtures.createBooking(id = 42))
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(BookingDetailEvent.UpdateModifyType("seat_change"))
        assertEquals("seat_change", viewModel.uiState.value.modifyType)
    }

    @Test
    fun `UpdateModifyDetails updates details`() = runTest {
        coEvery { bookingRepository.getBookingById(42) } returns Result.Success(TestFixtures.createBooking(id = 42))
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(BookingDetailEvent.UpdateModifyDetails("New date: March 20"))
        assertEquals("New date: March 20", viewModel.uiState.value.modifyDetails)
    }

    @Test
    fun `SubmitModification with blank details shows error`() = runTest {
        coEvery { bookingRepository.getBookingById(42) } returns Result.Success(TestFixtures.createBooking(id = 42))
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(BookingDetailEvent.UpdateModifyDetails(""))
        viewModel.onEvent(BookingDetailEvent.SubmitModification)
        assertEquals("Please provide details for the modification", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `SubmitModification success shows success message`() = runTest {
        coEvery { bookingRepository.getBookingById(42) } returns Result.Success(TestFixtures.createBooking(id = 42))
        coEvery {
            modificationRepository.createModification(42, "date_change", "March 20")
        } returns Result.Success(TestFixtures.createModificationRequest())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(BookingDetailEvent.UpdateModifyDetails("March 20"))
        viewModel.onEvent(BookingDetailEvent.SubmitModification)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("Modification request submitted", state.successMessage)
        assertFalse(state.isSubmittingModification)
    }

    @Test
    fun `ClearMessages resets error and success`() = runTest {
        coEvery { bookingRepository.getBookingById(42) } returns Result.Success(TestFixtures.createBooking(id = 42))
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(BookingDetailEvent.ClearMessages)
        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
    }
}
