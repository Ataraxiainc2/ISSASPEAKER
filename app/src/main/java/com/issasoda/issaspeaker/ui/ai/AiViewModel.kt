package com.issasoda.issaspeaker.ui.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.issasoda.issaspeaker.IssaSpeakerApplication
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.ai.AiResult
import com.issasoda.issaspeaker.ai.ChatTurn
import com.issasoda.issaspeaker.ai.KeyLookup
import com.issasoda.issaspeaker.ai.SavedProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatState(
    val turns: List<ChatTurn> = emptyList(),
    val sending: Boolean = false,
    /** The provider's own explanation of the last failure. */
    val error: String? = null,
)

/**
 * The AI tab (catalog F052, F057, F058): the providers the owner added, and a question box that uses the default
 * one. The conversation lives in memory only. AI answers are only shown; nothing the model writes triggers an
 * action (F021).
 */
class AiViewModel(app: Application) : AndroidViewModel(app) {
    private val services = app as IssaSpeakerApplication
    private val client = services.aiClient
    private val keys = services.aiKeys

    val saved: StateFlow<List<SavedProvider>> = keys.providers

    private val _chat = MutableStateFlow(ChatState())
    val chat: StateFlow<ChatState> = _chat.asStateFlow()

    init {
        viewModelScope.launch { keys.load() }
    }

    fun makeDefault(saved: SavedProvider) {
        viewModelScope.launch { keys.setDefault(saved.providerId) }
    }

    fun remove(saved: SavedProvider) {
        viewModelScope.launch { keys.remove(saved.providerId) }
    }

    fun ask(question: String) {
        val text = question.trim()
        if (text.isEmpty() || _chat.value.sending) return
        val turns = _chat.value.turns + ChatTurn(fromUser = true, text = text)
        _chat.value = ChatState(turns = turns, sending = true)
        viewModelScope.launch {
            val reply = when (val lookup = keys.key()) {
                is KeyLookup.Ready -> client.chat(lookup.provider.endpoint, lookup.apiKey, lookup.provider.modelId, turns.takeLast(MAX_TURNS))
                is KeyLookup.Unavailable, KeyLookup.Missing ->
                    AiResult.Failure(getApplication<Application>().getString(R.string.ai_key_unavailable))
            }
            _chat.update {
                when (reply) {
                    is AiResult.Success -> ChatState(turns = turns + ChatTurn(fromUser = false, text = reply.value))
                    is AiResult.Failure -> ChatState(turns = turns, error = reply.message)
                }
            }
        }
    }

    fun newConversation() {
        _chat.value = ChatState()
    }

    private companion object {
        /** The conversation sent with each question: enough context, bounded cost. */
        const val MAX_TURNS = 20
    }
}
