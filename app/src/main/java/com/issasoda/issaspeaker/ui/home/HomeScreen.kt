package com.issasoda.issaspeaker.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.data.records.FeatureStatus
import com.issasoda.issaspeaker.ui.AppState
import com.issasoda.issaspeaker.ui.RecordsState
import com.issasoda.issaspeaker.ui.components.AdaptiveGrid
import com.issasoda.issaspeaker.ui.components.DotLine
import com.issasoda.issaspeaker.ui.components.ScreenScaffold
import com.issasoda.issaspeaker.ui.components.SectionCard
import com.issasoda.issaspeaker.ui.components.fullWidth
import com.issasoda.issaspeaker.ui.theme.Links
import com.issasoda.issaspeaker.ui.theme.Sizes
import com.issasoda.issaspeaker.ui.theme.Spacing

@Composable
fun HomeScreen(state: AppState, onOpenSettings: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    ScreenScaffold(
        title = stringResource(R.string.app_name),
        account = state.account,
        onLogoClick = state.goHome,
        onAccountClick = state.openAccount,
    ) { padding ->
        AdaptiveGrid(padding, maxColumns = 3) {
            fullWidth("hero") { Hero(state, onOpenSettings) }
            item(key = "next") {
                SectionCard(stringResource(R.string.home_next_title)) {
                    listOf(R.string.home_next_speaker, R.string.home_next_google, R.string.home_next_routine)
                        .forEachIndexed { index, text -> DotLine(stringResource(text), index) }
                }
            }
            item(key = "privacy") {
                SectionCard(stringResource(R.string.home_privacy_title)) {
                    Text(stringResource(R.string.home_privacy_body), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { uriHandler.openUri(Links.PRIVACY) }) { Text(stringResource(R.string.action_read_privacy)) }
                }
            }
            item(key = "progress") { Progress(state) }
        }
    }
}

@Composable
private fun Hero(state: AppState, onOpenSettings: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(Sizes.heroLogo))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                val account = state.account
                Text(
                    if (account == null) {
                        stringResource(R.string.home_hero_title)
                    } else {
                        stringResource(R.string.home_hero_welcome, account.displayName?.substringBefore(' ') ?: account.email)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    if (account == null) {
                        Button(onClick = state.accountActions.onSignIn, enabled = !state.accountActions.busy) {
                            Text(stringResource(R.string.account_sign_in))
                        }
                    }
                    OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.action_settings)) }
                }
                state.accountActions.message?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun Progress(state: AppState) {
    SectionCard(stringResource(R.string.home_progress_title)) {
        when (val records = state.records) {
            RecordsState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            RecordsState.Failed -> Text(stringResource(R.string.records_failed), color = MaterialTheme.colorScheme.error)
            is RecordsState.Ready -> {
                val done = records.records.count(FeatureStatus.IMPLEMENTED)
                val total = records.records.features.size
                Text(stringResource(R.string.features_ready, done, total), style = MaterialTheme.typography.bodyLarge)
                LinearProgressIndicator(progress = { done.toFloat() / total }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
