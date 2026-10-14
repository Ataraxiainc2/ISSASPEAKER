package com.issasoda.issaspeaker.ui.ai

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.issasoda.issaspeaker.IssaSpeakerApplication
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.ai.AiResult
import com.issasoda.issaspeaker.ai.ChatTurn
import com.issasoda.issaspeaker.ai.KeyLookup
import com.issasoda.issaspeaker.ai.RegistryProvider
import com.issasoda.issaspeaker.ai.SavedProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.security.GeneralSecurityException

/** A model the key can use; everything but [id] comes from the registry when it knows the model. */
data class ModelOption(
    val id: String,
    val name: String?,
    val releaseDate: String?,
    /** The model line used to group the list: the registry's family, else the id's vendor or first word. */
    val family: String,
    val contextTokens: Long?,
)

/** The steps of adding a provider, or of changing the model of one already saved ([ChooseModel.existing]). */
sealed interface AddStep {
    data object ChooseProvider : AddStep
    data class EnterKey(val provider: RegistryProvider) : AddStep
    data class ChooseModel(val provider: RegistryProvider, val models: List<ModelOption>, val existing: Boolean) : AddStep
}

data class RegistryState(
    val loading: Boolean = true,
    val providers: List<RegistryProvider> = emptyList(),
    /** The registry could not be loaded (offline with no copy yet). */
    val failed: Boolean = false,
    val query: String = "",
    /** False: only the [AddProviderViewModel.TOP_PROVIDERS] most widely served labs are listed until "Show all". */
    val showAll: Boolean = false,
)

data class AddFlowState(
    val registry: RegistryState = RegistryState(),
    /** Null: the flow is closed and the AI tab shows. */
    val step: AddStep? = null,
    val busy: Boolean = false,
    /** The provider's own explanation of the last failure (wrong key, no credit, no such model...). */
    val providerError: String? = null,
)

/**
 * Adding a provider (catalog F051, F052, F416, F419): live registry → key, checked by listing the provider's models
 * → model, checked with one short test call. Also changing the model of a saved provider. Same flow as the Movie
 * Recommendation app. A key is in memory only while it is being checked; [com.issasoda.issaspeaker.ai.AiKeyStore]
 * encrypts it before it is written.
 */
class AddProviderViewModel(app: Application) : AndroidViewModel(app) {
    private val services = app as IssaSpeakerApplication
    private val registry = services.providerRegistry
    private val client = services.aiClient
    private val keys = services.aiKeys

    private val _state = MutableStateFlow(AddFlowState())
    val state: StateFlow<AddFlowState> = _state.asStateFlow()

    private val _messages = Channel<Int>(Channel.BUFFERED)

    /** One-off messages (string resources) for a snackbar. */
    val messages: Flow<Int> = _messages.receiveAsFlow()

    private var pendingKey: String = ""

    fun start() {
        pendingKey = ""
        _state.update { it.copy(step = AddStep.ChooseProvider, providerError = null) }
        if (_state.value.registry.providers.isEmpty()) loadProviders()
    }

