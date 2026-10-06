package org.magic.magicaddons.features.farming.greenhousePresets.warnings

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.sqrt
import kotlin.random.Random
import org.magic.magicaddons.data.greenhouse.crops.definitions.misc.DeadPlant
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic.ChorusFruit
import org.magic.magicaddons.data.greenhouse.plot.GREENHOUSE_SIZE
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout

object ChorusCollision {

    const val RISK_HORIZON_TICKS: Int = 36

    private const val RUN_COUNT: Int = 1_000_000

    private const val IMMEDIATE_RUN_COUNT: Int = 100_000

    private const val BATCH_RUN_COUNT: Int = 4_000

    private const val PLAN_RUN_COUNT: Int = 100_000

    private const val LONG_PLAN_CHECK_RUN_COUNT: Int = 400_000

    private const val LAST_FULL_CHECK_HORIZON: Int = 5

    private const val SWAP_SEED_STEP: Int = 1_000

    private const val PREFIX_SEED_STEP: Int = 2_000

    private const val MAX_THREADS: Int = 4

    private const val TILE_COUNT: Int = GREENHOUSE_SIZE * GREENHOUSE_SIZE

    private const val EMPTY: Int = -1
    private const val RIPE: Int = -2
    private const val PLANT: Int = -3
    private const val DEAD: Int = -4

    private const val NEVER: Int = Int.MAX_VALUE

    private val workers = Executors.newFixedThreadPool((Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, MAX_THREADS)) { task ->
        Thread(task, "MagicAddons chorus risk").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }

    private val planner = Executors.newSingleThreadExecutor { task ->
        Thread(task, "MagicAddons chorus break order").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }

    data class Break(val x: Int, val y: Int, val stage: Int, val isOnSpawnTile: Boolean, val chanceAfter: Double)

    class BreakOrder(val breaks: List<Break>, val finalChance: Double)

    internal class Decays(val tiles: IntArray, val ticks: IntArray)

    class Risk internal constructor(
        private val stageOrKind: IntArray,
        private val spawnTiles: IntArray,
        private val spawnTileLastTicks: IntArray,
        private val decays: Decays,
        private val spawnChance: Double
    ) {
        val fingerprint: Int = listOf(
            stageOrKind.contentHashCode(),
            spawnTiles.contentHashCode(),
            spawnTileLastTicks.contentHashCode(),
            decays.tiles.contentHashCode(),
            decays.ticks.contentHashCode(),
            spawnChance.hashCode()
        ).fold(0) { hash, part -> 31 * hash + part }

        val ripeCells: List<Pair<Int, Int>> = stageOrKind.indices.filter { stageOrKind[it] == RIPE }.map { cellOf(it) }

        fun calculate(): Calculation = Calculation(this)

        internal fun startTiles(): IntArray = stageOrKind.copyOf()

        internal fun isSpawnTile(tile: Int): Boolean = tile in spawnTiles

        internal fun lossesBy(horizon: Int, tiles: IntArray, seed: Long): Int {
            val random = Random(seed)
            val run = Run(spawnTiles, spawnTileLastTicks, decays, spawnChance)
            var losses = 0
            repeat(BATCH_RUN_COUNT) {
                if (run.firstLossTick(tiles.copyOf(), random, horizon) >= 0) losses++
            }
            return losses
        }

        internal fun lossesByTick(batch: Int): IntArray {
            val random = Random(fingerprint.toLong() * (RUN_COUNT / BATCH_RUN_COUNT) + batch)
            val lossesByTick = IntArray(RISK_HORIZON_TICKS)
            val run = Run(spawnTiles, spawnTileLastTicks, decays, spawnChance)
            repeat(BATCH_RUN_COUNT) {
                val lostAt = run.firstLossTick(stageOrKind.copyOf(), random)
                if (lostAt >= 0) lossesByTick[lostAt]++
            }
            return lossesByTick
        }
    }

    class Calculation internal constructor(private val risk: Risk) {
        val fingerprint: Int get() = risk.fingerprint

        @Volatile
        private var isCancelled: Boolean = false

        private val batches = List(RUN_COUNT / BATCH_RUN_COUNT) { batch ->
            CompletableFuture.supplyAsync({ if (isCancelled) null else risk.lossesByTick(batch) }, workers)
        }

