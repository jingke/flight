package com.flightbooking.app.data.repository

import com.flightbooking.app.data.remote.api.LoyaltyApi
import com.flightbooking.app.data.remote.dto.RedeemPointsRequest
import com.flightbooking.app.domain.model.LoyaltyPoints
import com.flightbooking.app.domain.repository.LoyaltyRepository
import com.flightbooking.app.domain.util.Result
import javax.inject.Inject

class LoyaltyRepositoryImpl @Inject constructor(
    private val loyaltyApi: LoyaltyApi
) : LoyaltyRepository {

    override suspend fun getLoyaltyPoints(): Result<LoyaltyPoints> {
        return try {
            val response = loyaltyApi.getLoyaltyPoints()
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to get loyalty points", exception = e)
        }
    }

    override suspend fun redeemPoints(points: Int): Result<LoyaltyPoints> {
        return try {
            val response = loyaltyApi.redeemPoints(RedeemPointsRequest(points))
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to redeem points", exception = e)
        }
    }
}
