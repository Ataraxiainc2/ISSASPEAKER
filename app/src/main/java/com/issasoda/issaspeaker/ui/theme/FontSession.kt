package com.issasoda.issaspeaker.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * When the fonts change (decision C20): every time the app is opened or comes back after the user switched to
 * another app, never because the phone rotated or the language changed. MainActivity reports real stops
 * (not configuration changes); the next start draws a new [FontAssignment].
 */
object FontSession {

    private val state = MutableStateFlow(FontPool.draw())
    private var stoppedForReal = false

    val fonts: StateFlow<FontAssignment> = state.asStateFlow()

    fun onStop(isChangingConfigurations: Boolean) {
        if (!isChangingConfigurations) stoppedForReal = true
    }

    fun onStart() {
        if (stoppedForReal) {
            stoppedForReal = false
            state.value = FontPool.draw()
        }
    }
}
