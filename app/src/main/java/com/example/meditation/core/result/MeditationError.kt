package com.example.meditation.core.result

sealed class MeditationError {
    data class DatabaseError(
        val operation: String,
        val cause: Throwable
    ) : MeditationError()
    
    data class ValidationError(
        val field: String,
        val message: String
    ) : MeditationError()
    
    data class UnexpectedError(
        val cause: Throwable
    ) : MeditationError()
} 