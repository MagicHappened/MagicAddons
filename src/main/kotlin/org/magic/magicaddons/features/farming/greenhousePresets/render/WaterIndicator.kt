package org.magic.magicaddons.features.farming.greenhousePresets.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import org.magic.magicaddons.data.greenhouse.crops.PlantStage
import org.magic.magicaddons.data.greenhouse.crops.ScannedPlant
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.PlotPrediction
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.render.WorldRenderer

object WaterIndicator {

    private const val CYAN: Int = 0xFF33E6FF.toInt()

    private const val FILL_ALPHA_LOW: Int = 0x28
    private const val FILL_ALPHA_HIGH: Int = 0x70

    fun submitDryPlants(poseStack: PoseStack, collector: SubmitNodeCollector, cameraPos: Vec3) {
        if (!GreenhousePresets.waterIndicatorOn()) return
        if (GreenhousePresets.waterIndicatorOnlyWithoutPlanner() && LayoutRenderState.hasSomethingToShow) return

        if (!GreenhouseData.inOwnGreenhouse()) return
        val grid = GreenhouseData.getCurrentGrid() ?: return

        val ignoreGrown = GreenhousePresets.waterIndicatorIgnoresGrown()

        val dryPlants = grid.scannedPlants.filter { scannedPlant ->
            val plant = scannedPlant.plant
            val water = plant.waterLevel

            // todo add always show water highlight if a suggybud could spawn nearby
            val feedsDrainer = plant.cropDef.needsWater && grid.layout.plantsSurrounding(plant).any { it.cropDef.drainsNeighbours && !it.isFullyGrown }

            (plant.consumesWater || feedsDrainer) && !plant.cropDef.drainsNeighbours &&
                    (water == null || water < PlotPrediction.WATER_FULL_LEVEL) &&
                    !(ignoreGrown && !feedsDrainer && fullGrowthNoNegativeWater(grid, scannedPlant))
        }
        if (dryPlants.isEmpty()) return

        val alpha = WorldRenderer.pulsedAlpha(FILL_ALPHA_LOW, FILL_ALPHA_HIGH)

        val presetBatch = WorldRenderer.BlockRenderBatch(cameraPos)
        dryPlants.forEach { scannedPlant ->
            val soil = grid.getPosForSlot(scannedPlant.plant.slot) ?: return@forEach
            val footprint = scannedPlant.plant.cropDef.footprint
            val footprintBox = Shapes.create(AABB(0.0, 0.0, 0.0, footprint.width.toDouble(), 1.0, footprint.height.toDouble()))
            presetBatch.fillWithOutline(soil, footprintBox, CYAN, alpha)
        }
        presetBatch.submitBatch(poseStack, collector)
    }

    private fun fullGrowthNoNegativeWater(grid: GreenhouseGrid, scannedPlant: ScannedPlant): Boolean {
        val plant = scannedPlant.plant
        val water = plant.waterLevel ?: return false

        val stage = when (val growth = plant.growthStage) {
            is PlantStage.Known -> growth.stage
            is PlantStage.Estimated -> growth.range.first
            null -> return false
        }

        val ticksLeft = (plant.cropDef.maxStage - stage).coerceAtLeast(0)

        return PlotPrediction.waterLevelAfter(water, ticksLeft, GreenhouseGrid.waterEffectAt(grid.layout, plant.slot)) >= 0
    }
}
