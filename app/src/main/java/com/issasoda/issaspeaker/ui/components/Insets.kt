package com.issasoda.issaspeaker.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable

/*
 * Every screen uses the whole width in landscape, camera cutout included (the theme sets
 * windowLayoutInDisplayCutoutMode = always): only the system bars keep their space. Material's defaults also avoid the
 * cutout, so every Scaffold and top bar of the app uses these instead. Same rule as the Movie Recommendation app.
 */

/** For Scaffold.contentWindowInsets and full-screen content. */
val screenInsets: WindowInsets
    @Composable get() = WindowInsets.systemBars

/** For TopAppBar.windowInsets. */
val topBarInsets: WindowInsets
    @Composable get() = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
