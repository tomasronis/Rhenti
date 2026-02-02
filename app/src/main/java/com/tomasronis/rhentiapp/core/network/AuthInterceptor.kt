package com.tomasronis.rhentiapp.core.network

import com.tomasronis.rhentiapp.core.security.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that adds authentication headers to API requests.
 *
 * Automatically injects:
 * - Authorization header with JWT token
 * - x-white-label header
 * - x-user-id header
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Don't add auth headers to login/register endpoints
        val url = originalRequest.url.toString()
        if (url.contains("/login") ||
            url.contains("/register") ||
            url.contains("/forgot")
        ) {
            return chain.proceed(originalRequest)
        }

        val requestBuilder = originalRequest.newBuilder()

        // Add Authorization header if token exists
        runBlocking {
            tokenManager.getAuthToken()?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            // Add white-label header
            tokenManager.getWhiteLabel()?.let { whiteLabel ->
                requestBuilder.addHeader("x-white-label", whiteLabel)
            } ?: run {
                // Default white label
                requestBuilder.addHeader("x-white-label", "rhenti_mobile")
            }

            // Add user ID header
            tokenManager.getUserId()?.let { userId ->
                requestBuilder.addHeader("x-user-id", userId)
            }
        }

        // Add standard headers
        requestBuilder.addHeader("Content-Type", "application/json")
        requestBuilder.addHeader("Accept", "application/json")

        return chain.proceed(requestBuilder.build())
    }
}
