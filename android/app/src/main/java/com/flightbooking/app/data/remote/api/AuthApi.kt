package com.flightbooking.app.data.remote.api

import com.flightbooking.app.data.remote.dto.LoginRequest
import com.flightbooking.app.data.remote.dto.RegisterRequest
import com.flightbooking.app.data.remote.dto.TokenResponse
import com.flightbooking.app.data.remote.dto.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @GET("auth/me")
    suspend fun getCurrentUser(): UserResponse
}
