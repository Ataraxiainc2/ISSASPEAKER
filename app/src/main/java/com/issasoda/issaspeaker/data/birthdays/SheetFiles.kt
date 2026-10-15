package com.issasoda.issaspeaker.data.birthdays

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * Opens the spreadsheet the owner picked in Android's file picker (catalog F134). A Google Sheet in Drive is a
 * "virtual" file with no bytes of its own: Drive converts it on request, so it is asked for CSV first, then Excel.
 * Excel and CSV files are read as they are; which one it is comes from the content, not the name. At most
 * [MAX_BYTES] are read. No Google permission is needed: the picker grants access to that one file only.
 */
object SheetFiles {

    private const val MAX_BYTES = 10 * 1024 * 1024
    private const val CSV = "text/csv"
    private const val XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private const val ZIP_MAGIC = "PK"

    /** The MIME types to offer in the picker: Google Sheets, Excel and CSV. */
    val pickerTypes = arrayOf(
        "application/vnd.google-apps.spreadsheet",
        XLSX,
        "application/vnd.ms-excel",
        CSV,
        "text/comma-separated-values",
        "application/csv",
        "text/plain",
    )

    /** The people in the picked file, or null when it could not be read. */
    suspend fun read(context: Context, uri: Uri): FileImport? = withContext(Dispatchers.IO) {
        try {
            val bytes = open(context, uri)?.use { it.readNBytes(MAX_BYTES) } ?: return@withContext null
            if (bytes.size >= 2 && String(bytes, 0, 2, Charsets.ISO_8859_1) == ZIP_MAGIC) {
                BirthdayImport.parseXlsx(bytes)
            } else {
                BirthdayImport.parseCsv(String(bytes, Charsets.UTF_8).removePrefix(Char(BYTE_ORDER_MARK).toString()))
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    /** A virtual document (a Google Sheet) is opened as an export; anything else as the file it is. */
    private fun open(context: Context, uri: Uri): InputStream? {
        val resolver = context.contentResolver
        if (!isVirtual(context, uri)) return resolver.openInputStream(uri)
        val offered = resolver.getStreamTypes(uri, "*/*").orEmpty()
        val type = listOf(CSV, XLSX).firstOrNull { it in offered } ?: offered.firstOrNull() ?: throw FileNotFoundException("No export offered")
        return resolver.openTypedAssetFileDescriptor(uri, type, null)?.createInputStream()
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
