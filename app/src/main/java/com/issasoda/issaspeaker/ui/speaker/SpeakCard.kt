package com.issasoda.issaspeaker.ui.speaker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.speaker.VoiceOption
import com.issasoda.issaspeaker.ui.components.SectionCard
import com.issasoda.issaspeaker.ui.theme.Spacing
import kotlin.math.roundToInt

/** Type a text and hear it on the speaker, with the phone voice and the announcement volume chosen here. */
@Composable
fun SpeakCard(viewModel: SpeakViewModel) {
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
        VoicePicker(voices, style.voice, onVoice = viewModel::setVoice)
        // The slider keeps its own value while dragging; it is saved once the finger lifts.
        val volume = remember(style.volume) { SliderState(value = style.volume) }
        Text(stringResource(R.string.speak_volume, (volume.value * PERCENT).roundToInt()), style = MaterialTheme.typography.labelLarge)
        Slider(state = volume, onValueChange = { volume.value = it }, onValueChangeFinished = { viewModel.setVolume(volume.value) })
        if (state.running) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let {
            Text(
                stringResource(it),
                style = MaterialTheme.typography.bodyLarge,
                color = if (state.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { viewModel.speak(text) }, enabled = text.isNotBlank() && !state.running) {
                Text(stringResource(R.string.speak_button))
            }
        }
    }
}

/** The phone's voices for the app language; "Automatic" lets the engine choose. Offline voices are marked. */
@Composable
private fun VoicePicker(voices: List<VoiceOption>, selected: String?, onVoice: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val automatic = stringResource(R.string.speak_voice_auto)
    val offline = stringResource(R.string.speak_voice_offline)
    fun label(voice: VoiceOption?): String = voice?.let {
        listOfNotNull(it.locale.displayName, it.name, offline.takeIf { _ -> voice.offline }).joinToString(" · ")
    } ?: automatic
    Text(stringResource(R.string.speak_voice), style = MaterialTheme.typography.labelLarge)
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label(voices.firstOrNull { it.name == selected }), maxLines = 2)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(automatic) }, onClick = {
                onVoice(null)
                open = false
            })
            voices.forEach { voice ->
                DropdownMenuItem(
                    text = { Text(label(voice), modifier = Modifier.padding(vertical = Spacing.xs)) },
                    onClick = {
                        onVoice(voice.name)
                        open = false
                    },
                )
            }
        }
    }
}

private const val PERCENT = 100
