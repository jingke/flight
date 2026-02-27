package com.flightbooking.app.data.repository

import com.flightbooking.app.data.remote.api.ComplaintApi
import com.flightbooking.app.data.remote.dto.CreateComplaintRequest
import com.flightbooking.app.domain.model.Complaint
import com.flightbooking.app.domain.repository.ComplaintRepository
import com.flightbooking.app.domain.util.Result
import javax.inject.Inject

class ComplaintRepositoryImpl @Inject constructor(
    private val complaintApi: ComplaintApi
) : ComplaintRepository {

    override suspend fun submitComplaint(
        bookingId: Int?,
        subject: String,
        description: String
    ): Result<Complaint> {
        return try {
            val request = CreateComplaintRequest(bookingId, subject, description)
            val response = complaintApi.submitComplaint(request)
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to submit complaint", exception = e)
        }
    }

    override suspend fun getComplaints(): Result<List<Complaint>> {
        return try {
            val response = complaintApi.getComplaints()
            Result.Success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to get complaints", exception = e)
        }
    }

    override suspend fun getComplaintById(complaintId: Int): Result<Complaint> {
        return try {
            val response = complaintApi.getComplaintById(complaintId)
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to get complaint", exception = e)
        }
    }
}
