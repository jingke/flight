package com.flightbooking.app.data.remote.api

import com.flightbooking.app.data.remote.dto.CreateModificationRequest
import com.flightbooking.app.data.remote.dto.ModificationResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ModificationApi {

    @POST("modifications")
    suspend fun createModification(@Body request: CreateModificationRequest): ModificationResponse

    @GET("modifications")
    suspend fun getModifications(): List<ModificationResponse>
}
