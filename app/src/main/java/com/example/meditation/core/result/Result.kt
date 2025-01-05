package com.example.meditation.core.result

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: MeditationError) : Result<Nothing>()

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(error: MeditationError): Result<Nothing> = Error(error)
    }
} 