        val lossChanceByTick: CompletableFuture<DoubleArray?> = CompletableFuture.allOf(*batches.toTypedArray()).thenApply {
            lossChanceOf(batches.map { it.join() ?: return@thenApply null })
        }

        val isRunning: Boolean get() = !lossChanceByTick.isDone

        val ripeCells: List<Pair<Int, Int>> get() = risk.ripeCells

        private val breakOrders = ConcurrentHashMap<Pair<Int, Double>, CompletableFuture<BreakOrder?>>()

        fun cancel() {
            isCancelled = true
        }

        fun breakOrder(horizon: Int, tolerance: Double): CompletableFuture<BreakOrder?> =
            breakOrders.computeIfAbsent(horizon to tolerance) {
                lossChanceByTick.thenApplyAsync({ chances -> chances?.let { planBreaks(horizon, tolerance, it[horizon - 1]) } }, planner)
            }

        private fun planBreaks(horizon: Int, tolerance: Double, chanceBefore: Double): BreakOrder? {
            val start = risk.startTiles()
            val chosen = mutableListOf<Int>()
            var chance = chanceBefore

            while (chance > tolerance) {
                val tiles = withoutTiles(start, chosen)
                val candidates = tiles.indices.filter { tiles[it] >= 1 }
                if (candidates.isEmpty()) break
                val step = chosen.size
                val chances = candidates.map { tile -> lossChanceBy(horizon, withoutTiles(tiles, listOf(tile)), PLAN_RUN_COUNT, step) }
                    .map { it.join() ?: return null }

                val checkedChances = mutableMapOf<Int, Double>()
                val youngestReaching = candidates.indices
                    .filter { chances[it] <= tolerance }
                    .sortedWith(compareBy({ tiles[candidates[it]] }, { !risk.isSpawnTile(candidates[it]) }, { chances[it] }))
                    .firstOrNull { index ->
                        val checked = lossChanceBy(horizon, withoutTiles(tiles, listOf(candidates[index])), checkRunCountFor(horizon), step).join() ?: return null
                        checkedChances[index] = checked
                        checked <= tolerance
                    }
                val picked = youngestReaching ?: run {
                    val best = chances.min()
                    val margin = 2 * sqrt(maxOf(best, 1.0 / PLAN_RUN_COUNT) * (1 - best) / PLAN_RUN_COUNT)
                    candidates.indices
                        .filter { chances[it] <= best + margin }
                        .minWith(compareBy({ tiles[candidates[it]] }, { !risk.isSpawnTile(candidates[it]) }, { chances[it] }))
                }
                chosen += candidates[picked]
                chance = checkedChances[picked] ?: chances[picked]
            }
            if (chance <= tolerance) chance = keepOldestChorus(horizon, tolerance, start, chosen, chance) ?: return null

            val chancesAfter = chosen.indices.map { count ->
                if (count == chosen.lastIndex) CompletableFuture.completedFuture(chance)
                else lossChanceBy(horizon, withoutTiles(start, chosen.take(count + 1)), PLAN_RUN_COUNT, PREFIX_SEED_STEP)
            }.map { it.join() ?: return null }

            val breaks = chosen.mapIndexed { index, tile ->
                val (x, y) = cellOf(tile)
                Break(x, y, start[tile], risk.isSpawnTile(tile), chancesAfter[index])
            }
            return BreakOrder(breaks, chance)
        }

        private fun keepOldestChorus(horizon: Int, tolerance: Double, start: IntArray, chosen: MutableList<Int>, chance: Double): Double? {
            var finalChance = chance

            for (position in chosen.indices.sortedByDescending { start[chosen[it]] }) {
                val younger = start.indices.filter { start[it] in 1 until start[chosen[position]] && it !in chosen }
                if (younger.isEmpty()) continue
                fun swapped(tile: Int) = chosen.toMutableList().also { it[position] = tile }

                val estimates = younger.map { tile -> lossChanceBy(horizon, withoutTiles(start, swapped(tile)), PLAN_RUN_COUNT, SWAP_SEED_STEP) }
                    .map { it.join() ?: return null }
                val accepted = younger.indices
                    .filter { estimates[it] <= tolerance }
                    .sortedWith(compareBy({ start[younger[it]] }, { !risk.isSpawnTile(younger[it]) }, { estimates[it] }))
                    .firstNotNullOfOrNull { index ->
                        val checked = lossChanceBy(horizon, withoutTiles(start, swapped(younger[index])), checkRunCountFor(horizon), SWAP_SEED_STEP).join() ?: return null
                        if (checked <= tolerance) younger[index] to checked else null
                    }
                if (accepted != null) {
                    chosen[position] = accepted.first
                    finalChance = accepted.second
                }
            }
            return finalChance
        }

