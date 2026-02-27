package com.flightbooking.app.domain.model

data class LoyaltyPoints(
    val id: Int,
    val userId: Int,
    val earned: Int,
    val redeemed: Int,
    val balance: Int
)
