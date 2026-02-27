package com.flightbooking.app.data.repository

import com.flightbooking.app.data.remote.api.ModificationApi
import com.flightbooking.app.data.remote.dto.CreateModificationRequest
import com.flightbooking.app.domain.model.ModificationRequest
import com.flightbooking.app.domain.repository.ModificationRepository
import com.flightbooking.app.domain.util.Result
import javax.inject.Inject

class ModificationRepositoryImpl @Inject constructor(
    private val modificationApi: ModificationApi
) : ModificationRepository {

    override suspend fun createModification(
        bookingId: Int,
        type: String,
        details: String
    ): Result<ModificationRequest> {
        return try {
            val request = CreateModificationRequest(bookingId, type, details)
            val response = modificationApi.createModification(request)
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(
                message = e.message ?: "Failed to create modification request",
                exception = e
            )
        }
    }

    override suspend fun getModifications(): Result<List<ModificationRequest>> {
        return try {
            val response = modificationApi.getModifications()
            Result.Success(response.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(
                message = e.message ?: "Failed to get modifications",
                exception = e
            )
        }
    }
}