    fun loadProviders() {
        _state.update { it.copy(registry = it.registry.copy(loading = true, failed = false)) }
        viewModelScope.launch {
            val result = registry.providers()
            // The labs whose models the most providers serve come first, then by model count and name.
            val order = compareByDescending<RegistryProvider> { it.reach }.thenByDescending { it.models.size }.thenBy { it.name.lowercase() }
            val providers = result.getOrDefault(emptyList()).sortedWith(order)
            _state.update { it.copy(registry = it.registry.copy(loading = false, providers = providers, failed = result.isFailure)) }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(registry = it.registry.copy(query = query)) }

    fun showAll() = _state.update { it.copy(registry = it.registry.copy(showAll = true)) }

    fun chooseProvider(provider: RegistryProvider) {
        pendingKey = ""
        _state.update { it.copy(step = AddStep.EnterKey(provider), providerError = null) }
    }

    /** One step back; closes the flow from its first step (or from a model change). */
    fun back() {
        pendingKey = ""
        _state.update {
            it.copy(
                providerError = null,
                busy = false,
                step = when (val step = it.step) {
                    null, AddStep.ChooseProvider -> null
                    is AddStep.EnterKey -> AddStep.ChooseProvider
                    is AddStep.ChooseModel -> if (step.existing) null else AddStep.EnterKey(step.provider)
                },
            )
        }
    }

    /** Checks [key] by asking the provider which models it can use. */
    fun submitKey(key: String) {
        val step = _state.value.step as? AddStep.EnterKey ?: return
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return
        pendingKey = trimmed
        _state.update { it.copy(busy = true, providerError = null) }
        viewModelScope.launch { showModels(step.provider, trimmed, existing = false) }
    }

    /** Opens the model list of a saved provider, read live with its stored key and endpoint. */
    fun changeModel(saved: SavedProvider) {
        _state.update { it.copy(busy = true, providerError = null) }
        viewModelScope.launch {
            when (val lookup = keys.key(saved.providerId)) {
                is KeyLookup.Ready -> {
                    pendingKey = lookup.apiKey
                    // The saved endpoint, never the registry's current one; the registry only adds model details.
                    val known = registry.cached(saved.providerId)?.models.orEmpty()
                    showModels(RegistryProvider(saved.providerId, saved.name, endpoint = saved.endpoint, models = known), lookup.apiKey, existing = true)
                }
                is KeyLookup.Unavailable, KeyLookup.Missing -> {
                    _state.update { it.copy(busy = false) }
                    _messages.send(R.string.ai_key_unavailable)
                }
            }
        }
    }

    /** One short test call with [model]; saved only when the provider answers. */
    fun chooseModel(model: ModelOption) {
        val step = _state.value.step as? AddStep.ChooseModel ?: return
        _state.update { it.copy(busy = true, providerError = null) }
        viewModelScope.launch {
            val test = client.chat(step.provider.endpoint, pendingKey, model.id, listOf(ChatTurn(fromUser = true, text = TEST_PROMPT)))
            when (test) {
                is AiResult.Success -> save(step, model)
                is AiResult.Failure -> _state.update { it.copy(busy = false, providerError = test.message) }
            }
        }
    }

    private suspend fun save(step: AddStep.ChooseModel, model: ModelOption) {
        val failure = try {
            if (step.existing) {
                keys.setModel(step.provider.id, model.id)
            } else {
                keys.save(step.provider.id, step.provider.name, step.provider.endpoint, model.id, pendingKey)
            }
            null
        } catch (e: GeneralSecurityException) {
            e
        } catch (e: IOException) {
            e
        } catch (e: IllegalStateException) {
            e
        }
        _state.update { it.copy(busy = false, step = if (failure == null) null else it.step) }
        if (failure == null) {
            pendingKey = ""
            _messages.send(R.string.ai_saved)
        } else {
            Log.e(TAG, "Could not save the AI key", failure)
            _messages.send(R.string.ai_encrypt_failed)
        }
    }

    private suspend fun showModels(provider: RegistryProvider, apiKey: String, existing: Boolean) {
        when (val result = client.listModels(provider.endpoint, apiKey)) {
            is AiResult.Success -> {
                val models = modelOptions(provider, result.value)
                _state.update {
                    if (models.isEmpty()) {
                        it.copy(busy = false, providerError = getApplication<Application>().getString(R.string.ai_no_text_models))
                    } else {
                        it.copy(busy = false, step = AddStep.ChooseModel(provider, models, existing))
                    }
                }
            }
            is AiResult.Failure -> _state.update { it.copy(busy = false, providerError = result.message) }
        }
    }

    override fun onCleared() {
        pendingKey = ""
    }

    internal companion object {
        private const val TAG = "AddProviderViewModel"
        private const val TEST_PROMPT = "Reply with the single word OK."
        const val TOP_PROVIDERS = 12

        /**
         * The provider's list, minus models the registry knows produce no text (image, speech, embeddings); known text
         * models first, newest first, then the ones the registry does not know yet, by id.
         */
        fun modelOptions(provider: RegistryProvider, ids: List<String>): List<ModelOption> {
            val known = ids.mapNotNull { id -> provider.models[id]?.let { id to it } }
            val text = known.filter { it.second.textOutput }
                .sortedByDescending { it.second.releaseDate.orEmpty() }
                .map { (id, model) -> ModelOption(id, model.name, model.releaseDate, model.family ?: familyOf(id), model.contextTokens) }
            val unknown = ids.filter { it !in provider.models }.sorted().map { ModelOption(it, null, null, familyOf(it), null) }
            return text + unknown
        }

        /** "openai/gpt-4o" → "openai"; "claude-haiku-5-5" → "claude"; "o3" → "o3". */
        fun familyOf(id: String): String = id.substringBefore('/').takeIf { '/' in id }
            ?: id.substringBefore('-').substringBefore('.').lowercase()
    }
}
