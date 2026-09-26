package org.magic.magicaddons.data.greenhouse.plot

import org.magic.magicaddons.data.greenhouse.crops.Plant

object SoftImport {

    private const val REMOVED_PLANT_COST: Int = 2
    private const val REPLACED_SOIL_COST: Int = 1

    class ImportPlacement(
        val turns: Int,
        val plantsRemoved: Int,
        val soilsReplaced: Int,
        val soilsReused: Int,
        val sidesAgainstBuild: Int,
        val offsetX: Int,
        val offsetY: Int,
        val mergedLayout: PlotLayout
    ) {
        val cost: Int get() = plantsRemoved * REMOVED_PLANT_COST + soilsReplaced * REPLACED_SOIL_COST
    }

    fun bestImportPlacement(existing: PlotLayout, incoming: PlotLayout, preferredTurns: Int): ImportPlacement {
        val placements = (0 until 4).flatMap { turns ->
            val importedLayoutAtTurn = incoming.turnedBy(turns)
            val importedBox = boundingBoxOf(importedLayoutAtTurn) ?: return@flatMap listOf(placeOverExisting(existing, importedLayoutAtTurn, turns, 0, 0, null))

            val offsetsX = -importedBox.minX..(importedLayoutAtTurn.size - 1 - importedBox.maxX)
            val offsetsY = -importedBox.minY..(importedLayoutAtTurn.size - 1 - importedBox.maxY)
            offsetsX.flatMap { offsetX ->
                offsetsY.map { offsetY -> placeOverExisting(existing, layoutShiftedBy(importedLayoutAtTurn, offsetX, offsetY), turns, offsetX, offsetY, importedBox.shiftedBy(offsetX, offsetY)) }
            }
        }

        return placements.minWith(
            compareBy<ImportPlacement>(
                { it.cost },
                { quarterTurnsApart(it.turns, preferredTurns) },
                { -it.soilsReused },
                { -it.sidesAgainstBuild },
                { Math.floorMod(it.turns - preferredTurns, 4) },
                { it.offsetY },
                { it.offsetX }
            )
        )
    }

    private class Box(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int) {
        fun shiftedBy(offsetX: Int, offsetY: Int) = Box(minX + offsetX, minY + offsetY, maxX + offsetX, maxY + offsetY)
    }

    private fun boundingBoxOf(layout: PlotLayout): Box? {
        val setCells = layout.slots.filter { it.soil != null || it.mark != null }.map { it.x to it.y } +
                layout.plants.flatMap { it.coveredCells }
        if (setCells.isEmpty()) return null

        return Box(setCells.minOf { it.first }, setCells.minOf { it.second }, setCells.maxOf { it.first }, setCells.maxOf { it.second })
    }

    private fun layoutShiftedBy(layout: PlotLayout, offsetX: Int, offsetY: Int): PlotLayout {
        if (offsetX == 0 && offsetY == 0) return layout

        val shiftedLayout = PlotLayout(id = layout.id, name = layout.name, size = layout.size)
        layout.slots.forEach { slot ->
            shiftedLayout.getSlot(slot.x + offsetX, slot.y + offsetY)?.let {
                it.soil = slot.soil
                it.mark = slot.mark
            }
        }
        layout.plants.forEach { plant ->
            val slot = shiftedLayout.getSlot(plant.slot.x + offsetX, plant.slot.y + offsetY) ?: return@forEach
            shiftedLayout.plants.add(plant.copyForPrediction(slot))
        }
        return shiftedLayout
    }

    private fun quarterTurnsApart(turns: Int, preferredTurns: Int): Int {
        val clockwiseTurns = Math.floorMod(turns - preferredTurns, 4)
        return minOf(clockwiseTurns, 4 - clockwiseTurns)
    }

    private fun placeOverExisting(existing: PlotLayout, incoming: PlotLayout, turns: Int, offsetX: Int, offsetY: Int, box: Box?): ImportPlacement {
        val mergedLayout = existing.freshCopy()
        var soilsReplaced = 0
        var soilsReused = 0

        val incomingPlantByCell = mutableMapOf<Pair<Int, Int>, Plant>()
        incoming.plants.forEach { plant -> plant.coveredCells.forEach { incomingPlantByCell[it] = plant } }

        val slotsSetByImport = incoming.slots.filter { slot ->
            slot.soil != null || slot.mark != null || (slot.x to slot.y) in incomingPlantByCell
        }

        val unchangedPlants = mutableSetOf<Plant>()
        val removedPlants = mergedLayout.plants.filter { plant ->
            val matchingIncoming = incoming.plants.firstOrNull { it.placementEquals(plant) }
            if (matchingIncoming != null) {
                unchangedPlants.add(matchingIncoming)
                return@filter false
            }

            val accepted = plant.requiredSoils
            plant.coveredCells.any { cell ->
                cell in incomingPlantByCell ||
                        incoming.getSlot(cell.first, cell.second)?.soil?.let { it !in accepted } == true
            }
        }
        mergedLayout.plants.removeAll(removedPlants.toSet())

        slotsSetByImport.forEach { incomingSlot ->
            val slot = mergedLayout.getSlot(incomingSlot.x, incomingSlot.y) ?: return@forEach
            val standingSoil = slot.soil
            val incomingSoil = incomingSlot.soil
            val incomingPlant = incomingPlantByCell[incomingSlot.x to incomingSlot.y]

            if (incomingSoil != null) {
                when {
                    standingSoil == null -> Unit
                    standingSoil == incomingSoil -> soilsReused++
                    else -> soilsReplaced++
                }
                slot.soil = incomingSoil
            } else if (incomingPlant != null && standingSoil != null && standingSoil !in incomingPlant.requiredSoils) {
                soilsReplaced++
                slot.soil = null
            }

            if (incomingPlant != null || incomingSlot.mark != null) slot.mark = incomingSlot.mark
        }

        incoming.plants
            .filter { it !in unchangedPlants }
            .forEach { plant ->
                val slot = mergedLayout.getSlot(plant.slot.x, plant.slot.y) ?: return@forEach
                mergedLayout.plants.add(plant.copyForPrediction(slot))
            }

        val sidesAgainstBuild = box?.let { sidesAgainstBuild(existing, it) } ?: 0
        return ImportPlacement(turns, removedPlants.size, soilsReplaced, soilsReused, sidesAgainstBuild, offsetX, offsetY, mergedLayout)
    }

    private fun sidesAgainstBuild(existing: PlotLayout, box: Box): Int {
        fun isCellTaken(x: Int, y: Int): Boolean {
            val slot = existing.getSlot(x, y) ?: return true
            return slot.soil != null || slot.mark != null || existing.plantCovering(slot) != null
        }

        val sides = listOf(
            (box.minX..box.maxX).all { isCellTaken(it, box.minY - 1) },
            (box.minX..box.maxX).all { isCellTaken(it, box.maxY + 1) },
            (box.minY..box.maxY).all { isCellTaken(box.minX - 1, it) },
            (box.minY..box.maxY).all { isCellTaken(box.maxX + 1, it) }
        )
        return sides.count { it }
    }
}
