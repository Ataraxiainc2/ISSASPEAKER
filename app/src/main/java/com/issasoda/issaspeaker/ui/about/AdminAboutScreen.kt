package com.issasoda.issaspeaker.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.data.records.Feature
import com.issasoda.issaspeaker.data.records.FeatureStatus
import com.issasoda.issaspeaker.data.records.Finding
import com.issasoda.issaspeaker.data.records.ProjectRecords
import com.issasoda.issaspeaker.ui.AppState
import com.issasoda.issaspeaker.ui.RecordsState
import com.issasoda.issaspeaker.ui.components.ScreenScaffold
import com.issasoda.issaspeaker.ui.theme.FontAssignment
import com.issasoda.issaspeaker.ui.theme.FontRole
import com.issasoda.issaspeaker.ui.theme.Sizes
import com.issasoda.issaspeaker.ui.theme.Spacing
import com.issasoda.issaspeaker.ui.theme.fontFamilyOf

private enum class AdminTab(val label: Int) {
    IMPLEMENTED(R.string.admin_tab_implemented),
    PLANNED(R.string.admin_tab_planned),
    PENDING(R.string.admin_tab_pending),
    SECURITY(R.string.security),
    DIAGNOSTICS(R.string.admin_tab_diagnostics),
}

private const val SECURITY_MODULE = "SEC"

private val roleLabels = mapOf(
    FontRole.DISPLAY to R.string.font_role_display,
    FontRole.TITLE to R.string.font_role_title,
    FontRole.BODY to R.string.font_role_body,
    FontRole.LABEL to R.string.font_role_label,
)

/**
 * The admin About (catalog F008): what is implemented, what is to implement, what is pending, the security
 * status and diagnostics, read from the catalog and audit log shipped in the APK. Features fill the whole width
 * as a grid of cards: one column in portrait, several in landscape.
 */
