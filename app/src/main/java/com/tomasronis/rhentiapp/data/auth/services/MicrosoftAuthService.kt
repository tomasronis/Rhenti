package com.tomasronis.rhentiapp.data.auth.services

import android.app.Activity
import android.content.Context
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalUserCancelException
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MicrosoftAuthService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scopes = arrayOf("User.Read")

    private var publicClientApp: ISingleAccountPublicClientApplication? = null

    private suspend fun getPublicClientApp(): ISingleAccountPublicClientApplication =
        suspendCancellableCoroutine { continuation ->
            if (publicClientApp != null) {
                continuation.resume(publicClientApp!!)
                return@suspendCancellableCoroutine
            }

            PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                R.raw.msal_config,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        publicClientApp = application
                        continuation.resume(application)
                    }

                    override fun onError(exception: MsalException) {
                        if (BuildConfig.DEBUG) {
                            android.util.Log.e("MicrosoftAuth", "Failed to create MSAL app", exception)
                        }
                        continuation.resume(
                            throw MicrosoftAuthException.Failed(
                                exception.message ?: "Failed to initialize MSAL"
                            )
                        )
                    }
                }
            )
        }

    suspend fun signIn(activity: Activity): Result<Triple<String, String, String?>> =
        suspendCancellableCoroutine { continuation ->
            try {
                val app = kotlinx.coroutines.runBlocking { getPublicClientApp() }

                app.signIn(
                    activity,
                    null,
                    scopes,
                    object : AuthenticationCallback {
                        override fun onSuccess(authenticationResult: IAuthenticationResult) {
                            val accessToken = authenticationResult.accessToken
                            val email = authenticationResult.account.username
                            val displayName = authenticationResult.account.claims?.get("name") as? String

                            if (BuildConfig.DEBUG) {
                                android.util.Log.d("MicrosoftAuth", "Sign-in success: $email")
                            }

                            continuation.resume(Result.success(Triple(accessToken, email, displayName)))
                        }

                        override fun onError(exception: MsalException) {
                            if (BuildConfig.DEBUG) {
                                android.util.Log.e("MicrosoftAuth", "Sign-in error", exception)
                            }

                            when (exception) {
                                is MsalUserCancelException -> {
                                    continuation.resume(Result.failure(MicrosoftAuthException.Cancelled))
                                }
                                else -> {
                                    continuation.resume(
                                        Result.failure(
                                            MicrosoftAuthException.Failed(
                                                exception.message ?: "Unknown error"
                                            )
                                        )
                                    )
                                }
                            }
                        }

                        override fun onCancel() {
                            continuation.resume(Result.failure(MicrosoftAuthException.Cancelled))
                        }
                    }
                )
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }

    suspend fun signOut() {
        try {
            val app = getPublicClientApp()
            app.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("MicrosoftAuth", "Sign-out success")
                    }
                }

                override fun onError(exception: MsalException) {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.e("MicrosoftAuth", "Sign-out error", exception)
                    }
                }
            })
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("MicrosoftAuth", "Sign-out failed", e)
            }
        }
    }
}

sealed class MicrosoftAuthException : Exception() {
    data object Cancelled : MicrosoftAuthException()
    data class Failed(override val message: String) : MicrosoftAuthException()
}
