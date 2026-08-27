package org.magic.magicaddons.render

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.VoxelShape
import tech.thatgravyboat.skyblockapi.api.events.render.RenderWorldEvent

/**
 * Drawing single blocks into the world, for showing a player what a plot should look like.
 *
 * The world's own blocks are baked into chunk meshes, so nothing here tries to reach into one and
 * recolour it. A block that is in the way is instead outlined from its [VoxelShape], which is a
 * shape the game already keeps for every block and which exists whether the model is a full cube or
 * a paper thin stalk of sugar cane. A block that is missing is drawn as a model of its own, on top
 * of the world rather than inside it.
 */
object WorldRender {

    /** Full brightness: these are hints laid over the world, not blocks lit by it. */
    private const val FULL_BRIGHT: Int = 0xF000F0

    private const val NO_OVERLAY: Int = 0xA0000

    /** Thick enough to read from across a plot without hiding what it surrounds. */
    private const val OUTLINE_WIDTH: Float = 3f

    private val RANDOM: RandomSource = RandomSource.create(0)

    /**
     * Outlines the shape of whatever stands at [pos] in [color].
     *
     * Drawn without depth testing, so a block behind another one still reads, which is the point
     * when the player is being told to go and remove it.
     */
    fun RenderWorldEvent.outlineBlock(pos: BlockPos, shape: VoxelShape, color: Int) {
        if (shape.isEmpty) return

        atCamera {
            pushPose()
            translate(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())

            submitNodeCollector.submitShapeOutline(
                this,
                shape,
                RenderTypes.LINES_TRANSLUCENT,
                color,
                OUTLINE_WIDTH,
                false
            )

            popPose()
        }
    }

    /**
     * Draws [state] at [pos] as it would look if it were there, tinted by [tint].
     *
     * The parts of a block model are collected the same way the world collects them, so a crop
     * drawn this way has the shape the real one would have.
     */
    fun RenderWorldEvent.ghostBlock(pos: BlockPos, state: BlockState, tint: Int) {
        val parts = mutableListOf<BlockStateModelPart>()

        Minecraft.getInstance().modelManager.blockStateModelSet.get(state)
            .collectParts(RANDOM, parts)

        if (parts.isEmpty()) return

        atCamera {
            pushPose()
            translate(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())

            submitNodeCollector.submitBlockModel(
                this,
                RenderTypes.translucentMovingBlock(),
                parts,
                intArrayOf(tint),
                FULL_BRIGHT,
                NO_OVERLAY,
                tint
            )

            popPose()
        }
    }
}
