package com.luckyprospector.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck

class LuckyProspectorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // Installed before any Firestore call so every request carries a token.
        // Tokens are only checked once enforcement is switched on in the Firebase
        // console; until then this changes nothing for users.
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckProviderFactory())
    }
}
