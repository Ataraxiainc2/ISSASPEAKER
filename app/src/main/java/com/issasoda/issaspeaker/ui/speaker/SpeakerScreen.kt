package com.issasoda.issaspeaker.ui.speaker

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.speaker.FoundSpeaker
import com.issasoda.issaspeaker.speaker.Speaker
import com.issasoda.issaspeaker.ui.AppState
import com.issasoda.issaspeaker.ui.components.AdaptiveGrid
import com.issasoda.issaspeaker.ui.components.DotLine
import com.issasoda.issaspeaker.ui.components.ScreenScaffold
import com.issasoda.issaspeaker.ui.components.SectionCard
import com.issasoda.issaspeaker.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * The speaker tab: the chosen speaker and its connection test, the speakers found on this Wi-Fi, and what the
 * owner needs before any shows up (set up in Google Home, same Wi-Fi, local network allowed).
 */
@Composable
fun SpeakerScreen(app: AppState, viewModel: SpeakerViewModel, speak: SpeakViewModel) {
    val chosen by viewModel.chosen.collectAsStateWithLifecycle()
    val found by viewModel.found.collectAsStateWithLifecycle()
    val test by viewModel.test.collectAsStateWithLifecycle()
    var needsPermission by remember { mutableStateOf(viewModel.needsLocalNetworkPermission()) }
    var choosingVoice by rememberSaveable { mutableStateOf(false) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        needsPermission = viewModel.needsLocalNetworkPermission()
    }
    // Search only while the screen is visible and the network may be reached.
    LifecycleResumeEffect(needsPermission) {
        if (!needsPermission) viewModel.startSearch()
        onPauseOrDispose { viewModel.stopSearch() }
    }

    if (choosingVoice) {
        VoiceChooser(speak) { choosingVoice = false }
        return
    }
    ScreenScaffold(
        title = stringResource(R.string.nav_speaker),
        account = app.account,
        onLogoClick = app.goHome,
        onAccountClick = app.openAccount,
    ) { padding ->
        AdaptiveGrid(padding, maxColumns = 3) {
            item(key = "chosen") {
                ChosenSpeakerCard(chosen, test, onTest = viewModel::testConnection, onForget = viewModel::forget)
            }
            if (chosen != null) item(key = "speak") { SpeakCard(speak, onChooseVoice = { choosingVoice = true }) }
            item(key = "found") {
                SectionCard(stringResource(R.string.speaker_found_title)) {
                    if (needsPermission) {
                        Text(stringResource(R.string.speaker_permission_body), style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = { askPermission.launch(SpeakerViewModel.LOCAL_NETWORK) }) {
                            Text(stringResource(R.string.speaker_permission_allow))
                        }
                    } else {
                        FoundSpeakers(found, chosen?.deviceId, onChoose = viewModel::choose)
                    }
                }
            }
            item(key = "help") { SetupHelpCard() }
        }
    }
}

@Composable
private fun ChosenSpeakerCard(chosen: Speaker?, test: TestState, onTest: () -> Unit, onForget: () -> Unit) {
    SectionCard(stringResource(R.string.speaker_chosen_title)) {
        if (chosen == null) {
            Text(stringResource(R.string.speaker_none_chosen), style = MaterialTheme.typography.bodyLarge)
            return@SectionCard
        }
        Text(chosen.name, style = MaterialTheme.typography.headlineSmall)
        chosen.model?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (test.running) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(stringResource(R.string.speaker_test_running), style = MaterialTheme.typography.bodyMedium)
        }
        test.message?.let {
            Text(
                stringResource(it),
                style = MaterialTheme.typography.bodyLarge,
                color = if (test.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Button(onClick = onTest, enabled = !test.running) { Text(stringResource(R.string.speaker_test)) }
            TextButton(onClick = onForget, enabled = !test.running) { Text(stringResource(R.string.speaker_forget)) }
        }
    }
}

/** The live list; after a few seconds without results it says so instead of spinning forever. */
@Composable
private fun FoundSpeakers(found: List<FoundSpeaker>, chosenId: String?, onChoose: (Speaker) -> Unit) {
    var waitedLong by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(NOTHING_FOUND_AFTER_MS)
        waitedLong = true
    }
    if (found.isEmpty()) {
        if (!waitedLong) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(
            stringResource(if (waitedLong) R.string.speaker_nothing_found else R.string.speaker_searching),
            style = MaterialTheme.typography.bodyLarge,
        )
        return
    }
    Column(Modifier.selectableGroup()) {
        found.forEach { item ->
            val selected = item.speaker.deviceId == chosenId
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onChoose(item.speaker) })
                    .padding(vertical = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected, onClick = null)
                Column(Modifier.padding(start = Spacing.m)) {
                    Text(item.speaker.name, style = MaterialTheme.typography.bodyLarge)
                    val detail = listOfNotNull(item.speaker.model, stringResource(R.string.speaker_group).takeIf { item.isGroup })
                    if (detail.isNotEmpty()) {
                        Text(detail.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** What has to be true before a speaker can appear, and a way to Google Home to set one up. */
@Composable
private fun SetupHelpCard() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    SectionCard(stringResource(R.string.speaker_help_title)) {
        listOf(R.string.speaker_help_setup, R.string.speaker_help_wifi, R.string.speaker_help_permission)
            .forEachIndexed { index, text -> DotLine(stringResource(text), index) }
        OutlinedButton(onClick = {
            val launch: Intent? = context.packageManager.getLaunchIntentForPackage(GOOGLE_HOME_PACKAGE)
            if (launch != null) context.startActivity(launch) else uriHandler.openUri(GOOGLE_HOME_STORE)
        }) { Text(stringResource(R.string.speaker_open_google_home)) }
    }
}

private const val GOOGLE_HOME_PACKAGE = "com.google.android.apps.chromecast.app"
private const val GOOGLE_HOME_STORE = "https://play.google.com/store/apps/details?id=$GOOGLE_HOME_PACKAGE"
private const val NOTHING_FOUND_AFTER_MS = 12_000L
