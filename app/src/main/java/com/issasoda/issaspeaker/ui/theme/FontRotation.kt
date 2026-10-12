package com.issasoda.issaspeaker.ui.theme

import kotlin.random.Random

/**
 * Which typeface the app wears today (catalog F403, decision D2): a new family every day, in a random
 * order that does not repeat until the whole pool has been used.
 *
 * Days are grouped into cycles of `poolSize` days. Each cycle is a shuffle of the pool seeded with the
 * cycle number, so the choice is stable all day long, identical after a restart and needs no storage.
 * Where two cycles meet, the first family of the new cycle is swapped if it equals the last one of the
 * previous cycle, so the same face never shows two days in a row.
 */
object FontRotation {

    /** Fixed so every install walks the same sequence; changing it reshuffles all future days. */
    private const val SEED = 0x155A_5EA4L

    fun indexFor(epochDay: Long, poolSize: Int): Int {
        require(poolSize > 0) { "empty font pool" }
        val cycle = Math.floorDiv(epochDay, poolSize.toLong())
        val position = Math.floorMod(epochDay, poolSize.toLong()).toInt()
        return orderFor(cycle, poolSize)[position]
    }

    private fun orderFor(cycle: Long, poolSize: Int): List<Int> {
        val order = shuffled(cycle, poolSize).toMutableList()
        // Swapping positions 0 and 1 never moves the last element when there are more than two.
        if (poolSize > 2 && order[0] == shuffled(cycle - 1, poolSize).last()) {
            order[0] = order[1].also { order[1] = order[0] }
        }
        return order
    }

    private fun shuffled(cycle: Long, poolSize: Int): List<Int> =
        (0 until poolSize).shuffled(Random(SEED xor cycle))
}
