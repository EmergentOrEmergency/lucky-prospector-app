package com.luckyprospector.app.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * The signed-in account, reduced to what the app actually needs.
 *
 * [uid] is the only field the data layer may use as a key. Display name and
 * photo are for the UI alone and can be absent or change at any time.
 */
data class AuthUser(
    val uid: String,
    val displayName: String?,
    val photoUrl: String?,
)

sealed interface AuthResult {
    data class Success(val user: AuthUser) : AuthResult
    /** The user dismissed the account picker. Not an error; do not report it as one. */
    data object Cancelled : AuthResult
    data class Failure(val cause: Throwable) : AuthResult
}

/**
 * Boundary between the app and whatever performs the sign-in.
 *
 * It exists for the same reason [com.luckyprospector.app.inference.Detector] does:
 * the rest of the app is built and tested against the interface, so development
 * does not stall waiting for a configured Firebase project, and the tests never
 * touch the network.
 *
 * Signing in is never required to search for clovers or to save a finding
 * locally. It is required only to put something on the shared map, which is what
 * makes the offline-first promise in the README structural rather than a slogan.
 */
interface AuthProvider {
    val currentUser: StateFlow<AuthUser?>
    suspend fun signIn(): AuthResult
    suspend fun signOut()

    /**
     * Deletes the account itself. Callers must first remove what the account
     * published, because afterwards nothing can prove ownership any more.
     * Throws on failure, including when the user cancels a required re-sign-in.
     */
    suspend fun deleteAccount()
}
