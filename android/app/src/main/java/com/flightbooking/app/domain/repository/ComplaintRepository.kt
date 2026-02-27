package com.flightbooking.app.domain.repository

import com.flightbooking.app.domain.model.Complaint
import com.flightbooking.app.domain.util.Result

interface ComplaintRepository {
    suspend fun submitComplaint(
        bookingId: Int?,
        subject: String,
        description: String
    ): Result<Complaint>

    suspend fun getComplaints(): Result<List<Complaint>>
    suspend fun getComplaintById(complaintId: Int): Result<Complaint>
}
