package com.flightbooking.app.domain.repository

import com.flightbooking.app.domain.model.ModificationRequest
import com.flightbooking.app.domain.util.Result

interface ModificationRepository {
    suspend fun createModification(
        bookingId: Int,
        type: String,
        details: String
    ): Result<ModificationRequest>
    suspend fun getModifications(): Result<List<ModificationRequest>>
}
