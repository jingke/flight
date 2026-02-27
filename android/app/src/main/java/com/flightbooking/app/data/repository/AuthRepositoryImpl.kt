package com.flightbooking.app.data.repository

import com.flightbooking.app.data.local.TokenManager
import com.flightbooking.app.data.remote.api.AuthApi
import com.flightbooking.app.data.remote.dto.LoginRequest
import com.flightbooking.app.data.remote.dto.RegisterRequest
import com.flightbooking.app.domain.model.User
import com.flightbooking.app.domain.repository.AuthRepository
import com.flightbooking.app.domain.util.Result
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            tokenManager.saveToken(response.accessToken)
            Result.Success(response.accessToken)
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Login failed", exception = e)
        }
    }

    override suspend fun register(email: String, password: String, name: String): Result<User> {
        return try {
            val response = authApi.register(RegisterRequest(email, password, name))
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Registration failed", exception = e)
        }
    }

    override suspend fun getCurrentUser(): Result<User> {
        return try {
            val response = authApi.getCurrentUser()
            Result.Success(response.toDomain())
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Failed to get user", exception = e)
        }
    }

    override suspend fun logout() {
        tokenManager.clearToken()
    }

    override suspend fun getStoredToken(): String? = tokenManager.getToken()

    override suspend fun isLoggedIn(): Boolean = tokenManager.hasToken()
}
