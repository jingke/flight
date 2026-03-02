package com.flightbooking.app.data.remote.dto

import com.flightbooking.app.domain.model.User
import com.flightbooking.app.domain.model.UserRole
import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String
)

data class UserResponse(
    val id: Int,
    val email: String,
    val name: String,
    val role: String,
    @SerializedName("created_at") val createdAt: String
) {
    fun toDomain(): User = User(
        id = id,
        email = email,
        name = name,
        role = when (role) {
            "admin" -> UserRole.ADMIN
            else -> UserRole.CUSTOMER
        },
        createdAt = createdAt
    )
}
