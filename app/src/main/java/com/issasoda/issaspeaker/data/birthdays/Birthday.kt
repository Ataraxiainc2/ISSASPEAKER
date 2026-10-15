package com.issasoda.issaspeaker.data.birthdays

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit

/** Where a birthday came from, so an import never adds the same person twice. */
@Serializable
enum class BirthdaySource { MANUAL, CONTACTS, FILE }

/**
 * One person's birthday (catalog F097, F134). [year] is optional: without it the app says the birthday but not the
 * age. [note] is what the owner wants said or remembered ("loves jazz", a fun fact).
 */
@Serializable
data class Birthday(
    val id: String,
    val name: String,
    val month: Int,
    val day: Int,
    val year: Int? = null,
    val note: String = "",
    val source: BirthdaySource = BirthdaySource.MANUAL,
) {
    /** The next date this birthday falls on, today included. 29 February falls on 28 February in common years. */
    fun next(today: LocalDate): LocalDate {
        val thisYear = onYear(today.year)
        return if (thisYear.isBefore(today)) onYear(today.year + 1) else thisYear
    }

    fun daysUntil(today: LocalDate): Long = ChronoUnit.DAYS.between(today, next(today))

    /** The age the person turns on [next], when the year is known. */
    fun turning(today: LocalDate): Int? = year?.let { next(today).year - it }

    fun isToday(today: LocalDate): Boolean = daysUntil(today) == 0L

    /** Same person for import purposes: same name (ignoring case and spaces) and same day and month. */
    fun sameAs(other: Birthday): Boolean =
        normalized(name) == normalized(other.name) && month == other.month && day == other.day

    private fun onYear(year: Int): LocalDate {
        val monthDay = MonthDay.of(month, day)
        return if (monthDay.isValidYear(year)) monthDay.atYear(year) else LocalDate.of(year, month, day - 1)
    }

    companion object {
        /** True for a real calendar day; 29 February is valid. */
        fun isValid(month: Int, day: Int): Boolean = month in 1..12 && day in 1..MonthDay.of(month, 1).month.maxLength()

        private fun normalized(name: String) = name.trim().lowercase().replace(Regex("\\s+"), " ")
    }
}

/** Upcoming first: today, then by days to go; same day by name. */
fun List<Birthday>.upcoming(today: LocalDate): List<Birthday> =
    sortedWith(compareBy<Birthday> { it.daysUntil(today) }.thenBy { it.name.lowercase() })
