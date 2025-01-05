package com.example.meditation.core.result

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val error: MeditationError) : UiState<Nothing>()

    companion object {
        fun <T> loading(): UiState<T> = Loading
        fun <T> success(data: T): UiState<T> = Success(data)
        fun <T> error(error: MeditationError): UiState<T> = Error(error)
    }
} 