package com.issasoda.issaspeaker.ui.birthdays

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.issasoda.issaspeaker.IssaSpeakerApplication
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.data.birthdays.Birthday
import com.issasoda.issaspeaker.data.birthdays.BirthdayImport
import com.issasoda.issaspeaker.data.birthdays.BirthdaySource
import com.issasoda.issaspeaker.data.birthdays.ContactBirthdays
import com.issasoda.issaspeaker.data.birthdays.ImportCandidate
import com.issasoda.issaspeaker.data.birthdays.SheetFiles
import com.issasoda.issaspeaker.data.birthdays.upcoming
import com.issasoda.issaspeaker.speaker.AnnounceResult
import com.issasoda.issaspeaker.speaker.VoiceChoice
import com.issasoda.issaspeaker.ui.speaker.SpeakerViewModel
import com.issasoda.issaspeaker.ui.speaker.TestState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

/** People found by an import, waiting for the owner to tick who to add. [fileRows] let a file be re-read day/month swapped. */
data class ImportState(
    val source: BirthdaySource,
    val candidates: List<ImportCandidate>,
    val selected: Set<Int>,
    val dayFirst: Boolean,
    val skippedRows: Int,
    val fileRows: List<List<String>> = emptyList(),
)

/**
 * The birthdays tab (catalog F097, F131-F134): the list, the form, imports from contacts and files, and announcing
 * today's birthdays on the speaker in the app's language.
 */
class BirthdaysViewModel(app: Application) : AndroidViewModel(app) {
    private val services = app as IssaSpeakerApplication
    private val store = services.birthdays

    val birthdays: StateFlow<List<Birthday>> = store.birthdays

    private val _import = MutableStateFlow<ImportState?>(null)
    val import: StateFlow<ImportState?> = _import.asStateFlow()

    private val _announce = MutableStateFlow(TestState())
    val announce: StateFlow<TestState> = _announce.asStateFlow()

    init {
        viewModelScope.launch { store.load() }
    }

    fun save(birthday: Birthday) {
        viewModelScope.launch { store.save(birthday) }
    }

    fun delete(id: String) {
        viewModelScope.launch { store.delete(id) }
    }

    /** Reads the contacts' birthdays (the screen has asked for READ_CONTACTS). */
    fun importContacts() {
        viewModelScope.launch {
            val found = ContactBirthdays.read(getApplication())
            _import.value = review(BirthdaySource.CONTACTS, found, dayFirst = true, skipped = 0)
        }
    }

    /** Reads the spreadsheet the owner picked: a Google Sheet in Drive, an Excel workbook or a CSV. */
    fun importFile(uri: Uri) {
        viewModelScope.launch {
            val parsed = SheetFiles.read(getApplication(), uri)
            _import.value = parsed?.let { review(BirthdaySource.FILE, it.candidates, it.dayFirst, it.skippedRows).copy(fileRows = it.rows) }
                ?: ImportState(BirthdaySource.FILE, emptyList(), emptySet(), dayFirst = true, skippedRows = 0)
        }
    }

    /** The file's dates were read the wrong way round (3/4 as 3 April instead of 4 March): read them again swapped. */
    fun swapDateOrder() {
        val current = _import.value ?: return
        val parsed = BirthdayImport.parseRows(current.fileRows, dayFirst = !current.dayFirst)
        _import.value = review(BirthdaySource.FILE, parsed.candidates, parsed.dayFirst, parsed.skippedRows).copy(fileRows = current.fileRows)
    }

    fun toggle(index: Int) = _import.update { state ->
        state?.copy(selected = if (index in state.selected) state.selected - index else state.selected + index)
    }

    fun confirmImport() {
        val state = _import.value ?: return
        _import.value = null
        viewModelScope.launch { store.import(state.selected.sorted().map { state.candidates[it] }, state.source) }
    }

    fun cancelImport() {
        _import.value = null
    }

    /** Today's birthdays on the speaker, or that there are none and who is next. */
    fun announceToday() {
        val speaker = services.speakerStore.speaker.value
        if (speaker == null) {
            _announce.value = TestState(message = R.string.birthday_no_speaker)
            return
        }
        if (_announce.value.running) return
        _announce.value = TestState(running = true)
        viewModelScope.launch {
            val style = services.speakerStore.style.value
            val text = BirthdayAnnouncement.text(getApplication(), birthdays.value.upcoming(LocalDate.now()), LocalDate.now())
            val result = services.speakerAnnouncer.announce(speaker, text, VoiceChoice.decode(style.voice), style.volume, locale())
            _announce.value = when (result) {
                is AnnounceResult.Spoken -> TestState(message = R.string.birthday_announced, success = true)
                is AnnounceResult.Connection -> TestState(message = SpeakerViewModel.problemMessage(result.problem))
                else -> TestState(message = R.string.speak_playback_failed)
            }
        }
    }

    /** Everyone not already on the list starts ticked. */
    private fun review(source: BirthdaySource, found: List<ImportCandidate>, dayFirst: Boolean, skipped: Int): ImportState {
        val current = birthdays.value
        val fresh = found.indices.filter { i ->
            val c = found[i]
            current.none { it.sameAs(Birthday("", c.name, c.month, c.day)) }
        }.toSet()
        return ImportState(source, found, fresh, dayFirst, skipped)
    }

    private fun locale(): Locale = getApplication<Application>().resources.configuration.locales[0]
}