@Composable
fun AdminAboutScreen(state: AppState) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val language = LocalConfiguration.current.locales[0].language
    ScreenScaffold(
        title = stringResource(R.string.admin_title),
        account = state.account,
        onLogoClick = state.goHome,
        onAccountClick = state.openAccount,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val records = state.records) {
                RecordsState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(Spacing.l))
                RecordsState.Failed -> Text(stringResource(R.string.records_failed), Modifier.padding(Spacing.l))
                is RecordsState.Ready -> {
                    Summary(records.records)
                    PrimaryScrollableTabRow(selectedTabIndex = tab) {
                        AdminTab.entries.forEachIndexed { index, item ->
                            Tab(selected = tab == index, onClick = { tab = index }, text = { Text(stringResource(item.label)) })
                        }
                    }
                    FeatureGrid {
                        when (AdminTab.entries[tab]) {
                            AdminTab.IMPLEMENTED -> featureGroups(records.records.features.filter { it.status == FeatureStatus.IMPLEMENTED }, language)
                            AdminTab.PLANNED -> featureGroups(records.records.features.filter { it.status == FeatureStatus.PLANNED }, language)
                            AdminTab.PENDING -> featureGroups(records.records.features.filter { it.status.isPending }, language)
                            AdminTab.SECURITY -> security(records.records, language)
                            AdminTab.DIAGNOSTICS -> diagnostics(state.fonts)
                        }
                        fullRow("notes") { Notes() }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureGrid(content: LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(Sizes.featureCardMinWidth),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        content = content,
    )
}

private fun LazyGridScope.fullRow(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun Summary(records: ProjectRecords) {
    val done = records.count(FeatureStatus.IMPLEMENTED)
    val total = records.features.size
    Column(Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(stringResource(R.string.features_ready, done, total), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(
                R.string.admin_count_line,
                done,
                records.count(FeatureStatus.PLANNED),
                records.count(FeatureStatus.VALIDATE) + records.count(FeatureStatus.BLOCKED),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearProgressIndicator(progress = { done.toFloat() / total }, modifier = Modifier.fillMaxWidth())
    }
}

/** Features grouped under their milestone header, in catalog order. */
private fun LazyGridScope.featureGroups(features: List<Feature>, language: String) {
    if (features.isEmpty()) {
        fullRow("empty") { Text(stringResource(R.string.admin_empty)) }
        return
    }
    features.groupBy { it.milestone }.toSortedMap().forEach { (milestone, group) ->
        fullRow("header-$milestone") { MilestoneHeader(milestone, group.size) }
        items(group, key = { it.id }) { FeatureCard(it, language) }
    }
}

private fun LazyGridScope.security(records: ProjectRecords, language: String) {
    val controls = records.features.filter { it.module == SECURITY_MODULE }
    fullRow("controls") {
        SectionHeader(
            stringResource(R.string.admin_security_controls) + " · " +
                stringResource(
                    R.string.admin_count_line,
                    controls.count { it.status == FeatureStatus.IMPLEMENTED },
                    controls.count { it.status == FeatureStatus.PLANNED },
                    controls.count { it.status.isPending },
                ),
        )
    }
    items(controls, key = { it.id }) { FeatureCard(it, language) }
    fullRow("findings") { SectionHeader(stringResource(R.string.admin_security_findings)) }
    if (records.findings.isEmpty()) {
        fullRow("no-findings") { Text(stringResource(R.string.admin_security_no_findings)) }
    } else {
        items(records.findings, key = { it.id }) { FindingCard(it) }
    }
}

/** Fonts drawn for this session, each name written in its own face, so a screenshot identifies them. */
private fun LazyGridScope.diagnostics(fonts: FontAssignment) {
    fullRow("fonts-header") { SectionHeader(stringResource(R.string.admin_diag_fonts)) }
    items(FontRole.entries, key = { it.name }) { role ->
        val family = fonts.familyFor(role)
        val fontFamily = remember(family) { fontFamilyOf(family) }
        CardBlock(overline = stringResource(roleLabels.getValue(role))) {
            Text(family, style = MaterialTheme.typography.titleLarge.copy(fontFamily = fontFamily))
        }
    }
}

@Composable
private fun MilestoneHeader(milestone: String, size: Int) {
    val names = stringArrayResource(R.array.milestone_names)
    val name = milestone.removePrefix("M").toIntOrNull()?.let { names.getOrNull(it) } ?: milestone
    SectionHeader("$milestone · $name ($size)")
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = Spacing.m).semantics { heading() },
    )
}

@Composable
private fun FeatureCard(feature: Feature, language: String) {
    val status = when (feature.status) {
        FeatureStatus.VALIDATE -> " · " + stringResource(R.string.admin_pending_validate)
        FeatureStatus.BLOCKED -> " · " + stringResource(R.string.admin_pending_blocked)
        else -> ""
    }
    val accent = when (feature.status) {
        FeatureStatus.IMPLEMENTED -> MaterialTheme.colorScheme.primary
        FeatureStatus.PLANNED -> MaterialTheme.colorScheme.onSurfaceVariant
        FeatureStatus.VALIDATE -> MaterialTheme.colorScheme.tertiary
        FeatureStatus.BLOCKED -> MaterialTheme.colorScheme.error
    }
    CardBlock(overline = "${feature.id} · ${feature.module} · ${feature.priority}$status", overlineColor = accent) {
        Text(feature.title(language), style = MaterialTheme.typography.bodyLarge)
        if (feature.note.isNotEmpty()) Text(feature.note, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FindingCard(finding: Finding) {
    CardBlock(overline = "${finding.id} · ${finding.date} · ${finding.severity} · ${finding.status}") {
        Text(finding.title, style = MaterialTheme.typography.bodyLarge)
        Text(finding.detail, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CardBlock(overline: String, overlineColor: Color = Color.Unspecified, content: @Composable () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(overline, style = MaterialTheme.typography.labelMedium, color = overlineColor)
            content()
        }
    }
}

@Composable
private fun Notes() {
    Column(Modifier.padding(top = Spacing.l)) {
        Text(stringResource(R.string.admin_gate_note), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.admin_titles_note), style = MaterialTheme.typography.bodySmall)
    }
}
