package com.issasoda.issaspeaker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.issasoda.issaspeaker.admin.AdminPolicy
import com.issasoda.issaspeaker.data.records.ProjectRecords
import com.issasoda.issaspeaker.data.settings.AppSettings
import com.issasoda.issaspeaker.data.settings.ThemeMode
import com.issasoda.issaspeaker.ui.about.AboutScreen
import com.issasoda.issaspeaker.ui.about.AdminAboutScreen
import com.issasoda.issaspeaker.ui.home.HomeScreen
import com.issasoda.issaspeaker.ui.settings.SettingsScreen
import com.issasoda.issaspeaker.ui.theme.FontAssignment
import com.issasoda.issaspeaker.ui.theme.FontPool
import com.issasoda.issaspeaker.ui.theme.IssaSpeakerTheme

private enum class Screen { HOME, ABOUT, ADMIN, SETTINGS }

/** Everything the screens share: this session's typefaces and the project records. */
data class AppState(val fonts: FontAssignment, val records: ProjectRecords?, val adminVisible: Boolean)

@Composable
fun IssaSpeakerApp(settings: AppSettings, onDarkThemeChanged: (Boolean) -> Unit) {
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    LaunchedEffect(darkTheme) { onDarkThemeChanged(darkTheme) }

    val context = LocalContext.current
    val records by produceState<ProjectRecords?>(null) { value = ProjectRecords.load(context) }
    // No sign-in yet (M1): only debug builds open the admin tools.
    val state = AppState(FontPool.session, records, AdminPolicy.canOpenAdminTools(signedInEmail = null, emailVerified = false))

    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    IssaSpeakerTheme(state.fonts, darkTheme) {
        val goHome = { screen = Screen.HOME }
        when (screen) {
            Screen.HOME -> HomeScreen(
                state = state,
                onOpenSettings = { screen = Screen.SETTINGS },
                onOpenAbout = { screen = Screen.ABOUT },
                onOpenAdmin = { screen = Screen.ADMIN },
            )
            Screen.ABOUT -> AboutScreen(state, onBack = goHome)
            Screen.ADMIN -> AdminAboutScreen(state, onBack = goHome)
            Screen.SETTINGS -> SettingsScreen(
                state = state,
                themeMode = themeMode,
                onThemeModeChange = settings::setThemeMode,
                onBack = goHome,
            )
        }
    }
}
