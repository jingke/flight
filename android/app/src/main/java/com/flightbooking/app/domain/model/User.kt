package com.flightbooking.app.domain.model

data class User(
    val id: Int,
    val email: String,
    val name: String,
    val role: UserRole,
    val createdAt: String
)

enum class UserRole {
    CUSTOMER,
    ADMIN
}
