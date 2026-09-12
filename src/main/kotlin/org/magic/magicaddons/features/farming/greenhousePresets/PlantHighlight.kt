package org.magic.magicaddons.features.farming.greenhousePresets

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.ElementRuntimeState
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.render.WorldRenderer
import org.magic.magicaddons.util.EntityUtils
import java.time.Duration
import java.time.Instant

/** The plant picked on the greenhouse screen, outlined in the world so it can be found. */
object PlantHighlight : EntityUtils.HighlightSource {

    private val HIGHLIGHT_DURATION: Duration = Duration.ofSeconds(10)

    private const val RED: Int = 0xFFFF0000.toInt()

    /** Above the mob highlights, below the crop collector's own listing. */
    override val highlightPriority: Int = 50

    override fun highlightColor(entity: Entity): Int = RED

    private var highlightedPlant: GreenhouseElementInstance? = null

    private var blocks: List<BlockPos> = emptyList()

    private var highlightUntil: Instant? = null

    fun showPlant(runtime: ElementRuntimeState): Boolean {
        val samePlant = highlightedPlant === runtime.instance

        clear()
        if (samePlant) return false

        highlightedPlant = runtime.instance
        blocks = runtime.blocksMap?.keys?.toList().orEmpty()
        runtime.standEntities?.forEach { EntityUtils.add(it, this) }
        highlightUntil = Instant.now().plus(HIGHLIGHT_DURATION)
        return true
    }

    fun clear() {
        EntityUtils.removeAllForSource(this)
        highlightedPlant = null
        blocks = emptyList()
        highlightUntil = null
    }

    fun submitPlantHighlight(poseStack: PoseStack, collector: SubmitNodeCollector, cameraPos: Vec3) {
        val ends = highlightUntil ?: return
        if (Instant.now().isAfter(ends)) {
            clear()
            return
        }

        if (blocks.isEmpty()) return
        val level = Minecraft.getInstance().level ?: return

        val blockBatch = WorldRenderer.BlockRenderBatch(cameraPos)
        blocks.forEach { blockBatch.outline(it, level.getBlockState(it).getShape(level, it), RED) }
        blockBatch.submitBatch(poseStack, collector)
    }
}
