package com.luckyprospector.app.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.luckyprospector.app.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Google sign-in through Credential Manager, exchanged for a Firebase session.
 *
 * [activityContext] must be an Activity: Credential Manager shows the account
 * picker itself, and an application context has no window to show it in. Passing
 * one fails at the moment the user taps sign in, not at construction, which is
 * why it is stated here rather than left to be discovered.
 *
 * The server client id comes from `default_web_client_id`, a string resource the
 * google-services plugin generates from `app/google-services.json`. Reading it
 * from resources rather than pasting the value means it cannot drift out of step
 * with the Firebase project.
 */
class FirebaseAuthProvider(
    private val activityContext: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val credentialManager: CredentialManager = CredentialManager.create(activityContext),
) : AuthProvider {

    private val _currentUser = MutableStateFlow(auth.currentUser?.toAuthUser())
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    init {
        // Firebase restores a session on its own after a restart, and can drop one
        // when a token is revoked, so the app follows the listener rather than
        // assuming the result of the last sign-in still holds.
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser?.toAuthUser()
        }
    }

    override suspend fun signIn(): AuthResult {
        return try {
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken(), null)
            val user = auth.signInWithCredential(firebaseCredential).await().user
                ?: return AuthResult.Failure(IllegalStateException("Firebase returned no user"))

            AuthResult.Success(user.toAuthUser())
        } catch (cancelled: GetCredentialCancellationException) {
            // The user dismissed the picker. That is a choice, not a fault, and
            // showing an error for it reads as the app arguing with them.
            AuthResult.Cancelled
        } catch (cancellation: CancellationException) {
            // Coroutine cancellation must keep propagating; swallowing it here
            // would leave the calling scope believing the work is still running.
            throw cancellation
        } catch (credentialFailure: GetCredentialException) {
            AuthResult.Failure(credentialFailure)
        } catch (failure: Exception) {
            AuthResult.Failure(failure)
        }
    }

    /** Shows the Google account picker and returns the chosen account's ID token. */
    private suspend fun googleIdToken(): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            // Offering every account on the device, not only those already
            // authorised, keeps a first sign-in from looking like an empty
            // picker to someone who has never used the app.
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(activityContext.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credential = credentialManager.getCredential(activityContext, request).credential

        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type: ${credential.type}"
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    override suspend fun deleteAccount() {
        val user = auth.currentUser ?: return
        try {
            user.delete().await()
        } catch (stale: FirebaseAuthRecentLoginRequiredException) {
            // Firebase deletes an account only right after a sign-in. Asking for the
            // account again here is the documented remedy, not a second sign-up.
            user.reauthenticate(GoogleAuthProvider.getCredential(googleIdToken(), null)).await()
            user.delete().await()
        }
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    override suspend fun signOut() {
        auth.signOut()
        // Also clear the picker's own state, otherwise the next sign-in silently
        // reuses the account the user just signed out of.
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        displayName = displayName,
        photoUrl = photoUrl?.toString(),
    )
}
