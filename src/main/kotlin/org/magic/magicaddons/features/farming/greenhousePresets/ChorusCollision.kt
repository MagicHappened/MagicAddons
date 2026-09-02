package org.magic.magicaddons.features.farming.greenhousePresets

import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Whether the chorus in a greenhouse will run out of tiles to teleport into and start destroying
 * the plot. Sizes the mutations born while the player is away pessimistically, and says how many
 * young chorus to break. Model and simulation: notes/chorus-teleport-model.md, notes/chorus-sim.js.
 */
object ChorusCollision {

    /** The crops the rule is about, by the names the registry files them under. */
    const val CHORUS: String = "Chorus Fruit"
    const val JELLYBEAN: String = "Magic Jellybean"
    const val CHLORONITE: String = "Chloronite"

    /** A spawner is an empty tile ringed by exactly three jellybeans and five chloronite. */
    const val RING_JELLYBEANS: Int = 3
    const val RING_CHLORONITE: Int = 5

    /** An open spawn tile's chance of producing a chorus in one growth tick. */
    const val SPAWN_CHANCE: Double = 0.25

    /** Standard deviations of births to plan for: the ninety-ninth percentile, since a lost
     * jellybean costs far more than the chorus given up to avoid it. */
    const val QUANTILE: Double = 2.33

    /** The plot is square and this is its side, the same ten every layout is built on. */
    private const val SIZE: Int = 10

    /**
     * One greenhouse weighed against one absence: the margin it has, the margin it needs, and how
     * many young chorus to break to cover the difference.
     */
    data class Report(
        val movers: Int,
        val ripe: Int,
        val free: Int,
        val spawnOpen: Int,
        val ripening: Int,
        val margin: Int,
        val need: Int,
        val cull: Int,
        val jelliesAtRisk: Int,
        val ticks: Int
    ) {
        val warns: Boolean get() = cull > 0
    }

    /** What the greenhouse looks like that many ticks from now. Null when it holds no chorus. */
    fun analyse(grid: GreenhouseGrid, ticks: Int): Report? = analyse(grid.layout, ticks)

    fun analyse(layout: GreenhouseLayout, ticks: Int): Report? {
        if (ticks <= 0) return null

        val chorus = layout.elementInstances.filter { it.cropDef.name == CHORUS }
        if (chorus.isEmpty()) return null

        val maxStage = chorus.first().cropDef.maxStage

        // the lowest stage a plant might be at, so a plant only probably grown is still counted as
        // one that might teleport. Every guess here leans the same way: towards warning
        val movers = chorus.filter { (it.lowestStage ?: 1) < maxStage }
        val ripe = chorus.size - movers.size

        // a mover this close to the end stops moving inside the window, handing its tile back
        val ripening = movers.count { (it.lowestStage ?: 1) >= maxStage - ticks }

        val occupied = occupancy(layout)
        val free = SIZE * SIZE - occupied.count { it != null }
        val spawnOpen = countSpawners(occupied)

        val mean = SPAWN_CHANCE * spawnOpen * ticks
        val deviation = sqrt(spawnOpen * ticks * SPAWN_CHANCE * (1 - SPAWN_CHANCE))

        // a birth costs two margin: it fills a free tile and adds a mover to teleport into one
        val need = 2 * ceil(mean + QUANTILE * deviation).toInt()
        val margin = free - movers.size
        val have = margin + ripening

        // breaking a mover returns two margin, harvesting a ripe chorus returns one
        val cull = if (have < need) ceil((need - have) / 2.0).toInt() else 0

        return Report(
            movers = movers.size,
            ripe = ripe,
            free = free,
            spawnOpen = spawnOpen,
            ripening = ripening,
            margin = margin,
            need = need,
            cull = cull,
            // only jellybeans this plot grew itself: a bought one is scenery, a grown one is the
            // nine million coins the warning exists to protect
            jelliesAtRisk = layout.elementInstances.count {
                it.cropDef.name == JELLYBEAN &&
                        it.grewInPlace &&
                        (it.lowestStage ?: 1) < it.cropDef.maxStage
            },
            ticks = ticks
        )
    }

    /** The chorus to break first, youngest first: the cheapest thing standing on the plot. */
    fun cullOrder(layout: GreenhouseLayout): List<GreenhouseElementInstance> = layout.elementInstances
        .filter { it.cropDef.name == CHORUS && (it.lowestStage ?: 1) < it.cropDef.maxStage }
        .sortedBy { it.lowestStage ?: 1 }

    /** Which crop stands on each tile, by name, null for air. A big crop fills every tile it covers. */
    private fun occupancy(layout: GreenhouseLayout): Array<String?> {
        val tiles = arrayOfNulls<String>(SIZE * SIZE)

        layout.elementInstances.forEach { instance ->
            val footprint = instance.cropDef.footprint

            for (dy in 0 until footprint.height) {
                for (dx in 0 until footprint.width) {
                    val x = instance.slot.x + dx
                    val y = instance.slot.y + dy

                    if (x in 0 until SIZE && y in 0 until SIZE) {
                        tiles[y * SIZE + x] = instance.cropDef.name
                    }
                }
            }
        }

        return tiles
    }

    /**
     * Empty tiles with a full spawner ring around them. A chorus standing on one blocks it, and
     * rings may overlap.
     */
    private fun countSpawners(tiles: Array<String?>): Int {
        var open = 0

        for (y in 1 until SIZE - 1) {
            for (x in 1 until SIZE - 1) {
                if (tiles[y * SIZE + x] != null) continue

                var jellybeans = 0
                var chloronite = 0

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue

                        when (tiles[(y + dy) * SIZE + (x + dx)]) {
                            JELLYBEAN -> jellybeans++
                            CHLORONITE -> chloronite++
                        }
                    }
                }

                if (jellybeans == RING_JELLYBEANS && chloronite == RING_CHLORONITE) open++
            }
        }

        return open
    }
}
