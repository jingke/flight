package com.flightbooking.app.data.remote.api

import com.flightbooking.app.data.remote.dto.LoyaltyResponse
import com.flightbooking.app.data.remote.dto.RedeemPointsRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface LoyaltyApi {

    @GET("loyalty")
    suspend fun getLoyaltyPoints(): LoyaltyResponse

    @POST("loyalty/redeem")
    suspend fun redeemPoints(@Body request: RedeemPointsRequest): LoyaltyResponse
}
