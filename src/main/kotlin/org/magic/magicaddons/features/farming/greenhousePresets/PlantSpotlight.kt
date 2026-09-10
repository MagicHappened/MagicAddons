package org.magic.magicaddons.features.farming.greenhousePresets

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.ElementRuntimeState
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.render.WorldRender
import org.magic.magicaddons.util.EntityUtils
import java.time.Duration
import java.time.Instant

/** The plant picked on the greenhouse screen, outlined in the world so it can be found. */
object PlantSpotlight : EntityUtils.HighlightSource {

    /** How long the plant stays lit after it is picked. */
    private val HOLD: Duration = Duration.ofSeconds(10)

    private const val RED: Int = 0xFFFF0000.toInt()

    /** Above the mob highlights, below the crop collector's own listing. */
    override val highlightPriority: Int = 50

    override fun highlightColor(entity: Entity): Int = RED

    /** The plant lit now, so picking it again puts it out. */
    private var lit: GreenhouseElementInstance? = null

    private var blocks: List<BlockPos> = emptyList()

    private var until: Instant? = null

    /** Lights this plant for ten seconds, or puts it out when it is the one already lit. */
    fun show(runtime: ElementRuntimeState) {
        val again = lit === runtime.instance

        clear()
        if (again) return

        lit = runtime.instance
        blocks = runtime.blocksMap?.keys?.toList().orEmpty()
        runtime.standEntities?.forEach { EntityUtils.add(it, this) }
        until = Instant.now().plus(HOLD)
    }

    fun clear() {
        EntityUtils.removeAllForSource(this)
        lit = null
        blocks = emptyList()
        until = null
    }

    /** Draws the blocks of the lit plant, from the frame's own render pass. */
    fun submit(poseStack: PoseStack, collector: SubmitNodeCollector, cameraPos: Vec3) {
        val ends = until ?: return
        if (Instant.now().isAfter(ends)) {
            clear()
            return
        }

        if (blocks.isEmpty()) return
        val level = Minecraft.getInstance().level ?: return

        val batch = WorldRender.Batch(cameraPos)
        blocks.forEach { batch.outline(it, level.getBlockState(it).getShape(level, it), RED) }
        batch.submit(poseStack, collector)
    }
}
