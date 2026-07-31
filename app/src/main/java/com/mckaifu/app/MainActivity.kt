package com.mckaifu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mckaifu.app.ui.navigation.MainNavHost
import com.mckaifu.app.ui.theme.McKaiFuTheme
import com.mckaifu.app.ui.theme.ZalithThemeState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ZalithThemeState.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val mode = ZalithThemeState.themeMode.value
            val systemDark = isSystemInDarkTheme()
            val dark = when (mode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }
            McKaiFuTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainNavHost()
                }
            }
        }
    }
}
