package com.issasoda.issaspeaker.ui.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.data.records.FeatureStatus
import com.issasoda.issaspeaker.ui.AppState
import com.issasoda.issaspeaker.ui.components.ScreenColumn
import com.issasoda.issaspeaker.ui.components.ScreenScaffold
import com.issasoda.issaspeaker.ui.components.SectionCard

@Composable
fun HomeScreen(state: AppState, onOpenSettings: () -> Unit, onOpenAbout: () -> Unit, onOpenAdmin: () -> Unit) {
    ScreenScaffold(
        title = stringResource(R.string.app_name),
        onBack = null,
        actions = {
            if (state.adminVisible) {
                IconButton(onClick = onOpenAdmin) {
                    Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.action_admin))
                }
            }
            IconButton(onClick = onOpenAbout) { Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.action_about)) }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.action_settings))
            }
        },
    ) { padding ->
        ScreenColumn(padding) {
            Text(stringResource(R.string.tagline), style = MaterialTheme.typography.headlineSmall)

            SectionCard(stringResource(R.string.home_font_title)) {
                Text(state.fontName, style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.home_font_body), style = MaterialTheme.typography.bodyMedium)
            }

            SectionCard(stringResource(R.string.home_next_title)) {
                listOf(R.string.home_next_speaker, R.string.home_next_google, R.string.home_next_ai, R.string.home_next_routine)
                    .forEach { Text("• " + stringResource(it), style = MaterialTheme.typography.bodyLarge) }
            }

            state.records?.let { records ->
                val done = records.count(FeatureStatus.IMPLEMENTED)
                val total = records.features.size
                SectionCard(stringResource(R.string.home_progress_title)) {
                    Text(stringResource(R.string.features_ready, done, total), style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(
                        progress = { done.toFloat() / total },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
