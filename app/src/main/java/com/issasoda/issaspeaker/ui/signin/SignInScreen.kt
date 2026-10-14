package com.issasoda.issaspeaker.ui.signin

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.ui.account.AccountActions
import com.issasoda.issaspeaker.ui.components.DotLine
import com.issasoda.issaspeaker.ui.components.screenInsets
import com.issasoda.issaspeaker.ui.theme.BrandColors
import com.issasoda.issaspeaker.ui.theme.Links
import com.issasoda.issaspeaker.ui.theme.Sizes
import com.issasoda.issaspeaker.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * The first screen while signed out: what the app does (value proposition, three facts, a sample announcement typed
 * out), sound waves spreading from the speaker-shaped sign-in button at the bottom right, and a way to look around
 * without an account. Two columns in landscape.
 */
@Composable
fun SignInScreen(actions: AccountActions, onSkip: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        SoundWaves()
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(screenInsets).padding(Spacing.xl)) {
            if (maxWidth > maxHeight) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { Pitch() }
                    Column(Modifier.weight(1f)) { SignInActions(actions, onSkip) }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { Pitch() }
                    SignInActions(actions, onSkip)
                }
            }
        }
    }
}

/** Logo and name, the promise in one line, what it means in three, and how it sounds. */
@Composable
private fun ColumnScope.Pitch() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(Sizes.heroLogo))
        BasicText(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = MIN_TITLE_SP.sp, maxFontSize = MAX_TITLE_SP.sp),
            modifier = Modifier.padding(start = Spacing.l),
        )
    }
    Spacer(Modifier.size(Spacing.xxl))
    Text(
        stringResource(R.string.home_hero_title),
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        stringResource(R.string.signin_subtitle),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.s),
    )
    Column(Modifier.padding(vertical = Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        listOf(R.string.signin_value_speaker, R.string.signin_value_ai, R.string.signin_value_private)
            .forEachIndexed { index, text -> DotLine(stringResource(text), index) }
    }
    SampleAnnouncement()
}

/** "This is how it sounds": a sample announcement that types itself out. */
@Composable
private fun SampleAnnouncement() {
    val sample = stringResource(R.string.signin_sample)
    val still = animationsOff()
    var typed by remember(sample) { mutableStateOf(if (still) sample else "") }
    LaunchedEffect(sample) {
        if (typed.isEmpty()) {
            sample.forEach { char ->
                typed += char
                delay(TYPE_DELAY_MS)
            }
        }
    }
    Column(
        Modifier
            .widthIn(max = SAMPLE_MAX_WIDTH)
            .clip(RoundedCornerShape(Spacing.l))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(Spacing.l),
    ) {
        Text(stringResource(R.string.signin_sample_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        // The full sentence is read by screen readers at once; the typing is visual only.
        Text(typed, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/** The speaker-shaped sign-in button at the bottom right, its status line, and the secondary actions. */
@Composable
private fun ColumnScope.SignInActions(actions: AccountActions, onSkip: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Spacer(Modifier.weight(1f))
    actions.message?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.End))
    }
    Text(
        stringResource(R.string.signin_prompt),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.align(Alignment.End).padding(bottom = Spacing.m),
    )
    SpeakerButton(
        label = stringResource(R.string.signin_button),
        subtitle = stringResource(R.string.signin_button_sub),
        description = stringResource(R.string.account_sign_in),
        busy = actions.busy,
        onClick = actions.onSignIn,
        modifier = Modifier.align(Alignment.End),
    )
    Row(Modifier.fillMaxWidth().padding(top = Spacing.l), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onSkip) { Text(stringResource(R.string.signin_skip)) }
        TextButton(onClick = { uriHandler.openUri(Links.PRIVACY) }) { Text(stringResource(R.string.action_read_privacy)) }
    }
}

/** Rings in the logo's colors spreading from the bottom right, where the speaker button sits; still when animations are off. */
@Composable
private fun SoundWaves() {
    val progress = if (animationsOff()) {
        STATIC_PROGRESS
    } else {
        val value by rememberInfiniteTransition(label = "waves").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(WAVE_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "wave",
        )
        value
    }
    Canvas(Modifier.fillMaxSize()) {
        val origin = Offset(size.width * ORIGIN_X, size.height * ORIGIN_Y)
        val maxRadius = size.maxDimension
        BrandColors.lights.forEachIndexed { index, color ->
            val t = (progress + index.toFloat() / BrandColors.lights.size) % 1f
            drawCircle(
                color = color.copy(alpha = WAVE_ALPHA * (1f - t)),
                radius = maxRadius * t,
                center = origin,
                style = Stroke(width = WAVE_STROKE.toPx()),
            )
        }
    }
}

@Composable
private fun animationsOff(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

private val SAMPLE_MAX_WIDTH = 560.dp
private val WAVE_STROKE = 10.dp
private const val WAVE_ALPHA = 0.28f
private const val WAVE_MS = 6_000
private const val STATIC_PROGRESS = 0.3f
private const val ORIGIN_X = 0.85f
private const val ORIGIN_Y = 0.88f
private const val TYPE_DELAY_MS = 35L
private const val MIN_TITLE_SP = 18
private const val MAX_TITLE_SP = 30
