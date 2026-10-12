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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.ui.AppState
import com.issasoda.issaspeaker.ui.components.ScreenColumn
import com.issasoda.issaspeaker.ui.components.ScreenScaffold
import com.issasoda.issaspeaker.ui.components.SectionCard

/**
 * The app's own languages (catalog F002). An empty tag means "follow the phone". The choice is stored by
 * Android itself (LocaleManager, per-app language) and also appears in the phone's Settings > Apps.
 */
private enum class AppLanguage(val tag: String, val label: Int) {
    SYSTEM("", R.string.settings_language_system),
    ENGLISH("en-US", R.string.language_en),
    SPANISH("es-419", R.string.language_es),
    PORTUGUESE("pt-BR", R.string.language_pt),
}

@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit) {
    val localeManager = LocalContext.current.getSystemService(LocaleManager::class.java)
    val current = localeManager.applicationLocales.toLanguageTags()
    ScreenScaffold(title = stringResource(R.string.action_settings), onBack = onBack) { padding ->
        ScreenColumn(padding) {
            SectionCard(stringResource(R.string.settings_language)) {
                Column(Modifier.selectableGroup()) {
                    AppLanguage.entries.forEach { language ->
                        val selected = current == language.tag
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    // Android recreates the activity in the new language by itself.
                                    onClick = { localeManager.applicationLocales = LocaleList.forLanguageTags(language.tag) },
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Text(stringResource(language.label), Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            SectionCard(stringResource(R.string.settings_font)) {
                Text(stringResource(R.string.settings_font_body, state.fontName), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
