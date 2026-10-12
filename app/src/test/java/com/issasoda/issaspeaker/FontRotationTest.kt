package com.issasoda.issaspeaker

import com.issasoda.issaspeaker.ui.theme.FontPool
import com.issasoda.issaspeaker.ui.theme.FontRotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FontRotationTest {

    private val size = FontPool.families.size

    @Test
    fun poolHasAtLeastOneHundredDistinctFamilies() {
        assertTrue("pool has $size families", size >= 100)
        assertEquals(size, FontPool.families.toSet().size)
    }

    @Test
    fun theSameDayAlwaysGivesTheSameFamily() {
        val day = LocalDate.of(2026, 10, 12).toEpochDay()
        assertEquals(FontRotation.indexFor(day, size), FontRotation.indexFor(day, size))
    }

    @Test
    fun everyFamilyAppearsOncePerCycle() {
        for (cycle in listOf(0L, 1L, 205L, 999L)) {
            val start = cycle * size
            val seen = (start until start + size).map { FontRotation.indexFor(it, size) }
            assertEquals("cycle $cycle", (0 until size).toSet(), seen.toSet())
        }
    }

    @Test
    fun neverTheSameFamilyTwoDaysInARow() {
        val today = LocalDate.of(2026, 10, 12).toEpochDay()
        (today until today + size * 40L).zipWithNext().forEach { (a, b) ->
            assertNotEquals("days $a and $b", FontRotation.indexFor(a, size), FontRotation.indexFor(b, size))
        }
    }

    @Test
    fun daysBeforeEpochAreValid() {
        val index = FontRotation.indexFor(-1, size)
        assertTrue(index in 0 until size)
    }
}
