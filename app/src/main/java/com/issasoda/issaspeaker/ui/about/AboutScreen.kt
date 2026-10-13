package com.issasoda.issaspeaker.ui.about

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
import com.issasoda.issaspeaker.ui.RecordsState
import com.issasoda.issaspeaker.ui.components.AdaptiveGrid
import com.issasoda.issaspeaker.ui.components.ScreenScaffold
import com.issasoda.issaspeaker.ui.components.SectionCard
import com.issasoda.issaspeaker.ui.theme.FontPool
import com.issasoda.issaspeaker.ui.theme.Links

/** The About every user sees (catalog F007): what, privacy, status, help, source, security. */
@Composable
fun AboutScreen(state: AppState) {
    val uriHandler = LocalUriHandler.current
    ScreenScaffold(
        title = stringResource(R.string.action_about),
        account = state.account,
        onLogoClick = state.goHome,
        onAccountClick = state.openAccount,
    ) { padding ->
        AdaptiveGrid(padding) {
            item(key = "what") { TextCard(R.string.about_what_title, stringResource(R.string.about_what_body)) }
            item(key = "data") {
                LinkCard(R.string.about_privacy_title, stringResource(R.string.about_privacy_body), R.string.action_read_privacy) {
                    uriHandler.openUri(Links.PRIVACY)
                }
            }
            item(key = "status") {
                val ready = (state.records as? RecordsState.Ready)?.records
                val progress = ready?.let {
                    " " + stringResource(R.string.features_ready, it.count(FeatureStatus.IMPLEMENTED), it.features.size)
                }.orEmpty()
                val body = stringResource(R.string.about_status_body) + progress + "\n" +
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME)
                TextCard(R.string.about_status_title, body)
            }
            item(key = "help") {
                LinkCard(R.string.about_help_title, stringResource(R.string.about_help_body), R.string.about_help_action) {
                    uriHandler.openUri(Links.SUPPORT_EMAIL)
                }
            }
            item(key = "fonts") {
                TextCard(R.string.about_font_title, stringResource(R.string.about_font_body, FontPool.families.size))
            }
            item(key = "languages") { TextCard(R.string.about_languages_title, stringResource(R.string.about_languages_body)) }
            item(key = "source") {
                LinkCard(R.string.about_source_title, stringResource(R.string.about_source_body), R.string.about_source_link) {
                    uriHandler.openUri(Links.REPOSITORY)
                }
            }
            item(key = "security") {
                LinkCard(R.string.security, stringResource(R.string.about_security_body), R.string.about_security_link) {
                    uriHandler.openUri(Links.SECURITY_POLICY)
                }
            }
        }
    }
}

@Composable
private fun TextCard(titleRes: Int, body: String) {
    SectionCard(stringResource(titleRes)) {
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LinkCard(titleRes: Int, body: String, actionRes: Int, onClick: () -> Unit) {
    SectionCard(stringResource(titleRes)) {
        Text(body, style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = onClick) { Text(stringResource(actionRes)) }
    }
}
