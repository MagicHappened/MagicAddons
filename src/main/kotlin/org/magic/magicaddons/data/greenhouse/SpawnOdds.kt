package org.magic.magicaddons.data.greenhouse

import kotlin.math.max

object SpawnOdds {

    class MutationChance(val crop: CropDefinition, val chance: Double)

    private val POSITIVE_EFFECT_NAMES: Map<CropEffect.Kind, String> = mapOf(
        CropEffect.Kind.Yield to "Harvest Boost",
        CropEffect.Kind.Xp to "XP Boost",
        CropEffect.Kind.Water to "Water Retain",
        CropEffect.Kind.Drops to "Bonus Drops",
        CropEffect.Kind.Immunity to "Immunity",
        CropEffect.Kind.Spread to "Effect Spread"
    )

    /** a blank weight fills every roll up to this */
    private const val MIN_ROLL_WEIGHT_TOTAL: Double = 100.0

    fun mutationChancesAtSlot(
        layout: GreenhouseLayout,
        x: Int,
        y: Int,
        weightMultiplier: Double,
        ignoredPlant: Plant? = null
    ): List<MutationChance> {
        val weights = CropRegistry.all
            .filter { crop -> (crop.spawnRule?.weight ?: 0) > 0 && missingSpawnConditions(layout, crop, x, y, ignoredPlant).isEmpty() }
            .associateWith { it.spawnRule!!.weight * weightMultiplier }
        val rollTotal = max(MIN_ROLL_WEIGHT_TOTAL, weights.values.sum())

        return weights.map { (crop, weight) -> MutationChance(crop, weight / rollTotal) }.sortedByDescending { it.chance }
    }

    fun missingSpawnConditions(
        layout: GreenhouseLayout,
        crop: CropDefinition,
        x: Int,
        y: Int,
        ignoredPlant: Plant? = null
    ): List<String> {
        val rule = crop.spawnRule ?: return listOf("never appears on its own")
        val width = crop.footprint.width
        val height = crop.footprint.height
        if (x < 0 || y < 0 || x + width > layout.size || y + height > layout.size) return listOf("does not fit here")

        fun plantOn(cellX: Int, cellY: Int): Plant? =
            layout.getSlot(cellX, cellY)?.let { layout.plantCovering(it) }?.takeUnless { it === ignoredPlant }

        val footprintCells = (x until x + width).flatMap { cellX -> (y until y + height).map { cellY -> cellX to cellY } }
        val missing = mutableListOf<String>()

        if (footprintCells.any { (cellX, cellY) -> plantOn(cellX, cellY) != null }) missing += "no room"

        val footprintSoils = footprintCells.map { (cellX, cellY) -> layout.getSlot(cellX, cellY)?.soil?.block }
        if (footprintSoils.any { it == null || it !in crop.requiredSoil }) {
            missing += "needs ${crop.requiredSoil.joinToString(" or ") { it.name.string }}"
        }

        val surroundingCells = ((x - 1)..(x + width)).flatMap { cellX -> ((y - 1)..(y + height)).map { cellY -> cellX to cellY } }
            .filter { (cellX, cellY) -> (cellX to cellY) !in footprintCells && cellX in 0 until layout.size && cellY in 0 until layout.size }
        val surroundingCellsByCrop = surroundingCells.mapNotNull { (cellX, cellY) -> plantOn(cellX, cellY) }
            .groupingBy { it.cropDef.name }
            .eachCount()

        if (rule.needsNoNeighbours && surroundingCellsByCrop.isNotEmpty()) missing += "needs no crop around it"

        rule.requiredNeighbourCells.forEach { (neighbourCrop, requiredCells) ->
            val missingCount = requiredCells - (surroundingCellsByCrop[neighbourCrop] ?: 0)
            if (missingCount > 0) missing += "$missingCount more $neighbourCrop"
        }

        if (rule.needsAllPositiveEffects) {
            val effectsReceived = footprintCells.flatMap { (cellX, cellY) -> layout.getSlot(cellX, cellY)?.let { layout.effectsAt(it) }.orEmpty() }
            val missingEffects = POSITIVE_EFFECT_NAMES.filterKeys { kind -> effectsReceived.none { it.kind == kind && it.percent >= 0 } }.values
            if (missingEffects.isNotEmpty()) missing += "no ${missingEffects.joinToString(", ")}"
        }

        return missing
    }

    fun missingConditionsForTarget(layout: GreenhouseLayout, target: Plant): List<String> =
        missingSpawnConditions(layout, target.cropDef, target.slot.x, target.slot.y, ignoredPlant = target)

    fun unplannedMutationSpots(
        layout: GreenhouseLayout,
        plannedCropsBySlot: Map<Pair<Int, Int>, Set<CropDefinition>>
    ): Map<Pair<Int, Int>, List<CropDefinition>> {
        val mutations = CropRegistry.all.filter { (it.spawnRule?.weight ?: 0) > 0 }
        val spots = linkedMapOf<Pair<Int, Int>, List<CropDefinition>>()

        for (y in 0 until layout.size) {
            for (x in 0 until layout.size) {
                val slot = layout.getSlot(x, y) ?: continue
                val plantOnSlot = layout.plantCovering(slot)

                val waitingTarget = plantOnSlot?.takeIf { it.slot.mark == LayoutSlot.Marking.Target && it.slot === slot }
                if (plantOnSlot != null && waitingTarget == null) continue

                val plannedCrops = plannedCropsBySlot[x to y].orEmpty() + waitingTarget?.acceptedCrops.orEmpty()
                val spawnableMutations = mutations.filter { crop ->
                    crop !in plannedCrops && missingSpawnConditions(layout, crop, x, y, ignoredPlant = waitingTarget).isEmpty()
                }
                if (spawnableMutations.isNotEmpty()) spots[x to y] = spawnableMutations
            }
        }
        return spots
    }

    fun targetCropsBySlot(plan: GreenhouseLayout?): Map<Pair<Int, Int>, Set<CropDefinition>> =
        plan?.plants
            ?.filter { it.slot.mark == LayoutSlot.Marking.Target }
            ?.associate { (it.slot.x to it.slot.y) to it.acceptedCrops.toSet() }
            .orEmpty()
}
