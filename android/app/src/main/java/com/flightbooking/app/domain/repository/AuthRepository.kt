package com.flightbooking.app.domain.repository

import com.flightbooking.app.domain.model.User
import com.flightbooking.app.domain.util.Result

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<String>
    suspend fun register(email: String, password: String, name: String): Result<User>
    suspend fun getCurrentUser(): Result<User>
    suspend fun logout()
    suspend fun getStoredToken(): String?
    suspend fun isLoggedIn(): Boolean
}
