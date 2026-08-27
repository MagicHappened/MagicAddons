package org.magic.magicaddons.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.VoxelShape
import tech.thatgravyboat.skyblockapi.api.events.render.RenderWorldEvent

/**
 * Drawing single blocks into the world, for showing a player what a plot should look like.
 *
 * Everything here is positioned the way the game positions its own block outline: the pose stack of
 * a world render is already relative to the camera, so a block is drawn at its position minus the
 * camera's. Doing it any other way leaves the drawing hanging off the camera and sliding as the
 * player moves.
 *
 * A block that is in the way is drawn from its [VoxelShape] rather than from its model. Every block
 * has a shape, whether its model is a full cube or a paper thin stalk of sugar cane, and the shape
 * matches what the block actually occupies, so farmland is called out as the low slab it is. The
 * model itself is never drawn a second time, which is what made the paper thin cases fail.
 */
object WorldRender {

    /** Full brightness: these are hints laid over the world, not blocks lit by it. */
    private const val FULL_BRIGHT: Int = 0xF000F0

    private const val NO_OVERLAY: Int = 0xA0000

    /** Thick enough to read from across a plot without hiding what it surrounds. */
    private const val OUTLINE_WIDTH: Float = 3f

    private val RANDOM: RandomSource = RandomSource.create(0)

    /**
     * Marks whatever stands at [pos]: its shape filled in [color] at [fillAlpha] of it, and the
     * edges of that same shape drawn solid on top.
     *
     * Neither is depth tested, so a block behind another one still reads, which is the point when
     * the player is being told to go and remove it.
     */
    fun RenderWorldEvent.markBlock(
        pos: BlockPos,
        shape: VoxelShape,
        color: Int,
        fillAlpha: Int
    ) {
        if (shape.isEmpty) return

        val boxes = shape.toAabbs()

        atBlock(pos) { pose ->
            submitNodeCollector.submitCustomGeometry(
                pose,
                RenderTypes.debugFilledBox()
            ) { transform, consumer ->
                boxes.forEach { box -> consumer.fillBox(transform, box, ARGB.color(fillAlpha, color)) }
            }

            submitNodeCollector.submitShapeOutline(
                pose,
                shape,
                RenderTypes.LINES,
                color,
                OUTLINE_WIDTH,
                false
            )
        }
    }

    /**
     * Draws [state] at [pos] as it would look if it were there, see through so the player can tell
     * a plan from a plant.
     *
     * The parts of a block model are collected the same way the world collects them, so a crop
     * drawn this way has the shape the real one would have.
     */
    fun RenderWorldEvent.ghostBlock(pos: BlockPos, state: BlockState, tint: Int) {
        val parts = mutableListOf<BlockStateModelPart>()

        Minecraft.getInstance().modelManager.blockStateModelSet.get(state)
            .collectParts(RANDOM, parts)

        if (parts.isEmpty()) return

        atBlock(pos) { pose ->
            submitNodeCollector.submitBlockModel(
                pose,
                RenderTypes.translucentMovingBlock(),
                parts,
                intArrayOf(tint),
                FULL_BRIGHT,
                NO_OVERLAY,
                tint
            )
        }
    }

    /** Runs [action] with the pose stack sitting at [pos], as the game sets up its own outline. */
    private inline fun RenderWorldEvent.atBlock(pos: BlockPos, action: (PoseStack) -> Unit) {
        val pose = poseStack

        pose.pushPose()
        pose.translate(
            pos.x - cameraPosition.x,
            pos.y - cameraPosition.y,
            pos.z - cameraPosition.z
        )

        try {
            action(pose)
        } finally {
            pose.popPose()
        }
    }

    /** The six faces of [box], wound so the fill is visible from any side. */
    private fun VertexConsumer.fillBox(pose: PoseStack.Pose, box: AABB, color: Int) {
        val x1 = box.minX.toFloat()
        val y1 = box.minY.toFloat()
        val z1 = box.minZ.toFloat()
        val x2 = box.maxX.toFloat()
        val y2 = box.maxY.toFloat()
        val z2 = box.maxZ.toFloat()

        face(pose, color, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1)
        face(pose, color, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2)
        face(pose, color, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1)
        face(pose, color, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2)
        face(pose, color, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2)
        face(pose, color, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1)
    }

    private fun VertexConsumer.face(
        pose: PoseStack.Pose,
        color: Int,
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        dx: Float, dy: Float, dz: Float
    ) {
        addVertex(pose, ax, ay, az).setColor(color)
        addVertex(pose, bx, by, bz).setColor(color)
        addVertex(pose, cx, cy, cz).setColor(color)
        addVertex(pose, dx, dy, dz).setColor(color)
    }
}
