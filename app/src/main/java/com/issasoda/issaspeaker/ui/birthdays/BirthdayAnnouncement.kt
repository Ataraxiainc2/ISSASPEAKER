package com.issasoda.issaspeaker.ui.birthdays

import android.content.Context
import android.text.format.DateFormat
import com.issasoda.issaspeaker.R
import com.issasoda.issaspeaker.data.birthdays.Birthday
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What the speaker says about birthdays, in the app's language (catalog F097). */
object BirthdayAnnouncement {

    /** Today's birthdays, one sentence each with the age when known; with none, who is next and when. */
    fun text(context: Context, upcoming: List<Birthday>, today: LocalDate): String {
        val res = context.resources
        val todays = upcoming.filter { it.isToday(today) }
        if (todays.isNotEmpty()) {
            return todays.joinToString(" ") { b ->
                b.turning(today)?.let { res.getQuantityString(R.plurals.birthday_say_with_age, it, b.name, it) }
                    ?: res.getString(R.string.birthday_say, b.name)
            }
        }
        val next = upcoming.firstOrNull() ?: return res.getString(R.string.birthday_say_none_at_all)
        return res.getString(R.string.birthday_say_next, next.name, dayMonth(next.next(today), res.configuration.locales[0]))
    }

    /** "25 de marzo", "March 25", "25 de março": the day and month as the language writes them. */
    fun dayMonth(date: LocalDate, locale: Locale): String =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMMM"), locale).format(date)
}
