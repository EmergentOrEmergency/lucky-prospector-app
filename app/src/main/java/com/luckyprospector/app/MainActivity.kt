package com.luckyprospector.app

import android.graphics.Color
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.luckyprospector.app.ui.LuckyProspectorApp
import com.luckyprospector.app.ui.AppLanguageManager
import com.luckyprospector.app.ui.theme.LuckyProspectorTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        setContent {
            LuckyProspectorTheme {
                LuckyProspectorApp()
            }
        }
    }

    private fun configureSystemBars() {
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.rgb(5, 47, 32)
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.rgb(5, 47, 32)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }
}

