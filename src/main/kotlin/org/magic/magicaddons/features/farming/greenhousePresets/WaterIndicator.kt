package org.magic.magicaddons.features.farming.greenhousePresets

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import org.magic.magicaddons.data.greenhouse.ElementRuntimeState
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo
import org.magic.magicaddons.data.greenhouse.WaterModel
import org.magic.magicaddons.render.WorldRenderer

/** Marks the soil of every plant below full water in the greenhouse the player stands in. */
object WaterIndicator {

    private const val CYAN: Int = 0xFF33E6FF.toInt()

    /** the fill pulses between these */
    private const val FILL_ALPHA_LOW: Int = 0x28
    private const val FILL_ALPHA_HIGH: Int = 0x70

    fun submit(poseStack: PoseStack, collector: SubmitNodeCollector, cameraPos: Vec3) {
        if (!GreenhousePresets.waterIndicatorOn()) return
        if (GreenhousePresets.waterIndicatorOnlyWithoutPlanner() && LayoutRenderState.hasSomethingToShow) return

        val grid = GreenhouseData.getCurrentGrid() ?: return
        // the grid is found by plot number, which a visited garden has too, so the plot standing
        // where the player's own greenhouse would be is not theirs to mark
        if (!GreenhouseData.inOwnGarden() || !GreenhouseData.inGreenhouse()) return

        val ignoreGrown = GreenhousePresets.waterIndicatorIgnoresGrown()

        val thirsty = grid.elements.filter { element ->
            val plant = element.instance
            val water = plant.waterLevel

            plant.needsWater && water != null && water < WaterModel.FULL &&
                    !(ignoreGrown && reachesFullGrowth(grid, element))
        }
        if (thirsty.isEmpty()) return

        val alpha = WorldRenderer.pulsedAlpha(FILL_ALPHA_LOW, FILL_ALPHA_HIGH)

        val presetBatch = WorldRenderer.BlockRenderBatch(cameraPos)
        thirsty.forEach { element ->
            val soil = grid.getPosForSlot(element.instance.slot) ?: return@forEach
            val footprint = element.instance.cropDef.footprint
            val box = Shapes.create(AABB(0.0, 0.0, 0.0, footprint.width.toDouble(), 1.0, footprint.height.toDouble()))
            presetBatch.fillWithOutline(soil, box, CYAN, alpha)
        }
        presetBatch.submitBatch(poseStack, collector)
    }

    /**
     * Whether the plant has the water to reach its last stage without ever falling below zero, which
     * is the plant that never skips a tick for want of water and so is worth leaving alone. A stage
     * only estimated is taken at its lowest, since that is the most growing it may still have to do.
     */
    private fun reachesFullGrowth(grid: GreenhouseGrid, element: ElementRuntimeState): Boolean {
        val plant = element.instance
        val water = plant.waterLevel ?: return false

        val stage = when (val growth = plant.growthStage) {
            is GrowthStageInfo.Known -> growth.stage
            is GrowthStageInfo.Estimated -> growth.range.first
            null -> return false
        }

        val ticksLeft = (plant.cropDef.maxStage - stage).coerceAtLeast(0)

        return WaterModel.after(water, ticksLeft, grid.layout.waterEffectAt(plant.slot)) >= 0
    }
}
