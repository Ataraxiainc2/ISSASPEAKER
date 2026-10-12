package com.issasoda.issaspeaker.ui.about

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.issasoda.issaspeaker.BuildConfig
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.data.records.FeatureStatus
import com.issasoda.issaspeaker.ui.AppState
import com.issasoda.issaspeaker.ui.components.ScreenColumn
import com.issasoda.issaspeaker.ui.components.ScreenScaffold
import com.issasoda.issaspeaker.ui.components.SectionCard
import com.issasoda.issaspeaker.ui.theme.FontPool

private const val REPOSITORY_URL = "https://github.com/Ataraxiainc2/ISSASPEAKER"
private const val SECURITY_POLICY_URL = "$REPOSITORY_URL/blob/main/SECURITY.md"

/** The About every user sees (catalog F007). */
@Composable
fun AboutScreen(state: AppState, onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    ScreenScaffold(title = stringResource(R.string.action_about), onBack = onBack) { padding ->
        ScreenColumn(padding) {
            Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelLarge)
            TextSection(R.string.about_what_title, stringResource(R.string.about_what_body))
            TextSection(R.string.about_privacy_title, stringResource(R.string.about_privacy_body))
            state.records?.let { records ->
                TextSection(
                    R.string.about_status_title,
                    stringResource(R.string.about_status_body) + " " +
                        stringResource(R.string.features_ready, records.count(FeatureStatus.IMPLEMENTED), records.features.size),
                )
            }
            TextSection(R.string.about_font_title, stringResource(R.string.about_font_body, FontPool.families.size))
            TextSection(R.string.about_languages_title, stringResource(R.string.about_languages_body))
            SectionCard(stringResource(R.string.about_source_title)) {
                Text(stringResource(R.string.about_source_body), style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { uriHandler.openUri(REPOSITORY_URL) }) { Text(stringResource(R.string.about_source_link)) }
            }
            SectionCard(stringResource(R.string.security)) {
                Text(stringResource(R.string.about_security_body), style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { uriHandler.openUri(SECURITY_POLICY_URL) }) { Text(stringResource(R.string.about_security_link)) }
            }
        }
    }
}

@Composable
private fun TextSection(titleRes: Int, body: String) {
    SectionCard(stringResource(titleRes)) {
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}
