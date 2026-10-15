package com.issasoda.issaspeaker.data.birthdays

import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.zip.ZipInputStream

/**
 * Reads the cell values of an Excel workbook (.xlsx), such as a Google Sheet exported by Drive, one list of rows per
 * sheet in workbook order. Deliberately small: it never processes DTDs or entities beyond the five XML ones and
 * numeric references (no XXE), caps every unpacked part at [MAX_PART_BYTES] (no zip bomb), and ignores formulas,
 * styles and everything else. Dates stay as Excel serial numbers; [BirthdayImport] converts them.
 */
object XlsxReader {

    private const val MAX_PART_BYTES = 20 * 1024 * 1024
    private const val MAX_PARTS = 2_000
    private val sheetPart = Regex("""^xl/worksheets/sheet(\d+)\.xml$""")
    private val cell = Regex("""<c\b([^>]*?)(?:/>|>(.*?)</c>)""", RegexOption.DOT_MATCHES_ALL)
    private val row = Regex("""<row\b[^>]*?(?:/>|>(.*?)</row>)""", RegexOption.DOT_MATCHES_ALL)
    private val sharedItem = Regex("""<si\b[^>]*>(.*?)</si>""", RegexOption.DOT_MATCHES_ALL)
    private val text = Regex("""<t\b[^>]*>(.*?)</t>""", RegexOption.DOT_MATCHES_ALL)
    private val value = Regex("""<v\b[^>]*>(.*?)</v>""", RegexOption.DOT_MATCHES_ALL)
    private val reference = Regex("""\br="([A-Z]+)\d+"""")
    private val type = Regex("""\bt="([a-zA-Z]+)"""")

    /** The sheets' rows, or null when the bytes are not a readable workbook. */
    fun sheets(bytes: ByteArray): List<List<List<String>>>? {
        val parts = unzip(bytes) ?: return null
        val shared = parts["xl/sharedStrings.xml"]?.let { xml -> sharedItem.findAll(xml).map { joinedText(it.groupValues[1]) }.toList() }.orEmpty()
        val sheets = parts.keys.mapNotNull { name -> sheetPart.find(name)?.groupValues?.get(1)?.toInt()?.let { it to name } }
            .sortedBy { it.first }
            .map { (_, name) -> rows(parts.getValue(name), shared) }
        return sheets.takeIf { it.isNotEmpty() }
    }

    private fun rows(xml: String, shared: List<String>): List<List<String>> =
        row.findAll(xml).map { match ->
            val cells = sortedMapOf<Int, String>()
            var next = 0
            cell.findAll(match.groupValues[1]).forEach { c ->
                val attributes = c.groupValues[1]
                val column = reference.find(attributes)?.groupValues?.get(1)?.let(::columnIndex) ?: next
                cells[column] = cellValue(type.find(attributes)?.groupValues?.get(1), c.groupValues[2], shared)
                next = column + 1
            }
            if (cells.isEmpty()) emptyList() else (0..cells.lastKey()).map { cells[it].orEmpty() }
        }.toList()

    private fun cellValue(kind: String?, inner: String, shared: List<String>): String = when (kind) {
        "s" -> value.find(inner)?.groupValues?.get(1)?.trim()?.toIntOrNull()?.let { shared.getOrNull(it) }.orEmpty()
        "inlineStr" -> joinedText(inner)
        else -> value.find(inner)?.groupValues?.get(1)?.let(::unescape).orEmpty()
    }

    /** All <t> runs of a rich-text item, joined. */
    private fun joinedText(xml: String): String = text.findAll(xml).joinToString("") { unescape(it.groupValues[1]) }

    /** "A" → 0, "Z" → 25, "AA" → 26. */
    private fun columnIndex(letters: String): Int = letters.fold(0) { acc, c -> acc * LETTERS + (c - 'A' + 1) } - 1

    private fun unescape(s: String): String = s
        .replace(Regex("""&#x([0-9a-fA-F]+);""")) { m -> m.groupValues[1].toIntOrNull(HEX)?.let { String(Character.toChars(it)) } ?: "" }
        .replace(Regex("""&#(\d+);""")) { m -> m.groupValues[1].toIntOrNull()?.let { String(Character.toChars(it)) } ?: "" }
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")

    /** The text parts of the workbook, by name; null when it is not a zip, has too many parts or a part is too large. */
    private fun unzip(bytes: ByteArray): Map<String, String>? = try {
        if (partCount(bytes) > MAX_PARTS) null else readParts(bytes)
    } catch (_: IOException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    /** First pass: how many parts the zip has (stops counting past the limit). */
    private fun partCount(bytes: ByteArray): Int =
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip -> generateSequence { zip.nextEntry }.take(MAX_PARTS + 1).count() }

    /** Second pass: unpack only the shared strings and the sheets, each capped at [MAX_PART_BYTES]. */
    private fun readParts(bytes: ByteArray): Map<String, String>? {
        val parts = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }
                .filter { it.name == "xl/sharedStrings.xml" || sheetPart.matches(it.name) }
                .forEach { entry ->
                    val data = zip.readNBytes(MAX_PART_BYTES + 1)
                    if (data.size > MAX_PART_BYTES) return null
                    parts[entry.name] = String(data, Charsets.UTF_8)
                }
        }
        return parts.takeIf { it.isNotEmpty() }
    }

    private const val LETTERS = 26
    private const val HEX = 16
}
