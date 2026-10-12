package com.issasoda.issaspeaker.ui.theme

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

/**
 * Material 3 Expressive with the wallpaper's dynamic colors (always available: minSdk 36), light or dark as
 * the user chose, and this session's typefaces: one family per text role ([FontAssignment]).
 */
@Composable
fun IssaSpeakerTheme(fonts: FontAssignment, darkTheme: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    val typography = remember(fonts) { typographyWith(fonts) }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = typography,
        content = content,
    )
}

private fun typographyWith(fonts: FontAssignment): Typography {
    val families = FontRole.entries.associateWith { FontPool.fontFamily(fonts.familyFor(it)) }
    fun TextStyle.inRole(role: FontRole) = copy(fontFamily = families.getValue(role))
    return with(Typography()) {
        copy(
            displayLarge = displayLarge.inRole(FontRole.DISPLAY),
            displayMedium = displayMedium.inRole(FontRole.DISPLAY),
            displaySmall = displaySmall.inRole(FontRole.DISPLAY),
            headlineLarge = headlineLarge.inRole(FontRole.DISPLAY),
            headlineMedium = headlineMedium.inRole(FontRole.DISPLAY),
            headlineSmall = headlineSmall.inRole(FontRole.DISPLAY),
            titleLarge = titleLarge.inRole(FontRole.TITLE),
            titleMedium = titleMedium.inRole(FontRole.TITLE),
            titleSmall = titleSmall.inRole(FontRole.TITLE),
            bodyLarge = bodyLarge.inRole(FontRole.BODY),
            bodyMedium = bodyMedium.inRole(FontRole.BODY),
            bodySmall = bodySmall.inRole(FontRole.BODY),
            labelLarge = labelLarge.inRole(FontRole.LABEL),
            labelMedium = labelMedium.inRole(FontRole.LABEL),
            labelSmall = labelSmall.inRole(FontRole.LABEL),
        )
    }
}

/** For screens that show a family name written in that family (Settings > Fonts in use). */
fun fontFamilyOf(name: String): FontFamily = FontPool.fontFamily(name)
