package com.issasoda.issaspeaker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.data.account.Account
import com.issasoda.issaspeaker.ui.theme.BrandColors
import com.issasoda.issaspeaker.ui.theme.Sizes
import com.issasoda.issaspeaker.ui.theme.Spacing

/**
 * One frame for every screen: a top bar with the logo (back to Home), the screen title and the account
 * photo (opens the account sheet). The bar collapses while scrolling and comes back on the way up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    account: Account?,
    onLogoClick: () -> Unit,
    onAccountClick: () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = screenInsets,
        topBar = {
            TopAppBar(
                windowInsets = topBarInsets,
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onLogoClick) {
                        Image(
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = stringResource(R.string.action_home),
                            modifier = Modifier.size(Sizes.topBarIcon),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAccountClick) { AccountPhoto(account, Sizes.topBarIcon) }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

/** The Google profile photo, or a neutral account icon when signed out or while it loads. */
@Composable
fun AccountPhoto(account: Account?, size: Dp) {
    val description = account?.let { stringResource(R.string.account_signed_in_as, it.email) }
        ?: stringResource(R.string.account_signed_out)
    if (account?.photoUrl == null) {
        Icon(Icons.Filled.AccountCircle, contentDescription = description, modifier = Modifier.size(size))
    } else {
        AsyncImage(
            model = account.photoUrl,
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape),
        )
    }
}

/**
 * Cards laid out over the full width of the screen: as many columns as fit [minColumnWidth], so a phone in
 * portrait shows one or two columns and the same phone in landscape more, with no wasted side margins.
 * [maxColumns] caps the count at the number of cards a screen has, so a few cards still fill the width.
 */
@Composable
fun AdaptiveGrid(
    padding: PaddingValues,
    minColumnWidth: Dp = Sizes.cardMinWidth,
    maxColumns: Int = Int.MAX_VALUE,
    content: LazyStaggeredGridScope.() -> Unit,
) {
    val direction = LocalLayoutDirection.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val start = padding.calculateStartPadding(direction) + Spacing.l
        val end = padding.calculateEndPadding(direction) + Spacing.l
        val available = maxWidth - start - end
        val columns = ((available + Spacing.l) / (minColumnWidth + Spacing.l)).toInt().coerceIn(1, maxOf(1, maxColumns))
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = start,
                top = padding.calculateTopPadding() + Spacing.s,
                end = end,
                bottom = padding.calculateBottomPadding() + Spacing.l,
            ),
            verticalItemSpacing = Spacing.l,
            horizontalArrangement = Arrangement.spacedBy(Spacing.l),
            content = content,
        )
    }
}

/** A card that spans every column (hero, summaries). */
fun LazyStaggeredGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = StaggeredGridItemSpan.FullLine) { content() }
}

/** A titled card with free content: the building block of every screen. The title is a heading for screen readers. */
@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            content()
        }
    }
}

/** A list line with a brand-colored dot, for short scannable lists. */
@Composable
fun DotLine(text: String, index: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .padding(end = Spacing.m)
                .size(Spacing.s)
                .clip(CircleShape)
                .background(BrandColors.lights[index % BrandColors.lights.size]),
        )
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
