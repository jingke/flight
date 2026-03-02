package com.flightbooking.app.data.remote.api

import com.flightbooking.app.data.remote.dto.ComplaintResponse
import com.flightbooking.app.data.remote.dto.CreateComplaintRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ComplaintApi {

    @POST("complaints")
    suspend fun submitComplaint(@Body request: CreateComplaintRequest): ComplaintResponse

    @GET("complaints")
    suspend fun getComplaints(): List<ComplaintResponse>

    @GET("complaints/{id}")
    suspend fun getComplaintById(@Path("id") complaintId: Int): ComplaintResponse
}
