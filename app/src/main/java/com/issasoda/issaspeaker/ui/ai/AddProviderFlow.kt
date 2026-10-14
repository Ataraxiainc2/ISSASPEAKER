package com.issasoda.issaspeaker.ui.ai

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.ai.RegistryProvider
import com.issasoda.issaspeaker.ui.theme.Sizes
import com.issasoda.issaspeaker.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Adding a provider over the whole screen: the live provider list → the key → the model. Providers and models are
 * grids that fill the width (more columns in landscape), so little scrolling is needed. From the key step on the
 * window is secure: no screenshots, no recent-apps preview (catalog F016).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProviderFlow(ui: AddFlowState, step: AddStep, viewModel: AddProviderViewModel, snackbar: SnackbarHostState) {
    BackHandler(onBack = viewModel::back)
    val window = LocalActivity.current?.window
    val secure = step !is AddStep.ChooseProvider
    DisposableEffect(secure) {
        if (secure) window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (step) {
                            AddStep.ChooseProvider -> stringResource(R.string.ai_choose_provider)
                            is AddStep.EnterKey -> step.provider.name
                            is AddStep.ChooseModel -> step.provider.name
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            ui.providerError?.let {
                Text(
                    stringResource(R.string.ai_provider_said, it),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(Spacing.l),
                )
            }
            when (step) {
                AddStep.ChooseProvider -> ProviderGrid(ui.registry, viewModel)
                is AddStep.EnterKey -> KeyEntry(step.provider, busy = ui.busy, onSubmit = viewModel::submitKey)
                is AddStep.ChooseModel -> ModelGrid(step.models, busy = ui.busy, onChoose = viewModel::chooseModel)
            }
        }
    }
}

@Composable
private fun ProviderGrid(registry: RegistryState, viewModel: AddProviderViewModel) {
    when {
        registry.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        registry.failed -> Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Text(stringResource(R.string.ai_registry_failed), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = viewModel::loadProviders) { Text(stringResource(R.string.action_retry)) }
        }
        else -> {
            // A search covers every provider; otherwise only the most widely served labs, until "Show all".
            val searching = registry.query.isNotBlank()
            val matching = registry.providers.filter {
                it.name.contains(registry.query, ignoreCase = true) || it.id.contains(registry.query, ignoreCase = true)
            }
            val shown = if (searching || registry.showAll) matching else matching.take(AddProviderViewModel.TOP_PROVIDERS)
            TileGrid(minTileWidth = PROVIDER_TILE_WIDTH) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SearchField(registry.query, viewModel::setQuery)
                        Text(
                            pluralStringResource(R.plurals.ai_registry_source, registry.providers.size, registry.providers.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(shown, key = { it.id }) { provider -> ProviderTile(provider, onClick = { viewModel.chooseProvider(provider) }) }
                if (!searching && !registry.showAll && matching.size > shown.size) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        OutlinedButton(onClick = viewModel::showAll, modifier = Modifier.fillMaxWidth()) {
                            Text(pluralStringResource(R.plurals.ai_show_all_providers, matching.size, matching.size))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TileGrid(minTileWidth: Dp, content: LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minTileWidth),
        contentPadding = PaddingValues(start = Spacing.l, end = Spacing.l, bottom = Spacing.xl),
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier.fillMaxSize(),
        content = content,
    )
}

/** A provider: its logo (from models.dev), the name, the API host its key would go to, and its model count. */
@Composable
private fun ProviderTile(provider: RegistryProvider, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            ProviderLogo(provider.id, Sizes.topBarIcon + Spacing.m)
            Column(Modifier.weight(1f).padding(start = Spacing.m)) {
                Text(provider.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    provider.endpoint.host,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (provider.models.isNotEmpty()) {
                    Text(
                        pluralStringResource(R.plurals.ai_model_count, provider.models.size, provider.models.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        placeholder = { Text(stringResource(R.string.action_search)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s),
    )
}

/** The key: where it will go (host), a large hidden field, the check button and the provider's docs. */
@Composable
private fun KeyEntry(provider: RegistryProvider, busy: Boolean, onSubmit: (String) -> Unit) {
    var key by remember(provider.id) { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProviderLogo(provider.id, Sizes.heroLogo)
            Column(Modifier.padding(start = Spacing.l)) {
                Text(provider.name, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(provider.endpoint.host, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(stringResource(R.string.ai_key_explanation, provider.name), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text(stringResource(R.string.ai_key_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onDone = { onSubmit(key) }),
            modifier = Modifier.fillMaxWidth().widthIn(max = KEY_FIELD_MAX_WIDTH),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.l)) {
            Button(onClick = { onSubmit(key) }, enabled = key.isNotBlank() && !busy) { Text(stringResource(R.string.ai_validate_key)) }
            provider.doc?.let { doc -> TextButton(onClick = { uriHandler.openUri(doc) }) { Text(stringResource(R.string.ai_provider_docs)) } }
        }
        Text(
            stringResource(R.string.ai_key_privacy, provider.name, provider.endpoint.host),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Models as a grid of cards, filtered by family chips and search; newest known text models first. */
@Composable
private fun ModelGrid(models: List<ModelOption>, busy: Boolean, onChoose: (ModelOption) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var family by rememberSaveable { mutableStateOf<String?>(null) }
    val families = remember(models) { models.groupingBy { it.family }.eachCount().entries.sortedByDescending { it.value }.map { it.key } }
    val shown = models.filter {
        (family == null || it.family == family) &&
            (it.id.contains(query, ignoreCase = true) || it.name?.contains(query, ignoreCase = true) == true)
    }
    TileGrid(minTileWidth = MODEL_TILE_WIDTH) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(pluralStringResource(R.plurals.ai_choose_model, models.size, models.size), style = MaterialTheme.typography.bodyLarge)
                SearchField(query, onQuery = { query = it })
                if (families.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        item { FilterChip(selected = family == null, onClick = { family = null }, label = { Text(stringResource(R.string.ai_all_families)) }) }
                        items(families) { name ->
                            FilterChip(selected = family == name, onClick = { family = if (family == name) null else name }, label = { Text(name) })
                        }
                    }
                }
            }
        }
        items(shown, key = { it.id }) { model -> ModelCard(model, enabled = !busy, onClick = { onChoose(model) }) }
    }
}

@Composable
private fun ModelCard(model: ModelOption, enabled: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    model.name ?: model.id,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isRecent(model.releaseDate)) Badge { Text(stringResource(R.string.ai_model_new)) }
            }
            if (model.name != null) {
                Text(
                    model.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val details = listOfNotNull(
                model.releaseDate,
                model.contextTokens?.let { stringResource(R.string.ai_model_context, it / TOKENS_PER_K) },
            )
            if (details.isNotEmpty()) {
                Text(details.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** Released in the last 90 days. */
private fun isRecent(releaseDate: String?): Boolean = try {
    releaseDate != null && LocalDate.parse(releaseDate.take(DATE_LENGTH)).isAfter(LocalDate.now().minusDays(RECENT_DAYS))
} catch (_: DateTimeParseException) {
    false
}

private val PROVIDER_TILE_WIDTH = 220.dp
private val MODEL_TILE_WIDTH = 240.dp
private val KEY_FIELD_MAX_WIDTH = 720.dp
private const val TOKENS_PER_K = 1000
private const val DATE_LENGTH = 10
private const val RECENT_DAYS = 90L
