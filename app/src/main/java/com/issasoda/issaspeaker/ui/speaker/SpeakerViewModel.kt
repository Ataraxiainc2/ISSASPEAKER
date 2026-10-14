package com.issasoda.issaspeaker.ui.speaker

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.issasoda.issaspeaker.IssaSpeakerApplication
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.speaker.ConnectionProblem
import com.issasoda.issaspeaker.speaker.FoundSpeaker
import com.issasoda.issaspeaker.speaker.SessionResult
import com.issasoda.issaspeaker.speaker.Speaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The last connection test: running, or its result as a string resource. */
data class TestState(val running: Boolean = false, val message: Int? = null, val success: Boolean = false)

/**
 * The speaker tab (catalog F089, F090): find the Cast speakers on the Wi-Fi, choose one, test the connection.
 * Searching happens only while the screen is visible ([startSearch] / [stopSearch]).
 */
class SpeakerViewModel(app: Application) : AndroidViewModel(app) {
    private val services = app as IssaSpeakerApplication
    private val store = services.speakerStore
    private val discovery = services.speakerDiscovery
    private val connection = services.speakerConnection

    val chosen: StateFlow<Speaker?> = store.speaker
    val found: StateFlow<List<FoundSpeaker>> = discovery.found

    private val _test = MutableStateFlow(TestState())
    val test: StateFlow<TestState> = _test.asStateFlow()

    /**
     * True when the phone protects the local network (Android 17's ACCESS_LOCAL_NETWORK) and the owner has not
     * allowed it yet. Phones without that protection reach the speaker freely, so nothing is asked there.
     */
    fun needsLocalNetworkPermission(): Boolean {
        val app = getApplication<Application>()
        val known = try {
            app.packageManager.getPermissionInfo(LOCAL_NETWORK, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
        return known && ContextCompat.checkSelfPermission(app, LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED
    }

    fun startSearch() = discovery.start()

    fun stopSearch() = discovery.stop()

    fun choose(speaker: Speaker) {
        store.choose(speaker)
        _test.value = TestState()
    }

    fun forget() {
        store.forget()
        _test.value = TestState()
    }

    /** Connects to the chosen speaker; it plays its connection sound. */
    fun testConnection() {
        val speaker = chosen.value ?: return
        if (_test.value.running) return
        _test.value = TestState(running = true)
        viewModelScope.launch {
            _test.value = when (val result = connection.test(speaker)) {
                is SessionResult.Done -> TestState(message = R.string.speaker_test_ok, success = true)
                is SessionResult.Problem -> TestState(message = problemMessage(result.problem))
            }
        }
    }

    override fun onCleared() = discovery.stop()

    companion object {
        /** What to tell the owner when the speaker could not be reached. */
        fun problemMessage(problem: ConnectionProblem): Int = when (problem) {
            ConnectionProblem.NotFound -> R.string.speaker_test_not_found
            ConnectionProblem.TimedOut -> R.string.speaker_test_timeout
            is ConnectionProblem.Failed -> R.string.speaker_test_failed
        }

        /** Android 17's local network permission; absent on earlier phones. */
        const val LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
