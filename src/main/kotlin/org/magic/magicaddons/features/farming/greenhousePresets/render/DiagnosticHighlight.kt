package org.magic.magicaddons.features.farming.greenhousePresets.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import org.magic.magicaddons.data.greenhouse.plot.DiagnosticPlanner
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.render.WorldRenderer
import org.magic.magicaddons.util.SBLocation
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId

object DiagnosticHighlight {

    const val COLOR: Int = 0xFF33CCFF.toInt()

    fun submitPlantsToDiagnose(poseStack: PoseStack, collector: SubmitNodeCollector, cameraPos: Vec3) {
        if (!GreenhousePresets.diagnosticHighlightAlways() && !DiagnosticPlanner.isLinePinned) return
        if (!SBLocation.OwnGreenhouse.inside()) return
        if (Minecraft.getInstance().player?.mainHandItem?.getSkyBlockId()?.id != GreenhouseData.DIAGNOSTICS_TOOL_ID) return

        val grid = GreenhouseData.getCurrentGrid() ?: return
        val slotsToDiagnose = grid.state.slotsToDiagnose
        if (slotsToDiagnose.isEmpty()) return

        val batch = WorldRenderer.BlockRenderBatch(cameraPos)
        grid.scannedPlants.filter { (it.plant.slot.x to it.plant.slot.y) in slotsToDiagnose }.forEach { scannedPlant ->
            val soil = grid.getPosForSlot(scannedPlant.plant.slot) ?: return@forEach
            val footprint = scannedPlant.plant.cropDef.footprint
            batch.outline(soil, Shapes.create(AABB(0.0, 1.0, 0.0, footprint.width.toDouble(), 2.0, footprint.height.toDouble())), COLOR)
        }
        batch.submitBatch(poseStack, collector)
    }
}
