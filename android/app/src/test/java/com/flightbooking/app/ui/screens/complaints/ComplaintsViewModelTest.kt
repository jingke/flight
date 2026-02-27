package com.flightbooking.app.ui.screens.complaints

import com.flightbooking.app.MainDispatcherRule
import com.flightbooking.app.TestFixtures
import com.flightbooking.app.domain.repository.BookingRepository
import com.flightbooking.app.domain.repository.ComplaintRepository
import com.flightbooking.app.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
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
class ComplaintsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var complaintRepository: ComplaintRepository
    private lateinit var bookingRepository: BookingRepository

    @Before
    fun setup() {
        complaintRepository = mockk()
        bookingRepository = mockk()
    }

    private fun createViewModel(): ComplaintsViewModel {
        return ComplaintsViewModel(complaintRepository, bookingRepository)
    }

    @Test
    fun `init loads complaints and bookings`() = runTest {
        val complaints = listOf(TestFixtures.createComplaint(id = 1))
        val bookings = listOf(TestFixtures.createBooking(id = 1))
        coEvery { complaintRepository.getComplaints() } returns Result.Success(complaints)
        coEvery { bookingRepository.getBookings() } returns Result.Success(bookings)
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(1, state.complaints.size)
        assertEquals(1, state.bookings.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `init handles complaint error gracefully`() = runTest {
        coEvery { complaintRepository.getComplaints() } returns Result.Error("Connection failed")
        coEvery { bookingRepository.getBookings() } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.complaints.isEmpty())
        assertEquals("Connection failed", state.errorMessage)
    }

    @Test
    fun `ShowNewComplaintDialog resets form fields`() = runTest {
        coEvery { complaintRepository.getComplaints() } returns Result.Success(emptyList())
        coEvery { bookingRepository.getBookings() } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(ComplaintsEvent.ShowNewComplaintDialog)
        val state = viewModel.uiState.value
        assertTrue(state.showNewComplaintDialog)
        assertEquals("", state.subject)
        assertEquals("", state.description)
        assertNull(state.selectedBookingId)
    }

    @Test
    fun `DismissNewComplaintDialog hides dialog`() = runTest {
        coEvery { complaintRepository.getComplaints() } returns Result.Success(emptyList())
        coEvery { bookingRepository.getBookings() } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(ComplaintsEvent.ShowNewComplaintDialog)
        viewModel.onEvent(ComplaintsEvent.DismissNewComplaintDialog)
        assertFalse(viewModel.uiState.value.showNewComplaintDialog)
    }

    @Test
    fun `UpdateSelectedBooking updates booking id`() = runTest {
        coEvery { complaintRepository.getComplaints() } returns Result.Success(emptyList())
        coEvery { bookingRepository.getBookings() } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(ComplaintsEvent.UpdateSelectedBooking(5))
        assertEquals(5, viewModel.uiState.value.selectedBookingId)
    }

    @Test
    fun `UpdateSubject updates subject field`() = runTest {
        coEvery { complaintRepository.getComplaints() } returns Result.Success(emptyList())
        coEvery { bookingRepository.getBookings() } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(ComplaintsEvent.UpdateSubject("Lost baggage"))
        assertEquals("Lost baggage", viewModel.uiState.value.subject)
    }

    @Test
    fun `SubmitComplaint with blank subject shows error`() = runTest {
        coEvery { complaintRepository.getComplaints() } returns Result.Success(emptyList())
        coEvery { bookingRepository.getBookings() } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(ComplaintsEvent.UpdateDescription("Details"))
        viewModel.onEvent(ComplaintsEvent.SubmitComplaint)
        assertEquals("Subject and description are required", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `SubmitComplaint with blank description shows error`() = runTest {
        coEvery { complaintRepository.getComplaints() } returns Result.Success(emptyList())
        coEvery { bookingRepository.getBookings() } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(ComplaintsEvent.UpdateSubject("Issue"))
        viewModel.onEvent(ComplaintsEvent.SubmitComplaint)
        assertEquals("Subject and description are required", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `SubmitComplaint success shows success message and reloads`() = runTest {
        coEvery { complaintRepository.getComplaints() } returns Result.Success(emptyList())
        coEvery { bookingRepository.getBookings() } returns Result.Success(emptyList())
        coEvery {
            complaintRepository.submitComplaint(null, "Issue", "Details")
        } returns Result.Success(TestFixtures.createComplaint())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(ComplaintsEvent.UpdateSubject("Issue"))
        viewModel.onEvent(ComplaintsEvent.UpdateDescription("Details"))
        viewModel.onEvent(ComplaintsEvent.SubmitComplaint)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("Complaint submitted successfully", state.successMessage)
        assertFalse(state.showNewComplaintDialog)
        coVerify(exactly = 2) { complaintRepository.getComplaints() }
    }

    @Test
    fun `SubmitComplaint failure shows error`() = runTest {
        coEvery { complaintRepository.getComplaints() } returns Result.Success(emptyList())
        coEvery { bookingRepository.getBookings() } returns Result.Success(emptyList())
        coEvery {
            complaintRepository.submitComplaint(any(), any(), any())
        } returns Result.Error("Server error")
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(ComplaintsEvent.UpdateSubject("Issue"))
        viewModel.onEvent(ComplaintsEvent.UpdateDescription("Details"))
        viewModel.onEvent(ComplaintsEvent.SubmitComplaint)
        advanceUntilIdle()
        assertEquals("Server error", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `ClearMessages resets messages`() = runTest {
        coEvery { complaintRepository.getComplaints() } returns Result.Success(emptyList())
        coEvery { bookingRepository.getBookings() } returns Result.Success(emptyList())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(ComplaintsEvent.ClearMessages)
        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
    }
}
