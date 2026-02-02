package com.tomasronis.rhentiapp.data.auth.services

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.tomasronis.rhentiapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun signIn(activity: Activity): Result<Pair<String, String>> = withContext(Dispatchers.Main) {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            when (val credential = result.credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                        Result.success(googleCred.idToken to googleCred.id)
                    } else {
                        Result.failure(Exception("Unexpected credential type"))
                    }
                }
                else -> Result.failure(Exception("Unexpected credential"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(GoogleAuthException.Cancelled)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("GoogleAuthService", "Sign-in failed", e)
            }
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        val credentialManager = CredentialManager.create(context)
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}

sealed class GoogleAuthException : Exception() {
    data object Cancelled : GoogleAuthException()
}
