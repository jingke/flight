package com.flightbooking.app.domain.util

sealed class Result<out T> {

    data class Success<out T>(val data: T) : Result<T>()

    data class Error(
        val message: String,
        val code: Int? = null,
        val exception: Throwable? = null
    ) : Result<Nothing>()

    data object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }

    companion object {
        fun <T> fromResponse(block: suspend () -> T): suspend () -> Result<T> = {
            try {
                Success(block())
            } catch (e: Exception) {
                Error(
                    message = e.message ?: "Unknown error occurred",
                    exception = e
                )
            }
        }
    }
}
