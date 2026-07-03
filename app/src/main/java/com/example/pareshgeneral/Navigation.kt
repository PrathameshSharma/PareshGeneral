package com.example.pareshgeneral

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.pareshgeneral.ui.login.LockScreen
import com.example.pareshgeneral.ui.main.DashboardScreen
import com.example.pareshgeneral.ui.splash.SplashScreen

sealed interface AppScreen {
    object Splash : AppScreen
    object Lock : AppScreen
    object Dashboard : AppScreen
}

@Composable
fun MainNavigation() {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }

    when (currentScreen) {
        AppScreen.Splash -> {
            SplashScreen(
                onSplashComplete = {
                    currentScreen = AppScreen.Lock
                }
            )
        }
        AppScreen.Lock -> {
            LockScreen(
                onUnlockSuccess = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }
        AppScreen.Dashboard -> {
            DashboardScreen(
                onLogout = {
                    currentScreen = AppScreen.Lock
                }
            )
        }
    }
}
