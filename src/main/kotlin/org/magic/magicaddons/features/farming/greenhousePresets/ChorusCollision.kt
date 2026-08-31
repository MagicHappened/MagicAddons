package org.magic.magicaddons.features.farming.greenhousePresets

import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Whether the chorus fruit standing in a greenhouse will start destroying the plot before the
 * player is next looking at it.
 *
 * Chorus teleports on every growth stage below its last, and every chorus on the plot teleports in
 * the same tick, onto distinct free tiles. So while there are at least as many free tiles as
 * moving chorus, nothing is hurt; the moment there are more movers than free tiles, each spare
 * mover lands on a random tile and destroys whatever stood there. A magic jellybean being grown to
 * its last stage is worth about nine million coins, so the plot losing one costs far more than the
 * two or three stage-one chorus a player would give up to keep the margin.
 *
 * The naive check - free tiles minus chorus below zero - only reports a collision that is already
 * happening. What matters is whether one becomes likely over the ticks the player will be away,
 * since every mutation born in that window costs two margin: it takes a free tile and adds a
 * mover. This sizes the coming births to a high quantile rather than their mean, deliberately
 * pessimistic, and says how many chorus to break to stay clear of it.
 *
 * The model, the discord thread it came from and the Monte Carlo that checks this arithmetic are
 * in notes/chorus-teleport-model.md and notes/chorus-sim.js. Everything the thread left unproven
 * is written down there; the rule below was chosen not to depend on any of it.
 */
object ChorusCollision {

    /** The crops the rule is about, by the names the registry files them under. */
    const val CHORUS: String = "Chorus Fruit"
    const val JELLYBEAN: String = "Magic Jellybean"
    const val CHLORONITE: String = "Chloronite"

    /**
     * What a mutation spawner is made of: an empty tile whose eight neighbours are exactly three
     * magic jellybeans and five chloronite. The ring fills all eight neighbours, so a spawn tile
     * can never sit on the plot border.
     */
    const val RING_JELLYBEANS: Int = 3
    const val RING_CHLORONITE: Int = 5

    /** An open spawn tile's chance of producing a chorus in one growth tick. */
    const val SPAWN_CHANCE: Double = 0.25

    /**
     * How many standard deviations of birth count to plan for. The cost is lopsided - a handful of
     * stage-one chorus against a nine million coin jellybean - so the safe side is worth buying at
     * the ninety-ninth percentile rather than at the mean.
     */
    const val QUANTILE: Double = 2.33

    /** The plot is square and this is its side, the same ten every layout is built on. */
    private const val SIZE: Int = 10

    /**
     * One greenhouse weighed against one absence.
     *
     * [margin] is free tiles minus movers as the plot stands; [need] is the margin the window
     * asks for; [cull] is how many of the youngest chorus to break to cover the difference, zero
     * when there is nothing to warn about.
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

    /**
     * What [grid] looks like [ticks] growth ticks from now, if nobody touches it.
     *
     * Null for a greenhouse with no chorus in it, which is every greenhouse this does not concern.
     */
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
            // only the jellybeans this plot grew itself. One bought and stood in a spawner ring is
            // scenery worth replacing; one climbing towards a hundred and twenty is the nine
            // million coins the whole warning exists to protect
            jelliesAtRisk = layout.elementInstances.count {
                it.cropDef.name == JELLYBEAN &&
                        it.grewInPlace &&
                        (it.lowestStage ?: 1) < it.cropDef.maxStage
            },
            ticks = ticks
        )
    }

    /**
     * The chorus to break first, youngest first: a stage-one chorus is the cheapest thing standing
     * on the plot, and breaking one buys twice what harvesting a ripe one does.
     */
    fun cullOrder(layout: GreenhouseLayout): List<GreenhouseElementInstance> = layout.elementInstances
        .filter { it.cropDef.name == CHORUS && (it.lowestStage ?: 1) < it.cropDef.maxStage }
        .sortedBy { it.lowestStage ?: 1 }

    /**
     * Which crop stands on each tile, by name, null for air. Indexed y * [SIZE] + x, and a crop
     * bigger than one tile is written into every tile it covers rather than only its corner.
     */
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
     * Open spawn tiles: empty tiles ringed by exactly the jellybean and chloronite counts a
     * spawner is made of.
     *
     * A chorus standing on a spawn tile blocks it for that tick, which is why only empty ones are
     * counted; rings may overlap, so one plant can serve several spawners.
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
