package org.magic.magicaddons.features.farming.greenhousePresets

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import org.magic.magicaddons.data.greenhouse.WaterModel
import org.magic.magicaddons.render.WorldRender

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
        if (!GreenhouseData.inGreenhouse()) return

        val thirsty = grid.elements.filter { element ->
            val plant = element.instance
            val water = plant.waterLevel
            plant.needsWater && water != null && water < WaterModel.FULL
        }
        if (thirsty.isEmpty()) return

        val alpha = WorldRender.pulsedAlpha(FILL_ALPHA_LOW, FILL_ALPHA_HIGH)

        val batch = WorldRender.Batch(cameraPos)
        thirsty.forEach { element ->
            val soil = grid.getPosForSlot(element.instance.slot) ?: return@forEach
            val footprint = element.instance.cropDef.footprint
            val box = Shapes.create(AABB(0.0, 0.0, 0.0, footprint.width.toDouble(), 1.0, footprint.height.toDouble()))
            batch.mark(soil, box, CYAN, alpha)
        }
        batch.submit(poseStack, collector)
    }
}
