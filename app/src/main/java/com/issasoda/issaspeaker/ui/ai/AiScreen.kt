package com.issasoda.issaspeaker.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.ai.ChatTurn
import com.issasoda.issaspeaker.ai.SavedProvider
import com.issasoda.issaspeaker.ui.AppState
import com.issasoda.issaspeaker.ui.components.AdaptiveGrid
import com.issasoda.issaspeaker.ui.components.ScreenScaffold
import com.issasoda.issaspeaker.ui.components.SectionCard
import com.issasoda.issaspeaker.ui.components.fullWidth
import com.issasoda.issaspeaker.ui.theme.Sizes
import com.issasoda.issaspeaker.ui.theme.Spacing

/**
 * The AI tab: a question box that uses the default provider, one card per provider the owner added, and the way
 * to add another. Adding (or changing a model) takes the whole screen ([AddProviderFlow]).
 */
@Composable
fun AiScreen(app: AppState, viewModel: AiViewModel, add: AddProviderViewModel) {
    val flow by add.state.collectAsStateWithLifecycle()
    val chat by viewModel.chat.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(add) { add.messages.collect { snackbar.showSnackbar(resources.getString(it)) } }

    val step = flow.step
    if (step != null) {
        AddProviderFlow(flow, step, add, snackbar)
        return
    }
    var removing by remember { mutableStateOf<SavedProvider?>(null) }
    ScreenScaffold(
        title = stringResource(R.string.nav_ai),
        account = app.account,
        onLogoClick = app.goHome,
        onAccountClick = app.openAccount,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        AdaptiveGrid(padding, maxColumns = 3) {
            fullWidth("ask") {
                AskCard(saved.firstOrNull { it.isDefault }, chat, viewModel)
            }
            saved.forEach { provider ->
                item(key = "provider-${provider.providerId}") {
                    SavedProviderCard(
                        provider,
                        busy = flow.busy,
                        onMakeDefault = { viewModel.makeDefault(provider) },
                        onChangeModel = { add.changeModel(provider) },
                        onRemove = { removing = provider },
                    )
                }
            }
            item(key = "add") {
                SectionCard(stringResource(R.string.ai_add_provider)) {
                    Text(stringResource(R.string.ai_add_body), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = add::start) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.ai_add_provider), Modifier.padding(start = Spacing.s))
                    }
                }
            }
        }
    }
    removing?.let { provider ->
        RemoveDialog(provider, onConfirm = { viewModel.remove(provider) }, onDismiss = { removing = null })
    }
}

/** Removing asks first: the key cannot be recovered from the app afterwards. */
@Composable
private fun RemoveDialog(provider: SavedProvider, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ai_remove_title, provider.name)) },
        text = { Text(stringResource(R.string.ai_remove_body)) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text(stringResource(R.string.ai_remove)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Questions to the default provider. The conversation lives in memory only and is sent with each question. */
@Composable
private fun AskCard(default: SavedProvider?, chat: ChatState, viewModel: AiViewModel) {
    SectionCard(stringResource(R.string.ai_ask_title)) {
        if (default == null) {
            Text(stringResource(R.string.ai_ask_empty), style = MaterialTheme.typography.bodyLarge)
            return@SectionCard
        }
        Text(
            stringResource(R.string.ai_ask_using, default.name, default.modelId),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        chat.turns.forEach { Bubble(it) }
        if (chat.sending) LinearProgressIndicator(Modifier.fillMaxWidth())
        chat.error?.let {
            Text(stringResource(R.string.ai_provider_said, it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        var question by rememberSaveable { mutableStateOf("") }
        val send = {
            viewModel.ask(question)
            question = ""
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text(stringResource(R.string.ai_ask_hint)) },
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = send, enabled = question.isNotBlank() && !chat.sending) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.ai_ask_send))
            }
        }
        if (chat.turns.isNotEmpty()) {
            TextButton(onClick = viewModel::newConversation) { Text(stringResource(R.string.ai_ask_clear)) }
        }
    }
}

/** One turn: the owner's on the right, the model's on the left (selectable, so it can be copied). */
@Composable
private fun Bubble(turn: ChatTurn) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (turn.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            color = if (turn.fromUser) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.widthIn(max = Sizes.cardMinWidth),
        ) {
            SelectionContainer {
                Text(turn.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(Spacing.m))
            }
        }
    }
}

@Composable
private fun SavedProviderCard(
    provider: SavedProvider,
    busy: Boolean,
    onMakeDefault: () -> Unit,
    onChangeModel: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProviderLogo(provider.providerId, Sizes.topBarIcon + Spacing.m)
                Column(Modifier.weight(1f).padding(start = Spacing.m)) {
                    Text(provider.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(provider.endpoint.host, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (provider.isDefault) {
                    Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.ai_default), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(stringResource(R.string.ai_model_label, provider.modelId), style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                if (!provider.isDefault) OutlinedButton(onClick = onMakeDefault) { Text(stringResource(R.string.ai_make_default)) }
                TextButton(onClick = onChangeModel, enabled = !busy) { Text(stringResource(R.string.ai_change_model)) }
                TextButton(onClick = onRemove) { Text(stringResource(R.string.ai_remove)) }
            }
        }
    }
}

/** The provider's logo from models.dev (a monochrome SVG) on a white disc, so it reads in both themes. */
@Composable
fun ProviderLogo(providerId: String, size: Dp) {
    Box(
        Modifier.size(size).background(Color.White, CircleShape).padding(size * LOGO_PADDING_RATIO),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(model = "$LOGO_BASE$providerId.svg", contentDescription = null, modifier = Modifier.size(size))
    }
}

private const val LOGO_BASE = "https://models.dev/logos/"
private const val LOGO_PADDING_RATIO = 0.16f
