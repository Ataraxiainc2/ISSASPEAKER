package com.issasoda.issaspeaker.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.data.records.Feature
import com.issasoda.issaspeaker.data.records.FeatureStatus
import com.issasoda.issaspeaker.data.records.Finding
import com.issasoda.issaspeaker.data.records.ProjectRecords
import com.issasoda.issaspeaker.ui.AppState
import com.issasoda.issaspeaker.ui.components.ScreenScaffold

private enum class AdminTab(val label: Int) {
    IMPLEMENTED(R.string.admin_tab_implemented),
    PLANNED(R.string.admin_tab_planned),
    PENDING(R.string.admin_tab_pending),
    SECURITY(R.string.security),
}

private const val SECURITY_MODULE = "SEC"

/**
 * The admin About (catalog F008): what is implemented, what is to implement, what is pending and the
 * security status, read from the feature catalog and the audit log shipped in the APK.
 */
@Composable
fun AdminAboutScreen(state: AppState, onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val language = LocalConfiguration.current.locales[0].language
    ScreenScaffold(title = stringResource(R.string.admin_title), onBack = onBack) { padding ->
        val records = state.records
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (records == null) {
                Text(stringResource(R.string.loading), Modifier.padding(16.dp))
                return@Column
            }
            Summary(records)
            PrimaryScrollableTabRow(selectedTabIndex = tab) {
                AdminTab.entries.forEachIndexed { index, item ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(stringResource(item.label)) })
                }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                when (AdminTab.entries[tab]) {
                    AdminTab.IMPLEMENTED -> featureGroups(records.features.filter { it.status == FeatureStatus.IMPLEMENTED }, language)
                    AdminTab.PLANNED -> featureGroups(records.features.filter { it.status == FeatureStatus.PLANNED }, language)
                    AdminTab.PENDING -> featureGroups(records.features.filter { it.status.isPending }, language)
                    AdminTab.SECURITY -> security(records, language)
                }
                item { Notes() }
            }
        }
    }
}

@Composable
private fun Summary(records: ProjectRecords) {
    val done = records.count(FeatureStatus.IMPLEMENTED)
    val total = records.features.size
    Column(Modifier.padding(16.dp)) {
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
        LinearProgressIndicator(progress = { done.toFloat() / total }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

/** Features grouped under their milestone header, in catalog order. */
private fun LazyListScope.featureGroups(features: List<Feature>, language: String) {
    if (features.isEmpty()) {
        item { Text(stringResource(R.string.admin_empty), Modifier.padding(16.dp)) }
        return
    }
    features.groupBy { it.milestone }.toSortedMap().forEach { (milestone, group) ->
        item(key = "header-$milestone") { MilestoneHeader(milestone, group.size) }
        items(group, key = { it.id }) { FeatureRow(it, language) }
    }
}

private fun LazyListScope.security(records: ProjectRecords, language: String) {
    val controls = records.features.filter { it.module == SECURITY_MODULE }
    item {
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
    items(controls, key = { it.id }) { FeatureRow(it, language) }
    item { SectionHeader(stringResource(R.string.admin_security_findings)) }
    if (records.findings.isEmpty()) {
        item { Text(stringResource(R.string.admin_security_no_findings), Modifier.padding(16.dp)) }
    } else {
        items(records.findings, key = { it.id }) { FindingRow(it) }
    }
}

@Composable
private fun MilestoneHeader(milestone: String, size: Int) {
    val names = stringArrayResource(R.array.milestone_names)
    val index = milestone.removePrefix("M").toIntOrNull()
    val name = index?.let { names.getOrNull(it) } ?: milestone
    SectionHeader("$milestone · $name ($size)")
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun FeatureRow(feature: Feature, language: String) {
    val status = when (feature.status) {
        FeatureStatus.VALIDATE -> " · " + stringResource(R.string.admin_pending_validate)
        FeatureStatus.BLOCKED -> " · " + stringResource(R.string.admin_pending_blocked)
        else -> ""
    }
    ListItem(
        overlineContent = { Text("${feature.id} · ${feature.module} · ${feature.priority}$status") },
        supportingContent = if (feature.note.isNotEmpty()) ({ Text(feature.note) }) else null,
    ) { Text(feature.title(language)) }
    HorizontalDivider()
}

@Composable
private fun FindingRow(finding: Finding) {
    ListItem(
        overlineContent = { Text("${finding.id} · ${finding.date} · ${finding.severity} · ${finding.status}") },
        supportingContent = { Text(finding.detail) },
    ) { Text(finding.title) }
    HorizontalDivider()
}

@Composable
private fun Notes() {
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(R.string.admin_gate_note), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.admin_titles_note), style = MaterialTheme.typography.bodySmall)
    }
}
