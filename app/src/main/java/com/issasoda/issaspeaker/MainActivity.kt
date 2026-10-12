package com.issasoda.issaspeaker

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.issasoda.issaspeaker.data.settings.AppSettings
import com.issasoda.issaspeaker.ui.IssaSpeakerApp

class MainActivity : ComponentActivity() {

    private val settings by lazy { AppSettings(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IssaSpeakerApp(settings = settings, onDarkThemeChanged = ::styleSystemBars)
        }
    }

    /** Status and navigation bar icons follow the app's theme choice, not only the phone's. */
    private fun styleSystemBars(dark: Boolean) {
        val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
