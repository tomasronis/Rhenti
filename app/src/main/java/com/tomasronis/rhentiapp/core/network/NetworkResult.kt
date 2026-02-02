package com.tomasronis.rhentiapp.core.network

/**
 * Sealed class representing the result of a network operation.
 *
 * @param T the type of data expected from the network operation
 */
sealed class NetworkResult<out T> {
    /**
     * Represents a successful network operation.
     *
     * @param data the data returned from the operation
     */
    data class Success<T>(val data: T) : NetworkResult<T>()

    /**
     * Represents a failed network operation.
     *
     * @param exception the exception that caused the failure
     * @param cachedData optional cached data that can be used as fallback
     */
    data class Error<T>(
        val exception: Exception,
        val cachedData: T? = null
    ) : NetworkResult<T>()

    /**
     * Represents a loading state.
     *
     * @param data optional cached data that can be shown while loading
     */
    data class Loading<T>(val data: T? = null) : NetworkResult<T>()

    /**
     * Returns true if this is a Success result.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if this is an Error result.
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Returns true if this is a Loading result.
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Returns the data if this is a Success result, null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> cachedData
        is Loading -> data
    }
}
