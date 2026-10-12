package com.issasoda.issaspeaker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.issasoda.issaspeaker.admin.AdminPolicy
import com.issasoda.issaspeaker.data.records.ProjectRecords
import com.issasoda.issaspeaker.ui.about.AboutScreen
import com.issasoda.issaspeaker.ui.about.AdminAboutScreen
import com.issasoda.issaspeaker.ui.home.HomeScreen
import com.issasoda.issaspeaker.ui.settings.SettingsScreen
import com.issasoda.issaspeaker.ui.theme.FontPool
import com.issasoda.issaspeaker.ui.theme.IssaSpeakerTheme
import java.time.LocalDate

private enum class Screen { HOME, ABOUT, ADMIN, SETTINGS }

/** Everything the screens share: today's typeface and the project records. */
data class AppState(val fontName: String, val records: ProjectRecords?, val adminVisible: Boolean)

@Composable
fun IssaSpeakerApp() {
    // Re-read the date on every resume, so an app left open overnight wears the new day's typeface.
    var epochDay by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    LifecycleResumeEffect(Unit) {
        epochDay = LocalDate.now().toEpochDay()
        onPauseOrDispose { }
    }
    val fontName = FontPool.familyNameFor(epochDay)
    val fontFamily = remember(fontName) { FontPool.fontFamily(fontName) }

    val context = LocalContext.current
    val records by produceState<ProjectRecords?>(null) { value = ProjectRecords.load(context) }
    // No sign-in yet (M1): only debug builds open the admin tools.
    val state = AppState(fontName, records, AdminPolicy.canOpenAdminTools(signedInEmail = null, emailVerified = false))

    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    IssaSpeakerTheme(fontFamily) {
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
            Screen.SETTINGS -> SettingsScreen(state, onBack = goHome)
        }
    }
}
