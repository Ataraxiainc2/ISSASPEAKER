package com.issasoda.issaspeaker.ui.speaker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.ai.VoiceStyle
import com.issasoda.issaspeaker.speaker.VoiceChoice
import com.issasoda.issaspeaker.speaker.VoiceEntry
import com.issasoda.issaspeaker.speaker.VoiceGroup
import com.issasoda.issaspeaker.ui.ai.ProviderLogo
import com.issasoda.issaspeaker.ui.theme.Sizes
import com.issasoda.issaspeaker.ui.theme.Spacing

/**
 * All voices as cards over the whole screen: the phone's (free, private) and, for each AI provider whose key can
 * speak, its voices (the owner's credit). Tap a card to choose it; the play button previews it on the phone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChooser(viewModel: SpeakViewModel, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val voices by viewModel.voices.collectAsStateWithLifecycle()
    val style by viewModel.style.collectAsStateWithLifecycle()
    val selected = VoiceChoice.decode(style.voice)
    val sample = stringResource(R.string.speak_sample)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voice_choose)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = VOICE_CARD_WIDTH),
            contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, bottom = Spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            voices.phone?.let { group(it, selected, voices.previewing, viewModel, sample) }
            voices.ai.forEach { group(it, selected, voices.previewing, viewModel, sample) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (voices.loadingAi) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(stringResource(R.string.voice_ai_loading), style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (voices.ai.isEmpty()) {
                    Text(
                        stringResource(R.string.voice_ai_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun LazyGridScope.group(group: VoiceGroup, selected: VoiceChoice, previewing: VoiceChoice?, viewModel: SpeakViewModel, sample: String) {
    item(span = { GridItemSpan(maxLineSpan) }, key = "header-${group.provider?.providerId ?: "phone"}") { GroupHeader(group) }
    items(group.voices, key = { it.choice.encode() ?: "automatic" }) { entry ->
        VoiceCard(
            entry,
            selected = entry.choice == selected,
            previewing = entry.choice == previewing,
            onSelect = { viewModel.chooseVoice(entry.choice) },
            onPreview = { viewModel.preview(entry.choice, sample) },
        )
    }
}

@Composable
private fun GroupHeader(group: VoiceGroup) {
    Row(Modifier.padding(top = Spacing.l), verticalAlignment = Alignment.CenterVertically) {
        val provider = group.provider
        if (provider == null) {
            Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(Sizes.topBarIcon), tint = MaterialTheme.colorScheme.primary)
        } else {
            ProviderLogo(provider.providerId, Sizes.topBarIcon)
        }
        Column(Modifier.padding(start = Spacing.m)) {
            Text(
                provider?.let { "${it.name} · ${group.model}" } ?: stringResource(R.string.voice_phone_group),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                provider?.let { stringResource(R.string.voice_ai_note, it.name) } ?: stringResource(R.string.voice_phone_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VoiceCard(entry: VoiceEntry, selected: Boolean, previewing: Boolean, onSelect: () -> Unit, onPreview: () -> Unit) {
    val colors = if (selected) {
        CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        CardDefaults.elevatedCardColors()
    }
    ElevatedCard(
        colors = colors,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onSelect),
    ) {
        Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(voiceTitle(entry), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (selected) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.voice_selected),
                            modifier = Modifier.padding(start = Spacing.xs).size(Spacing.l),
                        )
                    }
                }
                Text(voiceDetail(entry), style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (previewing) {
                CircularProgressIndicator(Modifier.size(Sizes.topBarIcon).padding(Spacing.xs))
            } else {
                IconButton(onClick = onPreview) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.voice_preview, voiceTitle(entry)))
                }
            }
        }
    }
}

/** A voice's name as people see it: engine ids such as "es-us-x-esc-local" become "Voice 3". */
@Composable
fun voiceTitle(entry: VoiceEntry): String = when (val choice = entry.choice) {
    VoiceChoice.Automatic -> stringResource(R.string.voice_automatic)
    is VoiceChoice.Phone -> stringResource(R.string.voice_number, entry.number ?: 1)
    is VoiceChoice.Ai -> entry.name ?: choice.voice
}

/** Country and offline/online for phone voices, the style for AI voices, in the app's language. */
@Composable
fun voiceDetail(entry: VoiceEntry): String {
    val appLocale = LocalConfiguration.current.locales[0]
    return when (entry.choice) {
        VoiceChoice.Automatic -> stringResource(R.string.voice_automatic_detail, appLocale.getDisplayLanguage(appLocale))
        is VoiceChoice.Phone -> listOfNotNull(
            entry.locale?.getDisplayCountry(appLocale)?.takeIf { it.isNotBlank() },
            stringResource(if (entry.offline) R.string.voice_offline else R.string.voice_online),
        ).joinToString(" · ")
        is VoiceChoice.Ai -> entry.style?.let { stringResource(styleLabels.getValue(it)) } ?: stringResource(R.string.voice_ai_generic)
    }
}

private val styleLabels = mapOf(
    VoiceStyle.BRIGHT to R.string.voice_style_bright,
    VoiceStyle.UPBEAT to R.string.voice_style_upbeat,
    VoiceStyle.INFORMATIVE to R.string.voice_style_informative,
    VoiceStyle.FIRM to R.string.voice_style_firm,
    VoiceStyle.EXCITABLE to R.string.voice_style_excitable,
    VoiceStyle.YOUTHFUL to R.string.voice_style_youthful,
    VoiceStyle.BREEZY to R.string.voice_style_breezy,
    VoiceStyle.EASY_GOING to R.string.voice_style_easy_going,
    VoiceStyle.BREATHY to R.string.voice_style_breathy,
    VoiceStyle.CLEAR to R.string.voice_style_clear,
    VoiceStyle.SMOOTH to R.string.voice_style_smooth,
    VoiceStyle.GRAVELLY to R.string.voice_style_gravelly,
    VoiceStyle.SOFT to R.string.voice_style_soft,
    VoiceStyle.EVEN to R.string.voice_style_even,
    VoiceStyle.MATURE to R.string.voice_style_mature,
    VoiceStyle.FORWARD to R.string.voice_style_forward,
    VoiceStyle.FRIENDLY to R.string.voice_style_friendly,
    VoiceStyle.CASUAL to R.string.voice_style_casual,
    VoiceStyle.GENTLE to R.string.voice_style_gentle,
    VoiceStyle.LIVELY to R.string.voice_style_lively,
    VoiceStyle.KNOWLEDGEABLE to R.string.voice_style_knowledgeable,
    VoiceStyle.WARM to R.string.voice_style_warm,
)

private val VOICE_CARD_WIDTH = 200.dp
