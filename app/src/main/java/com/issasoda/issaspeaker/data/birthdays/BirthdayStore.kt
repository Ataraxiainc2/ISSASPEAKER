package com.issasoda.issaspeaker.data.birthdays

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * The birthday list on the phone (catalog F134), in `files/birthdays.json`, kept out of cloud backup and device
 * transfer (data_extraction_rules.xml): these are other people's dates. The copy in the owner's own Google Sheet
 * (next step) is what follows them to a new phone.
 */
class BirthdayStore(context: Context) {

    private val file = File(context.filesDir, FILE_NAME)
    private val mutex = Mutex()
    private val state = MutableStateFlow<List<Birthday>>(emptyList())
    private var loaded = false

    val birthdays: StateFlow<List<Birthday>> = state.asStateFlow()

    suspend fun load() = mutex.withLock { ensureLoaded() }

    /** Adds or replaces (same id) one birthday. */
    suspend fun save(birthday: Birthday) = mutex.withLock {
        ensureLoaded()
        write(state.value.filter { it.id != birthday.id } + birthday)
    }

    suspend fun delete(id: String) = mutex.withLock {
        ensureLoaded()
        write(state.value.filter { it.id != id })
    }

    /** Adds the candidates that are not already in the list; returns how many were added. */
    suspend fun import(candidates: List<ImportCandidate>, source: BirthdaySource): Int = mutex.withLock {
        ensureLoaded()
        val current = state.value
        val added = candidates
            .map { Birthday(UUID.randomUUID().toString(), it.name, it.month, it.day, it.year, it.note, source) }
            .filter { new -> current.none { it.sameAs(new) } }
            .distinctBy { "${it.name.trim().lowercase()}|${it.month}|${it.day}" }
        if (added.isNotEmpty()) write(current + added)
        added.size
    }

    /** "Erase everything". */
    suspend fun clear() = mutex.withLock {
        withContext(Dispatchers.IO) { file.delete() }
        state.value = emptyList()
        loaded = true
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        state.value = withContext(Dispatchers.IO) { read() }
        loaded = true
    }

    private suspend fun write(list: List<Birthday>) {
        withContext(Dispatchers.IO) {
            val temp = File(file.parentFile, "$FILE_NAME.tmp")
            temp.writeText(json.encodeToString(list))
            check(temp.renameTo(file)) { "Could not replace $FILE_NAME" }
        }
        state.value = list
    }

    private fun read(): List<Birthday> = try {
        if (file.exists()) json.decodeFromString<List<Birthday>>(file.readText()) else emptyList()
    } catch (e: SerializationException) {
        Log.w(TAG, "Unreadable birthday file; starting empty", e)
        emptyList()
    }

    private companion object {
        const val TAG = "BirthdayStore"
        const val FILE_NAME = "birthdays.json"
        val json = Json { ignoreUnknownKeys = true }
    }
}
