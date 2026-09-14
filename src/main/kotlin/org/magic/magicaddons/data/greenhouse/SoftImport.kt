package org.magic.magicaddons.data.greenhouse

/**
 * Lays an imported plot over an existing one instead of replacing it. The import is cut down to
 * the box its set slots cover, and that box is tried at every position it fits in, at each quarter
 * turn. Only the slots the import says something about change. Each plant removed costs
 * [PLANT_COST] and each soil overwritten [SOIL_COST]; the cheapest placement wins.
 */
object SoftImport {

    const val PLANT_COST: Int = 2
    const val SOIL_COST: Int = 1

    class Fit(
        val turns: Int,
        val plantsRemoved: Int,
        val soilsReplaced: Int,
        /** soils of the import landing on the same soil already there */
        val soilsReused: Int,
        /** sides of the box resting against the plot's edge or something already set */
        val snugness: Int,
        val offsetX: Int,
        val offsetY: Int,
        val layout: GreenhouseLayout
    ) {
        val cost: Int get() = plantsRemoved * PLANT_COST + soilsReplaced * SOIL_COST
    }

    /**
     * The best placement of [incoming] over [existing]. Ties go to the turn closest to
     * [preferredTurns], then to the most soils reused, then to the snuggest spot.
     */
    fun bestFit(existing: GreenhouseLayout, incoming: GreenhouseLayout, preferredTurns: Int): Fit {
        val fits = (0 until 4).flatMap { turns ->
            val turned = incoming.turned(turns)
            val box = boxOf(turned) ?: return@flatMap listOf(laidOver(existing, turned, turns, 0, 0, null))

            val offsetsX = -box.minX..(turned.size - 1 - box.maxX)
            val offsetsY = -box.minY..(turned.size - 1 - box.maxY)
            offsetsX.flatMap { offsetX ->
                offsetsY.map { offsetY -> laidOver(existing, shifted(turned, offsetX, offsetY), turns, offsetX, offsetY, box.shiftedBy(offsetX, offsetY)) }
            }
        }

        return fits.minWith(
            compareBy<Fit>(
                { it.cost },
                { turnDistance(it.turns, preferredTurns) },
                { -it.soilsReused },
                { -it.snugness },
                { Math.floorMod(it.turns - preferredTurns, 4) },
                { it.offsetY },
                { it.offsetX }
            )
        )
    }

    private class Box(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int) {
        fun shiftedBy(offsetX: Int, offsetY: Int) = Box(minX + offsetX, minY + offsetY, maxX + offsetX, maxY + offsetY)
    }

    /** The smallest box around every slot [layout] sets, or null when it sets nothing. */
    private fun boxOf(layout: GreenhouseLayout): Box? {
        val cells = layout.slots.filter { it.placedBlock != null || it.slotMark != null }.map { it.x to it.y } +
                layout.elementInstances.flatMap { cellsOf(it) }
        if (cells.isEmpty()) return null

        return Box(cells.minOf { it.first }, cells.minOf { it.second }, cells.maxOf { it.first }, cells.maxOf { it.second })
    }

    /** A copy of [layout] with everything moved by ([offsetX], [offsetY]); the caller keeps it on the grid. */
    private fun shifted(layout: GreenhouseLayout, offsetX: Int, offsetY: Int): GreenhouseLayout {
        if (offsetX == 0 && offsetY == 0) return layout

        val moved = GreenhouseLayout(id = layout.id, name = layout.name, size = layout.size)
        layout.slots.forEach { slot ->
            moved.getSlot(slot.x + offsetX, slot.y + offsetY)?.let {
                it.placedBlock = slot.placedBlock
                it.slotMark = slot.slotMark
            }
        }
        layout.elementInstances.forEach { plant ->
            val slot = moved.getSlot(plant.slot.x + offsetX, plant.slot.y + offsetY) ?: return@forEach
            moved.elementInstances.add(plant.copyForPrediction(slot))
        }
        return moved
    }

    /** Quarter turns between two turns either way round, so a half turn is the furthest. */
    private fun turnDistance(turns: Int, preferredTurns: Int): Int {
        val clockwise = Math.floorMod(turns - preferredTurns, 4)
        return minOf(clockwise, 4 - clockwise)
    }

