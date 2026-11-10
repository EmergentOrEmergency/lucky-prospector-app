package com.luckyprospector.app.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The signed-in account id, following Firebase's own listener.
 *
 * ViewModels use this rather than [FirebaseAuthProvider], which needs an Activity
 * for the account picker and must not outlive it.
 */
fun FirebaseAuth.uidChanges(): Flow<String?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}.distinctUntilChanged()
