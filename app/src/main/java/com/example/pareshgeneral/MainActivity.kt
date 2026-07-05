package com.example.pareshgeneral

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.pareshgeneral.theme.PareshGeneralTheme

import android.content.Context
import com.example.pareshgeneral.theme.ThemeConfig

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val prefs = getSharedPreferences("PareshGeneralPrefs", Context.MODE_PRIVATE)
    val savedPref = prefs.getString("user_theme_pref", "system")
    ThemeConfig.isDarkTheme = when (savedPref) {
      "light" -> false
      "dark" -> true
      else -> null
    }

    enableEdgeToEdge()
    setContent {
      PareshGeneralTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
