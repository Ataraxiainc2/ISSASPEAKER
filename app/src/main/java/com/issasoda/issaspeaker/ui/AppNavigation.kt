package com.issasoda.issaspeaker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * The app's menu: a rail on the side when the window is wide (landscape, tablets), a bar at the bottom on a phone in
 * portrait. Unlike Material's navigation suite, it keeps space only for the system bars, never for the camera cutout,
 * so every screen uses the whole width in landscape (owner's rule; same idea as ui/components/Insets.kt).
 */
@Composable
fun AppNavigation(
    items: List<Destination>,
    isSelected: (Destination) -> Boolean,
    onSelect: (Destination) -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= RAIL_MIN_WIDTH) {
            val railInsets = WindowInsets.systemBars.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)
            Row(Modifier.fillMaxSize()) {
                NavigationRail(windowInsets = railInsets) {
                    items.forEach { item ->
                        NavigationRailItem(
                            selected = isSelected(item),
                            onClick = { onSelect(item) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
                Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.systemBars.only(WindowInsetsSides.Start))) { content() }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))) { content() }
                NavigationBar(windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)) {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = isSelected(item),
                            onClick = { onSelect(item) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            }
        }
    }
}

/** Material's breakpoint between compact (bottom bar) and medium (rail) windows. */
private val RAIL_MIN_WIDTH = 600.dp
