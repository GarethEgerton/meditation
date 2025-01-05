package com.example.meditation.core.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    is Result.Error -> null
}

fun <T> Result<T>.getOrDefault(default: T): T = when (this) {
    is Result.Success -> data
    is Result.Error -> default
}

fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.success(transform(data))
    is Result.Error -> Result.error(error)
}

fun <T> Flow<Result<T>>.unwrapOrNull(): Flow<T?> = map { it.getOrNull() }

fun <T> Flow<Result<T>>.unwrapOrDefault(default: T): Flow<T> = map { it.getOrDefault(default) } 