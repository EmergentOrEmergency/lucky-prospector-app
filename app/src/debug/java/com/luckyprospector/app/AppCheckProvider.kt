package com.luckyprospector.app

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds cannot pass Play Integrity. The debug provider logs a token
 * ("DebugAppCheckProvider" in logcat) that must be registered in the Firebase
 * console under App Check before enforcement is enabled.
 */
internal fun appCheckProviderFactory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
