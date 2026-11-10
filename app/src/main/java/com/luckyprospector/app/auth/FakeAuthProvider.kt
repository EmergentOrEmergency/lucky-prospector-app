package com.luckyprospector.app.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [AuthProvider] used until a Firebase project is wired in, and in tests.
 *
 * It is deliberately obvious about being fake -- the display name says so -- because
 * a stand-in that looks like the real thing is how a demo build ends up shipped by
 * accident. The counterpart on the inference side is
 * [com.luckyprospector.app.inference.FakeDetector].
 *
 * [nextResult] lets a caller rehearse the paths that are awkward to trigger against
 * a real provider: a user who dismisses the account picker, and a failure.
 */
class FakeAuthProvider(
    initialUser: AuthUser? = null,
    var nextResult: AuthResult? = null,
) : AuthProvider {

    private val _currentUser = MutableStateFlow(initialUser)
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    override suspend fun signIn(): AuthResult {
        val result = nextResult ?: AuthResult.Success(DEMO_USER)
        if (result is AuthResult.Success) {
            _currentUser.value = result.user
        }
        return result
    }

    override suspend fun signOut() {
        _currentUser.value = null
    }

    override suspend fun deleteAccount() {
        _currentUser.value = null
    }

    companion object {
        val DEMO_USER = AuthUser(
            uid = "fake-uid-0001",
            displayName = "Utente di prova (accesso simulato)",
            photoUrl = null,
        )
    }
}
