package com.issasoda.issaspeaker.data.birthdays

import java.time.LocalDate

/** A person read from a file or the contacts, before the owner confirms the import. */
data class ImportCandidate(val name: String, val month: Int, val day: Int, val year: Int?, val note: String)

/** A file read for import: the people found, how the dates were read, and the rows (to read them again swapped). */
data class FileImport(
    val candidates: List<ImportCandidate>,
    val dayFirst: Boolean,
    val skippedRows: Int,
    val rows: List<List<String>> = emptyList(),
)

/**
 * Reads birthdays from the owner's files and contacts (catalog F134): a Google Sheet picked in Drive, an Excel workbook
 * or a CSV (any column order, a header such as "Nombre", "Name", "FN", "Fecha", "Birthday", "Data de nascimento"),
 * and contact birthday strings. Nothing here is executed or sent anywhere; text only.
 */
object BirthdayImport {

    private val nameHeaders = listOf("nombre", "name", "nome", "persona", "person", "pessoa")
    private val dateHeaders = listOf("fn", "fecha", "nacimiento", "cumple", "birthday", "birth", "date", "data", "aniversario", "aniversário", "dob")
    private val noteHeaders = listOf("nota", "note", "notas", "notes", "dato", "fact", "observ")
    private const val MAX_ROWS = 5_000

    /** Parses CSV text. [dayFirst] null: decided from the data (a first number above 12 means day first), else day first. */
    fun parseCsv(text: String, dayFirst: Boolean? = null): FileImport = parseRows(csvRows(text), dayFirst)

    /** The first sheet of a workbook that has names and dates (a Google Sheet's other tabs are skipped). */
    fun parseXlsx(bytes: ByteArray): FileImport? =
        XlsxReader.sheets(bytes)?.map { parseRows(it, null) }?.let { sheets -> sheets.firstOrNull { it.candidates.isNotEmpty() } ?: sheets.first() }

    /** Rows from any source: a header row is found by its names; without one, name is column A and date column B. */
    fun parseRows(allRows: List<List<String>>, dayFirst: Boolean? = null): FileImport {
        val rows = allRows.filter { row -> row.any { it.isNotBlank() } }.take(MAX_ROWS)
        if (rows.isEmpty()) return FileImport(emptyList(), dayFirst ?: true, 0)
        val header = rows.first().map { it.trim().lowercase() }
        val nameColumn = header.indexOfFirst { h -> nameHeaders.any { h.startsWith(it) } }
        val dateColumn = header.indexOfFirst { h -> dateHeaders.any { h.startsWith(it) || h == it } }
        val noteColumn = header.indexOfFirst { h -> noteHeaders.any { h.startsWith(it) } }
        val hasHeader = nameColumn >= 0 && dateColumn >= 0
        val body = if (hasHeader) rows.drop(1) else rows
        val name = if (hasHeader) nameColumn else 0
        val date = if (hasHeader) dateColumn else 1
        val order = dayFirst ?: guessDayFirst(body.mapNotNull { it.getOrNull(date) })
        var skipped = 0
        val candidates = body.mapNotNull { row ->
            val person = row.getOrNull(name)?.trim().orEmpty()
            val parsed = row.getOrNull(date)?.let { parseDate(it, order) }
            if (person.isEmpty() || parsed == null) {
                skipped++
                null
            } else {
                ImportCandidate(person, parsed.month, parsed.day, parsed.year, if (noteColumn >= 0) row.getOrNull(noteColumn)?.trim().orEmpty() else "")
            }
        }
        return FileImport(candidates, order, skipped, rows)
    }

