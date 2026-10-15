package com.issasoda.issaspeaker.ui.birthdays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.data.birthdays.BirthdaySource
import com.issasoda.issaspeaker.ui.components.screenInsets
import com.issasoda.issaspeaker.ui.theme.Spacing
import java.time.LocalDate

/**
 * Who an import found, to tick before anything is added. People already on the list start unticked. For a file, how
 * its dates were read (day/month) is shown with a one-tap swap, so 3/4 never silently becomes the wrong month.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSheet(state: ImportState, viewModel: BirthdaysViewModel) {
    val locale = LocalConfiguration.current.locales[0]
    ModalBottomSheet(
        onDismissRequest = viewModel::cancelImport,
        sheetState = fullSheet(),
        sheetMaxWidth = Dp.Unspecified,
        contentWindowInsets = { screenInsets },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.xl).padding(bottom = Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(
                stringResource(if (state.source == BirthdaySource.CONTACTS) R.string.birthday_from_contacts else R.string.birthday_from_file),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (state.candidates.isEmpty()) {
                val none = if (state.source == BirthdaySource.CONTACTS) R.string.birthday_import_none_contacts else R.string.birthday_import_none_file
                Text(stringResource(state.problem ?: none), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.birthday_format_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = viewModel::cancelImport) { Text(stringResource(R.string.action_cancel)) }
                return@Column
            }
            // The decision first, so it is never below the fold (landscape, long lists).
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Button(onClick = viewModel::confirmImport, enabled = state.selected.isNotEmpty()) {
                    Text(pluralStringResource(R.plurals.birthday_import_confirm, state.selected.size, state.selected.size))
                }
                TextButton(onClick = viewModel::cancelImport) { Text(stringResource(R.string.action_cancel)) }
            }
            if (state.source == BirthdaySource.FILE) FileReadInfo(state, onSwap = viewModel::swapDateOrder)
            LazyColumn(Modifier.heightIn(max = LIST_MAX_HEIGHT)) {
                itemsIndexed(state.candidates) { index, person ->
                    val checked = index in state.selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(checked, role = Role.Checkbox, onValueChange = { viewModel.toggle(index) })
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Column(Modifier.padding(start = Spacing.s)) {
                            Text(person.name, style = MaterialTheme.typography.bodyLarge)
                            val date = BirthdayAnnouncement.dayMonth(LocalDate.of(LEAP_YEAR, person.month, person.day), locale)
                            Text(listOfNotNull(date, person.year?.toString()).joinToString(" "), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

/** How the file's dates were read, with a one-tap swap, and how many rows were skipped. */
@Composable
private fun FileReadInfo(state: ImportState, onSwap: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(if (state.dayFirst) R.string.birthday_read_day_first else R.string.birthday_read_month_first),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSwap) { Text(stringResource(R.string.birthday_swap)) }
    }
    if (state.skippedRows > 0) {
        Text(
            pluralStringResource(R.plurals.birthday_skipped, state.skippedRows, state.skippedRows),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val LIST_MAX_HEIGHT = 420.dp

/** Any leap year, so 29 February can be shown. */
private const val LEAP_YEAR = 2000
