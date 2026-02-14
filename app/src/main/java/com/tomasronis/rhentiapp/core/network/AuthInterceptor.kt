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
        val requestBuilder = originalRequest.newBuilder()

        val url = originalRequest.url.toString()
        val isAuthEndpoint = url.contains("/login") ||
                           url.contains("/register") ||
                           url.contains("/forgot")
        // Twilio access token endpoint must be called without auth headers
        // to match the old app's behavior - the backend may use different
        // Twilio credentials based on white-label/auth headers
        val isTwilioTokenEndpoint = url.contains("/phone-tracking/accessToken")

        runBlocking {
            // Skip ALL custom headers for Twilio access token endpoint (matches old app behavior)
            if (isTwilioTokenEndpoint) {
                // Only add Content-Type, no auth/white-label headers
                requestBuilder.addHeader("Content-Type", "application/json")
                return@runBlocking
            }

            // Add Authorization header only for authenticated endpoints
            if (!isAuthEndpoint) {
                tokenManager.getAuthToken()?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }

                // Add user ID header for authenticated endpoints
                tokenManager.getUserId()?.let { userId ->
                    requestBuilder.addHeader("x-user-id", userId)
                }
            }

            // ALWAYS add x-white-label header (for all requests including login)
            tokenManager.getWhiteLabel()?.let { whiteLabel ->
                requestBuilder.addHeader("x-white-label", whiteLabel)
            } ?: run {
                // Default white label from BuildConfig
                requestBuilder.addHeader("x-white-label", com.tomasronis.rhentiapp.BuildConfig.WHITE_LABEL)
            }
        }

        if (!isTwilioTokenEndpoint) {
            // Add standard headers for non-Twilio endpoints
            requestBuilder.addHeader("Content-Type", "application/json")
            requestBuilder.addHeader("Accept", "application/json")
            requestBuilder.addHeader("x-language", "en")
        }

        return chain.proceed(requestBuilder.build())
    }
}