    /**
     * A date as people write it: 25/3/1990, 3/25/1990, 1990-03-25, 25-03, 25.03.1990, a contact's "--03-25", or an
     * Excel date (a serial day number such as 32957, counted from 30 December 1899).
     */
    fun parseDate(text: String, dayFirst: Boolean): ParsedDate? {
        val value = text.trim()
        value.toDoubleOrNull()?.takeIf { it >= MIN_SERIAL && it < MAX_SERIAL }?.let { serial ->
            val date = EXCEL_EPOCH.plusDays(serial.toLong())
            return checked(date.monthValue, date.dayOfMonth, date.year)
        }
        Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})""").find(value)?.let { m ->
            return checked(m.groupValues[2].toInt(), m.groupValues[3].toInt(), m.groupValues[1].toInt())
        }
        Regex("""^--(\d{1,2})-(\d{1,2})$""").find(value)?.let { m ->
            return checked(m.groupValues[1].toInt(), m.groupValues[2].toInt(), null)
        }
        val parts = value.split('/', '-', '.', ' ').filter { it.isNotBlank() }
        if (parts.size !in 2..3 || parts.any { p -> p.any { !it.isDigit() } }) return null
        val first = parts[0].toInt()
        val second = parts[1].toInt()
        val year = parts.getOrNull(2)?.toInt()?.let { if (it < 100) fullYear(it) else it }
        return if (dayFirst) checked(second, first, year) else checked(first, second, year)
    }

    /** Contacts store birthdays as "1990-03-25" or, without a year, "--03-25". */
    fun fromContact(name: String, value: String): ImportCandidate? =
        parseDate(value, dayFirst = false)?.let { ImportCandidate(name.trim(), it.month, it.day, it.year, "") }

    data class ParsedDate(val month: Int, val day: Int, val year: Int?)

    private fun checked(month: Int, day: Int, year: Int?): ParsedDate? {
        if (!Birthday.isValid(month, day)) return null
        val plausibleYear = year?.takeIf { it in MIN_YEAR..LocalDate.now().year }
        return ParsedDate(month, day, plausibleYear)
    }

    /** Two-digit years: 00-29 are 2000s, the rest 1900s. */
    private fun fullYear(twoDigits: Int) = if (twoDigits < CENTURY_PIVOT) 2000 + twoDigits else 1900 + twoDigits

    /** Day first unless some row can only be month first (its second number is above 12 and its first is not). */
    private fun guessDayFirst(values: List<String>): Boolean {
        val pairs = values.mapNotNull { v ->
            v.trim().split('/', '-', '.').takeIf { it.size >= 2 && !v.trim().startsWith("-") && v.trim().length < 12 }
                ?.let { it[0].toIntOrNull() to it[1].toIntOrNull() }
        }
        val monthFirst = pairs.any { (a, b) -> a != null && b != null && b > 12 && a <= 12 }
        val dayFirstSeen = pairs.any { (a, b) -> a != null && b != null && a > 12 && b <= 12 }
        return !(monthFirst && !dayFirstSeen)
    }

    /** RFC 4180 rows: commas or semicolons (whichever the first line has more of), quoted fields, CRLF or LF. */
    private fun csvRows(text: String): List<List<String>> {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        val separator = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','
        return CsvReader(text, separator).rows()
    }

    private class CsvReader(private val text: String, private val separator: Char) {
        private val rows = mutableListOf<List<String>>()
        private var row = mutableListOf<String>()
        private val field = StringBuilder()
        private var quoted = false
        private var i = 0

        fun rows(): List<List<String>> {
            while (i < text.length) {
                if (quoted) quotedChar(text[i]) else plainChar(text[i])
                i++
            }
            if (field.isNotEmpty() || row.isNotEmpty()) endRow()
            return rows
        }

        private fun quotedChar(c: Char) {
            when {
                c != '"' -> field.append(c)
                text.getOrNull(i + 1) == '"' -> field.append('"').also { i++ }
                else -> quoted = false
            }
        }

        private fun plainChar(c: Char) {
            when (c) {
                '"' -> quoted = true
                separator -> endField()
                '\n' -> endRow()
                '\r' -> {
                    if (text.getOrNull(i + 1) == '\n') i++
                    endRow()
                }
                else -> field.append(c)
            }
        }

        private fun endField() {
            row.add(field.toString())
            field.clear()
        }

        private fun endRow() {
            endField()
            rows.add(row)
            row = mutableListOf()
        }
    }

    private const val MIN_YEAR = 1900

    /** Excel serial dates: 1 March 1900 (61) to 31 December 2099; smaller numbers are a day or a month, not a date. */
    private const val MIN_SERIAL = 61.0
    private const val MAX_SERIAL = 73_051.0
    private val EXCEL_EPOCH: LocalDate = LocalDate.of(1899, 12, 30)
    private const val CENTURY_PIVOT = 30
}