        private fun withoutTiles(tiles: IntArray, broken: List<Int>): IntArray = tiles.copyOf().also { copy -> broken.forEach { copy[it] = EMPTY } }

        private fun checkRunCountFor(horizon: Int): Int = if (horizon <= LAST_FULL_CHECK_HORIZON) RUN_COUNT else LONG_PLAN_CHECK_RUN_COUNT

        private fun lossChanceBy(horizon: Int, tiles: IntArray, runs: Int, step: Int): CompletableFuture<Double?> {
            val batches = List(runs / BATCH_RUN_COUNT) { batch ->
                val seed = (risk.fingerprint.toLong() * (RUN_COUNT / BATCH_RUN_COUNT) + step) * (RUN_COUNT / BATCH_RUN_COUNT) + batch
                CompletableFuture.supplyAsync({ if (isCancelled) null else risk.lossesBy(horizon, tiles, seed) }, workers)
            }
            return CompletableFuture.allOf(*batches.toTypedArray()).thenApply {
                batches.sumOf { it.join() ?: return@thenApply null }.toDouble() / runs
            }
        }

        fun immediateLossChanceByTick(): DoubleArray =
            lossChanceOf(batches.take(IMMEDIATE_RUN_COUNT / BATCH_RUN_COUNT).mapIndexed { batch, future -> future.getNow(null) ?: risk.lossesByTick(batch) })

        private fun lossChanceOf(losses: List<IntArray>): DoubleArray {
            val runs = losses.size * BATCH_RUN_COUNT
            var runsLostSoFar = 0
            return DoubleArray(RISK_HORIZON_TICKS) { tick ->
                runsLostSoFar += losses.sumOf { it[tick] }
                runsLostSoFar.toDouble() / runs
            }
        }
    }

    private fun cellOf(tile: Int): Pair<Int, Int> = tile % GREENHOUSE_SIZE to tile / GREENHOUSE_SIZE

    fun riskOf(layout: PlotLayout, weightMultiplier: Double, nextTickInMs: Long?, tickMs: Long?): Risk? {
        val chorus = ChorusFruit.definition
        val stageOrKind = IntArray(TILE_COUNT) { EMPTY }
        val cropNames = arrayOfNulls<String>(TILE_COUNT)
        val decayTicks = IntArray(TILE_COUNT) { NEVER }
        var hasChorus = false

        layout.plants.forEach { plant ->
            val isChorus = plant.cropDef == chorus
            if (isChorus) hasChorus = true
            val stage = plant.lowestStage ?: 1
            val isDead = plant.cropDef == DeadPlant.definition
            val decayTick = if (isChorus || isDead) NEVER else ticksUntil(plant.decayRemainingMs, nextTickInMs, tickMs)

            plant.coveredCells.forEach { (x, y) ->
                if (x !in 0 until GREENHOUSE_SIZE || y !in 0 until GREENHOUSE_SIZE) return@forEach
                val tile = y * GREENHOUSE_SIZE + x
                cropNames[tile] = plant.cropDef.name
                decayTicks[tile] = decayTick
                stageOrKind[tile] = when {
                    isDead -> DEAD
                    !isChorus -> PLANT
                    stage >= chorus.maxStage -> RIPE
                    else -> stage
                }
            }
        }
        val spawnRecipe = chorus.spawnRule?.requiredNeighbourCells.orEmpty()
        fun namesAliveAt(tick: Int) = Array(TILE_COUNT) { tile -> cropNames[tile].takeIf { decayTicks[tile] > tick } }
        val spawnTiles = (0 until TILE_COUNT).filter { tile -> ringHolds(cropNames, tile, spawnRecipe) }.toIntArray()
        val spawnTileLastTicks = IntArray(spawnTiles.size) { index ->
            val ringBreaksAt = (0 until RISK_HORIZON_TICKS).firstOrNull { tick -> !ringHolds(namesAliveAt(tick), spawnTiles[index], spawnRecipe) }
            (ringBreaksAt ?: RISK_HORIZON_TICKS) - 1
        }
        if (!hasChorus && spawnTiles.none { stageOrKind[it] == EMPTY }) return null
        val decaying = (0 until TILE_COUNT).filter { stageOrKind[it] == PLANT && decayTicks[it] < RISK_HORIZON_TICKS }
        val decays = Decays(decaying.toIntArray(), decaying.map { decayTicks[it] }.toIntArray())
        val spawnChance = (chorus.spawnRule?.weight ?: 0) * weightMultiplier / 100.0

        return Risk(stageOrKind, spawnTiles, spawnTileLastTicks, decays, spawnChance)
    }

