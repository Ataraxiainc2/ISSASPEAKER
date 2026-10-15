package com.issasoda.issaspeaker.ui.birthdays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.data.birthdays.Birthday
import com.issasoda.issaspeaker.data.birthdays.BirthdaySource
import com.issasoda.issaspeaker.ui.theme.Spacing
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.UUID

/** What the form holds while it is open, and its checks. Errors show only after the first "Save". */
private class FormState(existing: Birthday?) {
    var name by mutableStateOf(existing?.name.orEmpty())
    var day by mutableStateOf(existing?.day?.toString().orEmpty())
    var month by mutableIntStateOf(existing?.month ?: LocalDate.now().monthValue)
    var year by mutableStateOf(existing?.year?.toString().orEmpty())
    var note by mutableStateOf(existing?.note.orEmpty())
    var tried by mutableStateOf(false)

    val nameError get() = tried && name.isBlank()
    val dayError get() = tried && day.toIntOrNull()?.let { Birthday.isValid(month, it) } != true
    val yearError get() = year.isNotBlank() && year.toIntOrNull()?.let { it in MIN_YEAR..LocalDate.now().year } != true

    companion object {
        fun saver(existing: Birthday?) = listSaver<FormState, Any>(
            save = { listOf(it.name, it.day, it.month, it.year, it.note, it.tried) },
            restore = { saved ->
                FormState(existing).apply {
                    name = saved[0] as String
                    day = saved[1] as String
                    month = saved[2] as Int
                    year = saved[3] as String
                    note = saved[4] as String
                    tried = saved[5] as Boolean
                }
            },
        )
    }

    /** The birthday to save, or null (and errors shown) when something is missing or wrong. */
    fun build(existing: Birthday?): Birthday? {
        tried = true
        val dayNumber = day.toIntOrNull()?.takeIf { Birthday.isValid(month, it) }
        if (name.isBlank() || dayNumber == null || yearError) return null
        return Birthday(
            id = existing?.id ?: UUID.randomUUID().toString(),
            name = name.trim(),
            month = month,
            day = dayNumber,
            year = year.toIntOrNull(),
            note = note.trim(),
            source = existing?.source ?: BirthdaySource.MANUAL,
        )
    }
}

/**
 * Add or change one birthday: name, day and month (month names in the app's language), the year only if known, and a
 * note. Errors show under their field; nothing typed is lost. Deleting asks first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayForm(existing: Birthday?, onSave: (Birthday) -> Unit, onDelete: (String) -> Unit, onDismiss: () -> Unit) {
    // Typed text survives rotation: the fields are saved as a list and put back into a new FormState.
    val form = rememberSaveable(existing?.id, saver = FormState.saver(existing)) { FormState(existing) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = fullSheet()) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(stringResource(if (existing == null) R.string.birthday_add else R.string.birthday_edit), style = MaterialTheme.typography.headlineSmall)
            FormFields(form)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Button(onClick = { form.build(existing)?.let(onSave) }) { Text(stringResource(R.string.birthday_save)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                if (existing != null) {
                    OutlinedButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.birthday_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.birthday_delete_title, existing.name)) },
            text = { Text(stringResource(R.string.birthday_delete_body)) },
            confirmButton = { TextButton(onClick = { onDelete(existing.id) }) { Text(stringResource(R.string.birthday_delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun FormFields(form: FormState) {
    OutlinedTextField(
        value = form.name,
        onValueChange = { form.name = it },
        label = { Text(stringResource(R.string.birthday_name)) },
        isError = form.nameError,
        supportingText = { if (form.nameError) Text(stringResource(R.string.birthday_name_required)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        OutlinedTextField(
            value = form.day,
            onValueChange = { form.day = it.filter(Char::isDigit).take(2) },
            label = { Text(stringResource(R.string.birthday_day)) },
            isError = form.dayError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        MonthField(form.month, onMonth = { form.month = it }, modifier = Modifier.weight(2f))
    }
    if (form.dayError) {
        Text(stringResource(R.string.birthday_day_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    OutlinedTextField(
        value = form.year,
        onValueChange = { form.year = it.filter(Char::isDigit).take(YEAR_DIGITS) },
        label = { Text(stringResource(R.string.birthday_year)) },
        isError = form.yearError,
        supportingText = { Text(stringResource(if (form.yearError) R.string.birthday_year_invalid else R.string.birthday_year_help)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.note,
        onValueChange = { form.note = it.take(MAX_NOTE) },
        label = { Text(stringResource(R.string.birthday_note)) },
        supportingText = { Text(stringResource(R.string.birthday_note_help)) },
        maxLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Month by name, in the app's language. */
@Composable
private fun MonthField(month: Int, onMonth: (Int) -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    var open by rememberSaveable { mutableStateOf(false) }
    fun label(m: Month) = m.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.uppercase(locale) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().padding(top = Spacing.s)) { Text(label(Month.of(month))) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Month.entries.forEach { m ->
                DropdownMenuItem(text = { Text(label(m)) }, onClick = {
                    onMonth(m.value)
                    open = false
                })
            }
        }
    }
}

private const val MIN_YEAR = 1900
private const val YEAR_DIGITS = 4
private const val MAX_NOTE = 200
