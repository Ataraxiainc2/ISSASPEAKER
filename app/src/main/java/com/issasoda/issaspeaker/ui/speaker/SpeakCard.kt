package com.issasoda.issaspeaker.ui.speaker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.speaker.VoiceChoice
import com.issasoda.issaspeaker.ui.components.SectionCard
import com.issasoda.issaspeaker.ui.theme.Spacing
import kotlin.math.roundToInt

/** Type a text and hear it on the speaker, with the voice and announcement volume chosen here. */
@Composable
fun SpeakCard(viewModel: SpeakViewModel, onChooseVoice: () -> Unit) {
    val style by viewModel.style.collectAsStateWithLifecycle()
    val voices by viewModel.voices.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sample = stringResource(R.string.speak_sample)
    var text by rememberSaveable { mutableStateOf(sample) }
    SectionCard(stringResource(R.string.speak_title)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.speak_text_label)) },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.speak(text) }),
            modifier = Modifier.fillMaxWidth(),
        )
        // The chosen voice, found in the loaded groups so it shows its human name and details.
        val choice = VoiceChoice.decode(style.voice)
        val group = (listOfNotNull(voices.phone) + voices.ai).firstOrNull { g -> g.voices.any { it.choice == choice } }
        val entry = group?.voices?.firstOrNull { it.choice == choice }
        Column {
            Text(stringResource(R.string.speak_voice), style = MaterialTheme.typography.labelLarge)
            Text(
                entry?.let { listOfNotNull(group.provider?.name, voiceTitle(it), voiceDetail(it)).joinToString(" · ") }
                    ?: stringResource(R.string.voice_automatic),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        OutlinedButton(onClick = onChooseVoice) { Text(stringResource(R.string.voice_choose)) }
        // The slider keeps its own value while dragging; it is saved once the finger lifts.
        val volume = remember(style.volume) { SliderState(value = style.volume) }
        Text(stringResource(R.string.speak_volume, (volume.value * PERCENT).roundToInt()), style = MaterialTheme.typography.labelLarge)
        Slider(state = volume, onValueChange = { volume.value = it }, onValueChangeFinished = { viewModel.setVolume(volume.value) })
        if (state.running) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let { message ->
            Text(
                state.arg?.let { stringResource(message, it) } ?: stringResource(message),
                style = MaterialTheme.typography.bodyLarge,
                color = if (state.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Button(onClick = { viewModel.speak(text) }, enabled = text.isNotBlank() && !state.running) {
                Text(stringResource(R.string.speak_button))
            }
        }
    }
}

private const val PERCENT = 100
