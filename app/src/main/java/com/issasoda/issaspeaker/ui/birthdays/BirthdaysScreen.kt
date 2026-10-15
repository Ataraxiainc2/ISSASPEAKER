package com.issasoda.issaspeaker.ui.birthdays

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.data.birthdays.Birthday
import com.issasoda.issaspeaker.data.birthdays.upcoming
import com.issasoda.issaspeaker.ui.AppState
import com.issasoda.issaspeaker.ui.components.AdaptiveGrid
import com.issasoda.issaspeaker.ui.components.ScreenScaffold
import com.issasoda.issaspeaker.ui.components.SectionCard
import com.issasoda.issaspeaker.ui.components.fullWidth
import com.issasoda.issaspeaker.ui.speaker.TestState
import com.issasoda.issaspeaker.ui.theme.BrandColors
import com.issasoda.issaspeaker.ui.theme.Sizes
import com.issasoda.issaspeaker.ui.theme.Spacing
import java.time.LocalDate

/**
 * Birthdays: who is next (and an "announce now" for the speaker), everyone as cards from the soonest, a quick form
 * and imports from contacts or a file. Everything is on this phone; the owner's Google Sheet copy comes next.
 */
@Composable
fun BirthdaysScreen(app: AppState, viewModel: BirthdaysViewModel) {
    val all by viewModel.birthdays.collectAsStateWithLifecycle()
    val import by viewModel.import.collectAsStateWithLifecycle()
    val announce by viewModel.announce.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val sorted = all.upcoming(today)
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val contacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.importContacts()
    }
    val file = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::importFile) }
    val imports = ImportActions(
        onContacts = { contacts.launch(Manifest.permission.READ_CONTACTS) },
        onFile = { file.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/plain")) },
    )

    ScreenScaffold(
        title = stringResource(R.string.nav_birthdays),
        account = app.account,
        onLogoClick = app.goHome,
        onAccountClick = app.openAccount,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = NEW },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.birthday_add)) },
            )
        },
    ) { padding ->
        AdaptiveGrid(padding, minColumnWidth = Sizes.featureCardMinWidth) {
            fullWidth("next") { NextCard(sorted, today, announce, viewModel::announceToday) }
            if (sorted.isEmpty()) {
                fullWidth("empty") { EmptyCard(imports) }
            }
            sorted.forEachIndexed { index, birthday ->
                item(key = birthday.id) { BirthdayCard(birthday, today, index, onClick = { editing = birthday.id }) }
            }
            if (sorted.isNotEmpty()) {
                fullWidth("import") { ImportRow(imports) }
            }
        }
    }
    editing?.let { id ->
        BirthdayForm(
            existing = all.firstOrNull { it.id == id },
            onSave = { viewModel.save(it); editing = null },
            onDelete = { viewModel.delete(it); editing = null },
            onDismiss = { editing = null },
        )
    }
    import?.let { ImportSheet(it, viewModel) }
}

/** The two ways to bring many birthdays at once. */
private class ImportActions(val onContacts: () -> Unit, val onFile: () -> Unit)

@Composable
private fun NextCard(sorted: List<Birthday>, today: LocalDate, announce: TestState, onAnnounce: () -> Unit) {
    val todays = sorted.filter { it.isToday(today) }
    val locale = LocalConfiguration.current.locales[0]
    SectionCard(stringResource(if (todays.isNotEmpty()) R.string.birthday_today_title else R.string.birthday_next_title)) {
        when {
            todays.isNotEmpty() -> todays.forEach { b ->
                Text(b.name, style = MaterialTheme.typography.headlineMedium)
                b.turning(today)?.let { Text(stringResource(R.string.birthday_turns, it), style = MaterialTheme.typography.titleMedium) }
            }
            sorted.isNotEmpty() -> {
                val next = sorted.first()
                Text(next.name, style = MaterialTheme.typography.headlineMedium)
                val days = next.daysUntil(today).toInt()
                Text(
                    "${BirthdayAnnouncement.dayMonth(next.next(today), locale)} · ${pluralStringResource(R.plurals.birthday_in_days, days, days)}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            else -> Text(stringResource(R.string.birthday_none_yet), style = MaterialTheme.typography.bodyLarge)
        }
        if (announce.running) LinearProgressIndicator(Modifier.fillMaxWidth())
        announce.message?.let {
            Text(
                stringResource(it),
                style = MaterialTheme.typography.bodyMedium,
                color = if (announce.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        if (sorted.isNotEmpty()) {
            Button(onClick = onAnnounce, enabled = !announce.running) { Text(stringResource(R.string.birthday_announce)) }
        }
    }
}

@Composable
private fun EmptyCard(imports: ImportActions) {
    SectionCard(stringResource(R.string.birthday_empty_title)) {
        Text(stringResource(R.string.birthday_empty_body), style = MaterialTheme.typography.bodyLarge)
        ImportButtons(imports)
    }
}

@Composable
private fun ImportRow(imports: ImportActions) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(stringResource(R.string.birthday_import_more), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ImportButtons(imports)
    }
}

@Composable
private fun ImportButtons(imports: ImportActions) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        OutlinedButton(onClick = imports.onContacts) { Text(stringResource(R.string.birthday_from_contacts)) }
        OutlinedButton(onClick = imports.onFile) { Text(stringResource(R.string.birthday_from_file)) }
    }
}

/** One person: an initial in a brand color, name, date, the age they turn and how soon. */
@Composable
private fun BirthdayCard(birthday: Birthday, today: LocalDate, index: Int, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val isToday = birthday.isToday(today)
    val colors = if (isToday) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
    } else {
        CardDefaults.cardColors()
    }
    Card(colors = colors, modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(Sizes.topBarIcon + Spacing.m).background(BrandColors.lights[index % BrandColors.lights.size], CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(birthday.name.trim().take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.surface)
            }
            Column(Modifier.weight(1f).padding(start = Spacing.m)) {
                Text(birthday.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val days = birthday.daysUntil(today).toInt()
                val whenText = if (isToday) stringResource(R.string.birthday_today) else pluralStringResource(R.plurals.birthday_in_days, days, days)
                val ageText = birthday.turning(today)?.let { stringResource(R.string.birthday_turns, it) }
                Text(
                    listOfNotNull(BirthdayAnnouncement.dayMonth(birthday.next(today), locale), ageText, whenText).joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                )
                if (birthday.note.isNotBlank()) {
                    Text(birthday.note, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** The form's id for "a new birthday". */
internal const val NEW = "new"
