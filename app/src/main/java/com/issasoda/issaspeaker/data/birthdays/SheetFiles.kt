package com.issasoda.issaspeaker.data.birthdays

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

/** What reading a picked spreadsheet gave. */
sealed interface SheetRead {
    data class Read(val result: FileImport) : SheetRead

    /** A Google Sheet whose only export Drive offers on Android is PDF: it has to be saved as Excel first. */
    data object GoogleSheet : SheetRead

    data object Unreadable : SheetRead
}

/**
 * Opens the spreadsheet the owner picked in Android's file picker (catalog F134). A Google Sheet in Drive is a
 * "virtual" file with no bytes of its own: Drive converts it on request into one of the formats it offers, so each
 * offered format is tried (CSV, TSV, Excel first) until one yields people. On 2026-10-15 Drive offered only PDF for
 * Google Sheets, so the owner is told to save the sheet as Excel first ([SheetRead.GoogleSheet]). Excel, CSV and TSV files are read as they
 * are; which one it is comes from the content, not the name. At most [MAX_BYTES] are read. No Google permission is
 * needed: the picker grants access to that one file only.
 */
object SheetFiles {

    private const val TAG = "SheetFiles"
    private const val MAX_BYTES = 10 * 1024 * 1024
    private const val CSV = "text/csv"
    private const val TSV = "text/tab-separated-values"
    private const val XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private const val ZIP_MAGIC = "PK"
    private val preferred = listOf(CSV, "text/comma-separated-values", TSV, XLSX)

    /** The MIME types to offer in the picker: Google Sheets, Excel, CSV and TSV. */
    val pickerTypes = arrayOf(
        "application/vnd.google-apps.spreadsheet",
        XLSX,
        "application/vnd.ms-excel",
        CSV,
        "text/comma-separated-values",
        TSV,
        "application/csv",
        "text/plain",
    )

    /** The people in the picked file; a Google Sheet Drive will not convert; or a file that could not be read. */
    suspend fun read(context: Context, uri: Uri): SheetRead = withContext(Dispatchers.IO) {
        try {
            val result = if (isVirtual(context, uri)) readExport(context, uri) else context.contentResolver.openInputStream(uri)?.let(::parse)
            when {
                result != null -> SheetRead.Read(result)
                isVirtual(context, uri) -> SheetRead.GoogleSheet
                else -> SheetRead.Unreadable
            }
        } catch (e: IOException) {
            Log.w(TAG, "Spreadsheet could not be read", e)
            SheetRead.Unreadable
        } catch (e: SecurityException) {
            Log.w(TAG, "No access to the picked spreadsheet", e)
            SheetRead.Unreadable
        }
    }

    /** Each export Drive offers, best first, until one has people; the last one read otherwise. */
    private fun readExport(context: Context, uri: Uri): FileImport? {
        val resolver = context.contentResolver
        val offered = resolver.getStreamTypes(uri, "*/*").orEmpty().toList()
        Log.i(TAG, "Drive offers $offered") // Formats only; never content.
        val order = preferred.filter { it in offered } + offered.filter { it !in preferred && it.startsWith("text/") }
        var last: FileImport? = null
        val withPeople = order.asSequence()
            .mapNotNull { type -> resolver.openTypedAssetFileDescriptor(uri, type, null)?.createInputStream()?.let { type to parse(it) } }
            .onEach { (type, result) ->
                last = result
                Log.i(TAG, "Export $type: ${result?.candidates?.size ?: 0} people")
            }
            .firstOrNull { (_, result) -> result?.candidates?.isNotEmpty() == true }
        return withPeople?.second ?: last
    }

    private fun parse(stream: InputStream): FileImport? {
        val bytes = stream.use { it.readNBytes(MAX_BYTES) }
        return if (bytes.size >= 2 && String(bytes, 0, 2, Charsets.ISO_8859_1) == ZIP_MAGIC) {
            BirthdayImport.parseXlsx(bytes)
        } else {
            BirthdayImport.parseCsv(String(bytes, Charsets.UTF_8).removePrefix(Char(BYTE_ORDER_MARK).toString()))
        }
    }

    private fun isVirtual(context: Context, uri: Uri): Boolean {
        if (!DocumentsContract.isDocumentUri(context, uri)) return false
        context.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_FLAGS), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getInt(0) and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
        }
        return false
    }

    private const val BYTE_ORDER_MARK = 0xFEFF
}
