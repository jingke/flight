package com.flightbooking.app.data.remote.dto

import com.flightbooking.app.domain.model.LoyaltyPoints
import com.google.gson.annotations.SerializedName

data class LoyaltyResponse(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    val earned: Int,
    val redeemed: Int,
    val balance: Int
) {
    fun toDomain(): LoyaltyPoints = LoyaltyPoints(
        id = id,
        userId = userId,
        earned = earned,
        redeemed = redeemed,
        balance = balance
    )
}

data class RedeemPointsRequest(
    val points: Int
)
