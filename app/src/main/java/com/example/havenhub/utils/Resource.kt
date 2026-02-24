package com.example.havenhub.utils

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val code: Int? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error
    val isLoading get() = this is Loading

    fun getOrNull(): T? = if (this is Success) data else null
    fun errorMessage(): String? = if (this is Error) message else null
}

fun <T> Result<T>.toResource(): Resource<T> {
    return if (isSuccess) Resource.Success(getOrThrow())
    else Resource.Error(exceptionOrNull()?.message ?: "Unknown error")
}
