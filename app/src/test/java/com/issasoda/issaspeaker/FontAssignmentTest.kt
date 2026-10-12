package com.issasoda.issaspeaker

import com.issasoda.issaspeaker.ui.theme.FontAssignment
import com.issasoda.issaspeaker.ui.theme.FontPool
import com.issasoda.issaspeaker.ui.theme.FontRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FontAssignmentTest {

    @Test
    fun poolHasAtLeastOneHundredDistinctFamilies() {
        assertTrue("pool has ${FontPool.families.size} families", FontPool.families.size >= 100)
        assertEquals(FontPool.families.size, FontPool.families.toSet().size)
    }

    @Test
    fun everyRoleGetsADifferentFamilyFromThePool() {
        repeat(200) { seed ->
            val assignment = FontAssignment.draw(FontPool.families, Random(seed))
            val families = FontRole.entries.map { assignment.familyFor(it) }
            assertEquals("seed $seed", FontRole.entries.size, families.toSet().size)
            assertTrue(families.all { it in FontPool.families })
        }
    }

    @Test
    fun drawsVaryBetweenRuns() {
        val draws = (0 until 50).map { FontAssignment.draw(FontPool.families, Random(it)) }.toSet()
        assertTrue("only ${draws.size} distinct draws in 50", draws.size > 45)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aPoolSmallerThanTheRolesIsRejected() {
        FontAssignment.draw(listOf("Inter", "Lato", "Inter"), Random(1))
    }
}