    private fun ticksUntil(remainingMs: Long?, nextTickInMs: Long?, tickMs: Long?): Int {
        if (remainingMs == null || nextTickInMs == null || tickMs == null || tickMs <= 0) return NEVER
        if (remainingMs <= nextTickInMs) return 0

        return ((remainingMs - nextTickInMs + tickMs - 1) / tickMs).coerceAtMost(NEVER.toLong()).toInt()
    }

    private fun ringHolds(cropNames: Array<String?>, tile: Int, recipe: Map<String, Int>): Boolean {
        val x = tile % GREENHOUSE_SIZE
        val y = tile / GREENHOUSE_SIZE

        for ((crop, needed) in recipe) {
            var found = 0
            for (dy in -1..1) {
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = x + dx
                    val ny = y + dy
                    if (nx !in 0 until GREENHOUSE_SIZE || ny !in 0 until GREENHOUSE_SIZE) continue
                    if (cropNames[ny * GREENHOUSE_SIZE + nx] == crop) found++
                }
            }
            if (found < needed) return false
        }
        return true
    }

    private class Run(
        private val spawnTiles: IntArray,
        private val spawnTileLastTicks: IntArray,
        private val decays: Decays,
        private val spawnChance: Double
    ) {
        private val maxStage = ChorusFruit.definition.maxStage
        private val free = IntArray(TILE_COUNT)
        private val movers = IntArray(TILE_COUNT)
        private val occupied = IntArray(TILE_COUNT)
        private val landed = IntArray(TILE_COUNT)
        private val stages = IntArray(TILE_COUNT)

        fun firstLossTick(tiles: IntArray, random: Random, horizon: Int = RISK_HORIZON_TICKS): Int {
            for (tick in 0 until horizon) {
                for (index in decays.tiles.indices) {
                    val tile = decays.tiles[index]
                    if (decays.ticks[index] == tick && tiles[tile] == PLANT) tiles[tile] = DEAD
                }

                var freeCount = 0
                var moverCount = 0
                var occupiedCount = 0
                for (tile in 0 until TILE_COUNT) {
                    val state = tiles[tile]
                    when {
                        state == EMPTY -> free[freeCount++] = tile
                        state in 1 until maxStage -> movers[moverCount++] = tile
                        else -> occupied[occupiedCount++] = tile
                    }
                }
                if (moverCount == 0 && spawnTileLastTicks.all { it < tick }) return -1

                shuffle(free, freeCount, random)
                for (index in 0 until moverCount) {
                    stages[index] = tiles[movers[index]]
                    tiles[movers[index]] = EMPTY
                }

                var lostPlant = false
                for (index in 0 until moverCount) {
                    landed[index] = if (index < freeCount) free[index] else {
                        var victim = occupied[random.nextInt(occupiedCount)]
                        while (tiles[victim] == EMPTY) victim = occupied[random.nextInt(occupiedCount)]
                        if (tiles[victim] == PLANT) lostPlant = true
                        tiles[victim] = EMPTY
                        victim
                    }
                }
                if (lostPlant) return tick

                for (index in 0 until moverCount) {
                    val stage = stages[index] + 1
                    tiles[landed[index]] = if (stage >= maxStage) RIPE else stage
                }

                for (index in spawnTiles.indices) {
                    val tile = spawnTiles[index]
                    if (tick <= spawnTileLastTicks[index] && tiles[tile] == EMPTY && random.nextDouble() < spawnChance) tiles[tile] = 1
                }
            }
            return -1
        }

        private fun shuffle(values: IntArray, count: Int, random: Random) {
            for (index in count - 1 downTo 1) {
                val other = random.nextInt(index + 1)
                val swapped = values[index]
                values[index] = values[other]
                values[other] = swapped
            }
        }
    }
}
