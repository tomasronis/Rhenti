package com.tomasronis.rhentiapp.core.network

import com.tomasronis.rhentiapp.BuildConfig

/**
 * Environment configuration for API endpoints.
 *
 * Provides different endpoints for UAT and Production environments
 * based on the build configuration.
 */
object Environment {

    enum class Type {
        UAT,
        DEMO,
        PRODUCTION
    }

    val current: Type
        get() = when (BuildConfig.ENVIRONMENT) {
            "PRODUCTION" -> Type.PRODUCTION
            "DEMO" -> Type.DEMO
            else -> Type.UAT
        }

    val apiBaseUrl: String
        get() = when (current) {
            Type.UAT -> BuildConfig.API_BASE_URL_UAT
            Type.DEMO -> BuildConfig.API_BASE_URL_DEMO
            Type.PRODUCTION -> BuildConfig.API_BASE_URL_PROD
        }

    val imageBaseUrl: String
        get() = when (current) {
            Type.UAT -> BuildConfig.API_IMAGE_URL_UAT
            Type.DEMO -> BuildConfig.API_IMAGE_URL_PROD // Use production images for demo
            Type.PRODUCTION -> BuildConfig.API_IMAGE_URL_PROD
        }

    fun getImageUrl(imagePath: String): String {
        return "$imageBaseUrl$imagePath"
    }
}
