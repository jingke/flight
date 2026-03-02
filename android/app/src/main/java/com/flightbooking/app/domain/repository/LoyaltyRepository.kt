package com.flightbooking.app.domain.repository

import com.flightbooking.app.domain.model.LoyaltyPoints
import com.flightbooking.app.domain.util.Result

interface LoyaltyRepository {
    suspend fun getLoyaltyPoints(): Result<LoyaltyPoints>
    suspend fun redeemPoints(points: Int): Result<LoyaltyPoints>
}
