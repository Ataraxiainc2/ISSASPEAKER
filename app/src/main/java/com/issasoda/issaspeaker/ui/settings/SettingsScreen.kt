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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.data.settings.ThemeMode
import com.issasoda.issaspeaker.ui.AppState
import com.issasoda.issaspeaker.ui.components.ScreenColumn
import com.issasoda.issaspeaker.ui.components.ScreenScaffold
import com.issasoda.issaspeaker.ui.components.SectionCard
import com.issasoda.issaspeaker.ui.theme.FontRole
import com.issasoda.issaspeaker.ui.theme.fontFamilyOf

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

private val roleLabels = mapOf(
    FontRole.DISPLAY to R.string.font_role_display,
    FontRole.TITLE to R.string.font_role_title,
    FontRole.BODY to R.string.font_role_body,
    FontRole.LABEL to R.string.font_role_label,
)

@Composable
fun SettingsScreen(state: AppState, themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit, onBack: () -> Unit) {
    val localeManager = LocalContext.current.getSystemService(LocaleManager::class.java)
    val currentLanguage = localeManager.applicationLocales.toLanguageTags()
    ScreenScaffold(title = stringResource(R.string.action_settings), onBack = onBack) { padding ->
        ScreenColumn(padding) {
            SectionCard(stringResource(R.string.settings_language)) {
                RadioGroup(
                    options = AppLanguage.entries,
                    selected = AppLanguage.entries.firstOrNull { it.tag == currentLanguage } ?: AppLanguage.SYSTEM,
                    label = { stringResource(it.label) },
                    // Android recreates the activity in the new language by itself.
                    onSelect = { localeManager.applicationLocales = LocaleList.forLanguageTags(it.tag) },
                )
            }
            SectionCard(stringResource(R.string.settings_theme)) {
                RadioGroup(
                    options = ThemeMode.entries,
                    selected = themeMode,
                    label = { stringResource(themeLabels.getValue(it)) },
                    onSelect = onThemeModeChange,
                )
            }
            SectionCard(stringResource(R.string.settings_fonts)) {
                Text(stringResource(R.string.settings_fonts_body), style = MaterialTheme.typography.bodyMedium)
                FontRole.entries.forEach { role ->
                    val family = state.fonts.familyFor(role)
                    val fontFamily = remember(family) { fontFamilyOf(family) }
                    Column(Modifier.padding(top = 12.dp)) {
                        Text(stringResource(roleLabels.getValue(role)), style = MaterialTheme.typography.labelMedium)
                        Text(family, style = MaterialTheme.typography.titleLarge.copy(fontFamily = fontFamily))
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> RadioGroup(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    Column(Modifier.selectableGroup()) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelect(option) })
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option == selected, onClick = null)
                Text(label(option), Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
