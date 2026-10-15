package com.issasoda.issaspeaker.data.birthdays

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Birthdays already saved in the phone's contacts (catalog F134), read with READ_CONTACTS, which the owner grants
 * when choosing "Import from contacts". Only names and birthday events are read; nothing leaves the phone.
 */
object ContactBirthdays {

    suspend fun read(context: Context): List<ImportCandidate> = withContext(Dispatchers.IO) {
        val projection = arrayOf(ContactsContract.Data.DISPLAY_NAME, ContactsContract.CommonDataKinds.Event.START_DATE)
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
        val args = arrayOf(
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString(),
        )
        val found = mutableListOf<ImportCandidate>()
        context.contentResolver.query(ContactsContract.Data.CONTENT_URI, projection, selection, args, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                val date = cursor.getString(1)
                if (name != null && date != null) BirthdayImport.fromContact(name, date)?.let(found::add)
            }
        }
        found.distinctBy { "${it.name.lowercase()}|${it.month}|${it.day}" }.sortedBy { it.name.lowercase() }
    }
}
