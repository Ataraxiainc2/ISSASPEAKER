package com.issasoda.issaspeaker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Spacing scale (4 dp base). Every margin and gap in the app comes from here. */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Fixed sizes shared by several screens. */
object Sizes {
    /** Cards never get narrower than this; wider screens get more columns instead of wider cards. */
    val cardMinWidth = 380.dp
    val topBarIcon = 32.dp
    val heroLogo = 72.dp
    val profilePhoto = 72.dp
    val featureCardMinWidth = 340.dp
}

/** The four status-light colors of the logo, reused as brand accents (bullets, markers). */
object BrandColors {
    val coral = Color(0xFFFF6B5A)
    val amber = Color(0xFFFFC857)
    val teal = Color(0xFF2EC4B6)
    val violet = Color(0xFF8B7CF6)
    val lights = listOf(coral, amber, teal, violet)
}