    private fun laidOver(existing: GreenhouseLayout, incoming: GreenhouseLayout, turns: Int, offsetX: Int, offsetY: Int, box: Box?): Fit {
        val merged = existing.copy()
        var soilsReplaced = 0
        var soilsReused = 0

        val incomingPlantAt = mutableMapOf<Pair<Int, Int>, GreenhouseElementInstance>()
        incoming.elementInstances.forEach { plant -> cellsOf(plant).forEach { incomingPlantAt[it] = plant } }

        val touchedSlots = incoming.slots.filter { slot ->
            slot.placedBlock != null || slot.slotMark != null || (slot.x to slot.y) in incomingPlantAt
        }

        // a plant stays only where the import leaves its cells alone, or puts the same plant in the
        // same place, and never on a soil the import brings that it cannot grow on
        val samePlants = mutableSetOf<GreenhouseElementInstance>()
        val removedPlants = merged.elementInstances.filter { plant ->
            val twin = incoming.elementInstances.firstOrNull { it.sameAs(plant) }
            if (twin != null) {
                samePlants.add(twin)
                return@filter false
            }

            val accepted = acceptedSoils(plant)
            cellsOf(plant).any { cell ->
                cell in incomingPlantAt ||
                        incoming.getSlot(cell.first, cell.second)?.placedBlock?.let { it.block !in accepted } == true
            }
        }
        merged.elementInstances.removeAll(removedPlants.toSet())

        touchedSlots.forEach { incomingSlot ->
            val slot = merged.getSlot(incomingSlot.x, incomingSlot.y) ?: return@forEach
            val standingSoil = slot.placedBlock
            val incomingSoil = incomingSlot.placedBlock
            val incomingPlant = incomingPlantAt[incomingSlot.x to incomingSlot.y]

            if (incomingSoil != null) {
                when {
                    standingSoil == null -> Unit
                    standingSoil.block == incomingSoil.block -> soilsReused++
                    else -> soilsReplaced++
                }
                slot.placedBlock = incomingSoil
            } else if (incomingPlant != null && standingSoil != null && standingSoil.block !in acceptedSoils(incomingPlant)) {
                // a soil the imported plant cannot grow on is cleared rather than guessed at
                soilsReplaced++
                slot.placedBlock = null
            }

            if (incomingPlant != null || incomingSlot.slotMark != null) slot.slotMark = incomingSlot.slotMark
        }

        incoming.elementInstances
            .filter { it !in samePlants }
            .forEach { plant ->
                val slot = merged.getSlot(plant.slot.x, plant.slot.y) ?: return@forEach
                merged.elementInstances.add(plant.copyForPrediction(slot))
            }

        val snugness = box?.let { snugnessOf(existing, it) } ?: 0
        return Fit(turns, removedPlants.size, soilsReplaced, soilsReused, snugness, offsetX, offsetY, merged)
    }

    /**
     * How many of the four sides of [box] rest against the plot's edge or against slots [existing]
     * already sets along their whole length. A box pushed into a corner or against a build leaves
     * the free space around it in one piece.
     */
    private fun snugnessOf(existing: GreenhouseLayout, box: Box): Int {
        fun blocked(x: Int, y: Int): Boolean {
            val slot = existing.getSlot(x, y) ?: return true
            return slot.placedBlock != null || slot.slotMark != null || existing.plantCovering(slot) != null
        }

        val sides = listOf(
            (box.minX..box.maxX).all { blocked(it, box.minY - 1) },
            (box.minX..box.maxX).all { blocked(it, box.maxY + 1) },
            (box.minY..box.maxY).all { blocked(box.minX - 1, it) },
            (box.minY..box.maxY).all { blocked(box.maxX + 1, it) }
        )
        return sides.count { it }
    }

    private fun GreenhouseElementInstance.sameAs(other: GreenhouseElementInstance): Boolean =
        slot.x == other.slot.x && slot.y == other.slot.y && everyCrop.toSet() == other.everyCrop.toSet()

    private fun acceptedSoils(plant: GreenhouseElementInstance) =
        plant.everyCrop.flatMapTo(mutableSetOf()) { it.requiredSoil }

    private fun cellsOf(plant: GreenhouseElementInstance): List<Pair<Int, Int>> = buildList {
        for (offsetX in 0 until plant.cropDef.footprint.width) {
            for (offsetY in 0 until plant.cropDef.footprint.height) {
                add(plant.slot.x + offsetX to plant.slot.y + offsetY)
            }
        }
    }
}
