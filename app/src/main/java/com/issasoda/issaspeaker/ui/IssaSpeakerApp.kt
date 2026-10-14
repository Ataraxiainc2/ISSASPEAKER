package com.issasoda.issaspeaker.ui

import android.app.LocaleManager
import android.os.LocaleList
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.issasoda.issaspeaker.IssaSpeakerApplication
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.admin.AdminPolicy
import com.issasoda.issaspeaker.data.account.Account
import com.issasoda.issaspeaker.data.account.AccountRepository
import com.issasoda.issaspeaker.data.account.SignInResult
import com.issasoda.issaspeaker.data.records.ProjectRecords
import com.issasoda.issaspeaker.data.settings.AppSettings
import com.issasoda.issaspeaker.data.settings.ThemeMode
import com.issasoda.issaspeaker.ui.about.AboutScreen
import com.issasoda.issaspeaker.ui.about.AdminAboutScreen
import com.issasoda.issaspeaker.ui.account.AccountActions
import com.issasoda.issaspeaker.ui.account.AccountSheet
import com.issasoda.issaspeaker.ui.ai.AddProviderViewModel
import com.issasoda.issaspeaker.ui.ai.AiScreen
import com.issasoda.issaspeaker.ui.ai.AiViewModel
import com.issasoda.issaspeaker.ui.home.HomeScreen
import com.issasoda.issaspeaker.ui.settings.SettingsScreen
import com.issasoda.issaspeaker.ui.speaker.SpeakerScreen
import com.issasoda.issaspeaker.ui.speaker.SpeakerViewModel
import com.issasoda.issaspeaker.ui.theme.FontAssignment
import com.issasoda.issaspeaker.ui.theme.FontSession
import com.issasoda.issaspeaker.ui.theme.IssaSpeakerTheme
import kotlinx.coroutines.launch

enum class Destination(val label: Int, val icon: ImageVector) {
    HOME(R.string.nav_home, Icons.Filled.Home),
    SPEAKER(R.string.nav_speaker, Icons.Filled.Notifications),
    AI(R.string.nav_ai, Icons.Filled.Face),
    SETTINGS(R.string.action_settings, Icons.Filled.Settings),
    ABOUT(R.string.action_about, Icons.Filled.Info),
    ADMIN(R.string.action_admin, Icons.Filled.Lock),
}

/** Loading state of the feature catalog and audit log shipped in the APK. */
sealed interface RecordsState {
    data object Loading : RecordsState
    data class Ready(val records: ProjectRecords) : RecordsState
    data object Failed : RecordsState
}

/** Everything the screens share. */
data class AppState(
    val fonts: FontAssignment,
    val records: RecordsState,
    val account: Account?,
    val adminVisible: Boolean,
    val openAccount: () -> Unit,
    val goHome: () -> Unit,
    val accountActions: AccountActions,
)

@Composable
fun IssaSpeakerApp(settings: AppSettings, accounts: AccountRepository, onDarkThemeChanged: (Boolean) -> Unit) {
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    LaunchedEffect(darkTheme) { onDarkThemeChanged(darkTheme) }
    val fonts by FontSession.fonts.collectAsStateWithLifecycle()
    val account by accounts.account.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val records by produceState<RecordsState>(RecordsState.Loading) {
        value = runCatching { ProjectRecords.load(context) }.fold({ RecordsState.Ready(it) }, { RecordsState.Failed })
    }
    val adminVisible = AdminPolicy.canOpenAdminTools(account?.email, account?.emailVerified == true)

    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    if (destination == Destination.ADMIN && !adminVisible) destination = Destination.HOME
    BackHandler(enabled = destination != Destination.HOME) { destination = Destination.HOME }

    var showAccount by rememberSaveable { mutableStateOf(false) }
    val accountActions = rememberAccountActions(accounts, settings) { showAccount = false }
    val state = AppState(
        fonts = fonts,
        records = records,
        account = account,
        adminVisible = adminVisible,
        openAccount = { showAccount = true },
        goHome = { destination = Destination.HOME },
        accountActions = accountActions,
    )

    IssaSpeakerTheme(fonts, darkTheme) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                Destination.entries.filter { it != Destination.ADMIN || adminVisible }.forEach { item ->
                    item(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.label)) },
                    )
                }
            },
        ) {
            DestinationScreen(destination, state, themeMode, settings) { destination = it }
            if (showAccount) AccountSheet(account, accountActions) { showAccount = false }
        }
    }
}

/** The screen of one navigation destination. */
@Composable
private fun DestinationScreen(destination: Destination, state: AppState, themeMode: ThemeMode, settings: AppSettings, onNavigate: (Destination) -> Unit) {
    when (destination) {
        Destination.HOME -> HomeScreen(state, onOpenSettings = { onNavigate(Destination.SETTINGS) })
        Destination.SPEAKER -> SpeakerScreen(state, viewModel<SpeakerViewModel>())
        Destination.AI -> AiScreen(state, viewModel<AiViewModel>(), viewModel<AddProviderViewModel>())
        Destination.SETTINGS -> SettingsScreen(state, themeMode, settings::setThemeMode)
        Destination.ABOUT -> AboutScreen(state)
        Destination.ADMIN -> AdminAboutScreen(state)
    }
}

/** Sign in, sign out and erase, with the busy flag and the status line the account panels show. */
@Composable
private fun rememberAccountActions(accounts: AccountRepository, settings: AppSettings, onDone: () -> Unit): AccountActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val notConfigured = stringResource(R.string.account_error_not_configured)
    val failed = stringResource(R.string.account_error_failed)
    return AccountActions(
        busy = busy,
        message = message,
        onSignIn = {
            scope.launch {
                busy = true
                message = when (val result = accounts.signIn(context)) {
                    is SignInResult.Success -> null.also { onDone() }
                    SignInResult.Cancelled -> null
                    SignInResult.NotConfigured -> notConfigured
                    is SignInResult.Failed -> failed
                }
                busy = false
            }
        },
        onSignOut = { scope.launch { accounts.signOut().also { onDone() } } },
        onEraseEverything = {
            scope.launch {
                accounts.signOut()
                settings.clear()
                val services = context.applicationContext as IssaSpeakerApplication
                services.aiKeys.eraseAll()
                services.speakerStore.clear()
                context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.getEmptyLocaleList()
                onDone()
            }
        },
    )
}
