package com.tomasronis.rhentiapp.core.network

import com.tomasronis.rhentiapp.data.auth.models.*
import retrofit2.http.*

/**
 * Retrofit API service interface for the Rhenti API.
 *
 * Defines all API endpoints used in the application.
 * This interface will be implemented by Retrofit at runtime.
 */
interface ApiClient {

    // ============================================================================
    // Authentication Endpoints
    // ============================================================================

    @POST("/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/register")
    suspend fun register(@Body request: RegistrationRequest)

    @POST("/forgot")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest)

    @POST("/integrations/sso/mobile/login")
    suspend fun ssoLogin(@Body request: SSOLoginRequest): LoginResponse

    // ============================================================================
    // Chat Hub Endpoints
    // ============================================================================

    @POST("/chat-hub/threads")
    suspend fun getThreads(@Body filter: Map<String, @JvmSuppressWildcards Any>): Map<String, @JvmSuppressWildcards Any>

    @GET("/chat-hub/threads/{threadId}")
    suspend fun getThread(@Path("threadId") threadId: String): Map<String, @JvmSuppressWildcards Any>

    @GET("/chat-hub/messages/{threadId}")
    suspend fun getMessages(
        @Path("threadId") threadId: String,
        @Query("limit") limit: Int? = 20,
        @Query("beforeId") beforeId: String? = null
    ): Map<String, @JvmSuppressWildcards Any>

    @POST("/message/{senderId}")
    suspend fun sendMessage(
        @Path("senderId") senderId: String,
        @Body message: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, @JvmSuppressWildcards Any>

    @PUT("/chat-hub/threads/{threadId}/badge")
    suspend fun clearBadge(@Path("threadId") threadId: String): Map<String, @JvmSuppressWildcards Any>

    @POST("/chat-hub/bookings/{bookingId}")
    suspend fun handleBooking(
        @Path("bookingId") bookingId: String,
        @Body action: Map<String, @JvmSuppressWildcards Any>
    ): Map<String, @JvmSuppressWildcards Any>

    @POST("/chat-hub/alternatives")
    suspend fun proposeAlternativeTimes(@Body request: Map<String, @JvmSuppressWildcards Any>): Map<String, @JvmSuppressWildcards Any>

    // ============================================================================
    // Contacts Endpoints
    // ============================================================================

    @GET("/phone-tracking/getContacts/{superAccountId}")
    suspend fun getContacts(@Path("superAccountId") superAccountId: String): Map<String, @JvmSuppressWildcards Any>

    @POST("/phone-tracking/getContactProfile")
    suspend fun getContactProfile(@Body request: Map<String, @JvmSuppressWildcards Any>): Map<String, @JvmSuppressWildcards Any>

    // ============================================================================
    // Calls/Phone Tracking Endpoints
    // ============================================================================

    @POST("/phone-tracking/accessToken")
    suspend fun getTwilioAccessToken(@Body request: Map<String, @JvmSuppressWildcards Any>): Map<String, @JvmSuppressWildcards Any>

    @GET("/phone-tracking/ownercontactlogs/{superAccountId}")
    suspend fun getCallLogs(@Path("superAccountId") superAccountId: String): Map<String, @JvmSuppressWildcards Any>

    @POST("/phone-tracking/callLog")
    suspend fun recordCallLog(@Body request: Map<String, @JvmSuppressWildcards Any>): Map<String, @JvmSuppressWildcards Any>

    // ============================================================================
    // Users Endpoints
    // ============================================================================

    @GET("/users/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): Map<String, @JvmSuppressWildcards Any>

    @PUT("/users")
    suspend fun updateUserProfile(@Body request: Map<String, @JvmSuppressWildcards Any>): Map<String, @JvmSuppressWildcards Any>

    @POST("/users")
    suspend fun batchGetUsers(@Body userIds: List<String>): List<Map<String, @JvmSuppressWildcards Any>>

    // ============================================================================
    // Properties Endpoints
    // ============================================================================

    @GET("/properties/{propertyId}")
    suspend fun getProperty(@Path("propertyId") propertyId: String): Map<String, @JvmSuppressWildcards Any>

    // ============================================================================
    // White Label Settings Endpoint
    // ============================================================================

    @GET("/admin/whitelabel/settings")
    suspend fun getWhiteLabelSettings(): Map<String, @JvmSuppressWildcards Any>

    @GET("/getAddressesForChatHub")
    suspend fun getAddressesForChatHub(): List<Map<String, @JvmSuppressWildcards Any>>

    // ============================================================================
    // App Version Endpoint
    // ============================================================================

    @GET("/appVersion")
    suspend fun getAppVersion(): Map<String, @JvmSuppressWildcards Any>
}
