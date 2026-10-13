package com.issasoda.issaspeaker.ui.settings

import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.data.settings.ThemeMode
import com.issasoda.issaspeaker.ui.AppState
import com.issasoda.issaspeaker.ui.account.AccountPanel
import com.issasoda.issaspeaker.ui.components.AdaptiveGrid
import com.issasoda.issaspeaker.ui.components.ScreenScaffold
import com.issasoda.issaspeaker.ui.components.SectionCard
import com.issasoda.issaspeaker.ui.theme.Links
import com.issasoda.issaspeaker.ui.theme.Spacing

/**
 * The app's own languages (catalog F002). An empty tag means "follow the phone". Android stores the choice
 * itself (LocaleManager) and also shows it in the phone's Settings > Apps.
 */
private enum class AppLanguage(val tag: String, val label: Int) {
    SYSTEM("", R.string.settings_language_system),
    ENGLISH("en-US", R.string.language_en),
    SPANISH("es-419", R.string.language_es),
    PORTUGUESE("pt-BR", R.string.language_pt),
}

private val themeLabels = mapOf(
    ThemeMode.SYSTEM to R.string.theme_system,
    ThemeMode.LIGHT to R.string.theme_light,
    ThemeMode.DARK to R.string.theme_dark,
)

@Composable
fun SettingsScreen(state: AppState, themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val localeManager = LocalContext.current.getSystemService(LocaleManager::class.java)
    val currentLanguage = localeManager.applicationLocales.toLanguageTags()
    val uriHandler = LocalUriHandler.current
    ScreenScaffold(
        title = stringResource(R.string.action_settings),
        account = state.account,
        onLogoClick = state.goHome,
        onAccountClick = state.openAccount,
    ) { padding ->
        AdaptiveGrid(padding, maxColumns = 4) {
            item(key = "account") {
                SectionCard(stringResource(R.string.settings_account)) {
                    AccountPanel(state.account, state.accountActions)
                }
            }
            item(key = "language") {
                SectionCard(stringResource(R.string.settings_language)) {
                    RadioGroup(
                        options = AppLanguage.entries,
                        selected = AppLanguage.entries.firstOrNull { it.tag == currentLanguage } ?: AppLanguage.SYSTEM,
                        // Android recreates the activity in the new language by itself.
                        onSelect = { localeManager.applicationLocales = LocaleList.forLanguageTags(it.tag) },
                    ) { stringResource(it.label) }
                }
            }
            item(key = "theme") {
                SectionCard(stringResource(R.string.settings_theme)) {
                    RadioGroup(
                        options = ThemeMode.entries,
                        selected = themeMode,
                        onSelect = onThemeModeChange,
                    ) { stringResource(themeLabels.getValue(it)) }
                }
            }
            item(key = "privacy") {
                SectionCard(stringResource(R.string.settings_privacy)) {
                    Text(stringResource(R.string.settings_privacy_body), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { uriHandler.openUri(Links.PRIVACY) }) { Text(stringResource(R.string.action_read_privacy)) }
                }
            }
        }
    }
}

@Composable
private fun <T> RadioGroup(options: List<T>, selected: T, onSelect: (T) -> Unit, label: @Composable (T) -> String) {
    Column(Modifier.selectableGroup()) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelect(option) })
                    .padding(vertical = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option == selected, onClick = null)
                Text(label(option), Modifier.padding(start = Spacing.m), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
