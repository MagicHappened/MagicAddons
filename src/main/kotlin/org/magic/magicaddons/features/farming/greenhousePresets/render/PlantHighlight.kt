package org.magic.magicaddons.features.farming.greenhousePresets.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.ScannedPlant
import org.magic.magicaddons.data.greenhouse.Plant
import org.magic.magicaddons.render.WorldRenderer
import org.magic.magicaddons.util.EntityUtils
import java.time.Duration
import java.time.Instant

/** The plant picked on the greenhouse screen, outlined in the world so it can be found. */
object PlantHighlight : EntityUtils.HighlightSource {

    private val HIGHLIGHT_DURATION: Duration = Duration.ofSeconds(10)

    private const val RED: Int = 0xFFFF0000.toInt()

    override val highlightPriority: Int = 50

    override fun highlightColor(entity: Entity): Int = RED

    private var highlightedPlant: Plant? = null

    private var outlinedBlocks: List<BlockPos> = emptyList()

    private var highlightUntil: Instant? = null

    fun showPlant(scanned: ScannedPlant): Boolean {
        val samePlant = highlightedPlant === scanned.plant

        clear()
        if (samePlant) return false

        highlightedPlant = scanned.plant
        outlinedBlocks = scanned.blocks?.keys?.toList().orEmpty()
        scanned.stands?.forEach { EntityUtils.add(it, this) }
        highlightUntil = Instant.now().plus(HIGHLIGHT_DURATION)
        return true
    }

    fun clear() {
        EntityUtils.removeAllForSource(this)
        highlightedPlant = null
        outlinedBlocks = emptyList()
        highlightUntil = null
    }

    fun submitPlantHighlight(poseStack: PoseStack, collector: SubmitNodeCollector, cameraPos: Vec3) {
        val highlightEnds = highlightUntil ?: return
        if (Instant.now().isAfter(highlightEnds)) {
            clear()
            return
        }

        if (outlinedBlocks.isEmpty()) return
        val level = Minecraft.getInstance().level ?: return

        val blockBatch = WorldRenderer.BlockRenderBatch(cameraPos)
        outlinedBlocks.forEach { blockBatch.outline(it, level.getBlockState(it).getShape(level, it), RED) }
        blockBatch.submitBatch(poseStack, collector)
    }
}
