package com.issasoda.issaspeaker.ui.signin

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.issasoda.issaspeaker.ui.theme.BrandColors
import com.issasoda.issaspeaker.ui.theme.Spacing

/**
 * A smart speaker as a button: a fabric-covered capsule like a Nest speaker, with the logo's four status lights on top
 * that pulse one after another, as a speaker does while it listens. [busy] makes them run faster. The pulse stops
 * when the phone's "remove animations" setting is on.
 */
@Composable
fun SpeakerButton(label: String, subtitle: String, description: String, busy: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val body = MaterialTheme.colorScheme.inverseSurface
    val content = MaterialTheme.colorScheme.inverseOnSurface
    val shape = RoundedCornerShape(percent = 50)
    val phase = lightPhase(fast = busy)
    Box(
        modifier
            .heightIn(min = BUTTON_HEIGHT)
            .widthIn(min = BUTTON_MIN_WIDTH)
            .shadow(Spacing.s, shape)
            .clip(shape)
            .background(body)
            .clickable(enabled = !busy, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        // The speaker fabric: a fine grid of dots in the content color.
        Canvas(Modifier.matchParentSize()) {
            val step = FABRIC_STEP.toPx()
            val dot = FABRIC_DOT.toPx()
            var y = step / 2
            while (y < size.height) {
                var x = step / 2
                while (x < size.width) {
                    drawCircle(content.copy(alpha = FABRIC_ALPHA), radius = dot, center = Offset(x, y))
                    x += step
                }
                y += step
            }
        }
        Column(Modifier.padding(horizontal = Spacing.xxl, vertical = Spacing.m), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                BrandColors.lights.forEachIndexed { index, color ->
                    val glow = lightLevel(phase, index)
                    Box(Modifier.size(LIGHT_SIZE).alpha(glow).clip(CircleShape).background(color))
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = content,
                modifier = Modifier.padding(top = Spacing.s),
            )
            Text(subtitle, style = MaterialTheme.typography.labelLarge, color = content.copy(alpha = SUBTITLE_ALPHA))
        }
    }
}

/** 0..4 around the four lights, looping; fixed when animations are off. */
@Composable
private fun lightPhase(fast: Boolean): Float {
    if (animationsOff()) return STATIC_PHASE
    val phase by rememberInfiniteTransition(label = "speaker lights").animateFloat(
        initialValue = 0f,
        targetValue = BrandColors.lights.size.toFloat(),
        animationSpec = infiniteRepeatable(tween(if (fast) FAST_CYCLE_MS else CYCLE_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    return phase
}

/** A light is brightest when the phase passes it and dims as it moves on. */
private fun lightLevel(phase: Float, index: Int): Float {
    val count = BrandColors.lights.size
    val distance = ((phase - index) % count + count) % count
    return (1f - distance / count).coerceIn(MIN_GLOW, 1f)
}

@Composable
private fun animationsOff(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

private val BUTTON_HEIGHT = 96.dp
private val BUTTON_MIN_WIDTH = 200.dp
private val LIGHT_SIZE = 10.dp
private val FABRIC_STEP = 7.dp
private val FABRIC_DOT = 1.dp
private const val FABRIC_ALPHA = 0.14f
private const val SUBTITLE_ALPHA = 0.8f
private const val MIN_GLOW = 0.35f
private const val STATIC_PHASE = 0.5f
private const val CYCLE_MS = 2_400
private const val FAST_CYCLE_MS = 700
