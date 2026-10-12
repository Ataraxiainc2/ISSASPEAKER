package com.issasoda.issaspeaker.ui.theme

import kotlin.random.Random

/** The text roles that each get their own typeface. */
enum class FontRole { DISPLAY, TITLE, BODY, LABEL }

/**
 * Which family each text role uses for this run of the app: four different families, drawn at random
 * from the pool (decision D9: always random, never pinned, different faces across the app).
 */
data class FontAssignment(val families: Map<FontRole, String>) {

    fun familyFor(role: FontRole): String = families.getValue(role)

    companion object {
        fun draw(pool: List<String>, random: Random): FontAssignment {
            require(pool.distinct().size >= FontRole.entries.size) { "the pool needs one family per role" }
            val picked = pool.distinct().shuffled(random).take(FontRole.entries.size)
            return FontAssignment(FontRole.entries.zip(picked).toMap())
        }
    }
}
