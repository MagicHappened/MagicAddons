package org.magic.magicaddons.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * Drawing single blocks into the world, for showing a player what a plot should look like.
 *
 * Positions come from the camera of the frame being drawn, handed in by the caller, rather than
 * from a camera read at some other moment. The pose stack of a world render is already relative to
 * the camera, so a block is drawn at its position minus the camera's; taking that camera from
 * anywhere else leaves the drawing lagging behind by however far the player moved in between, which
 * shows up as marks sliding while walking and settling once still.
 *
 * A block that is in the way is drawn from its [VoxelShape] rather than from its model. Every block
 * has a shape, whether its model is a full cube or a paper thin stalk of sugar cane, and the shape
 * matches what the block actually occupies, so farmland is marked as the low slab it is. The model
 * itself is never drawn a second time, which is what made the paper thin cases fail.
 */
object WorldRender {

    /** Thick enough to read from across a plot without hiding what it surrounds. */
    private const val OUTLINE_WIDTH: Float = 3f

    /**
     * Pulls an outlined box in off its faces a little.
     *
     * Two marked blocks side by side share a face, and drawn flush their edges land on top of each
     * other and read as one long box rather than as two. A gap this small is invisible on its own
     * and enough to tell them apart.
     */
    private const val OUTLINE_INSET: Double = 0.012

    /**
     * Pushes a filled box out past the block it covers.
     *
     * Drawn flush the fill sits exactly on the block's own faces and fights them for depth, and
     * drawn inside it is simply behind them, which is why an inset fill never appeared at all.
     */
    private const val FILL_EXPAND: Double = 0.002

    /**
     * Marks whatever stands at [pos]: the boxes of its shape filled in [color] at [fillAlpha], and
     * the edges of those same boxes drawn solid on top.
     *
     * Neither is depth tested, so a block behind another one still reads, which is the point when
     * the player is being told to go and deal with it.
     */
    fun mark(
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        cameraPos: Vec3,
        pos: BlockPos,
        shape: VoxelShape,
        color: Int,
        fillAlpha: Int
    ) {
        if (shape.isEmpty) return

        val boxes = shape.toAabbs()

        atBlock(poseStack, cameraPos, pos) { pose ->
            collector.submitCustomGeometry(pose, RenderTypes.debugFilledBox()) { transform, consumer ->
                boxes.forEach {
                    consumer.fillBox(transform, it.grow(FILL_EXPAND), ARGB.color(fillAlpha, color))
                }
            }

            // one call per box, so two marked blocks side by side stay two boxes rather than
            // merging into one long one the way a single combined shape would
            boxes.forEach { box ->
                collector.submitShapeOutline(
                    pose,
                    Shapes.create(box.grow(-OUTLINE_INSET)),
                    RenderTypes.LINES,
                    color,
                    OUTLINE_WIDTH,
                    false
                )
            }
        }
    }

    /**
     * Marks [pos] as somewhere [state] belongs and is not.
     *
     * Drawn as the box the block would fill rather than as the block itself. A block model carries
     * its own colours, and the tint handed to one only reaches quads that ask to be tinted, which
     * ordinary ground never does, so a ghost drawn as a model comes out fully opaque and reads as a
     * block that is really there. A see through box cannot be mistaken for one.
     */
    fun ghost(
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        cameraPos: Vec3,
        pos: BlockPos,
        state: BlockState,
        color: Int,
        fillAlpha: Int
    ) {
        val level = Minecraft.getInstance().level ?: return

        mark(poseStack, collector, cameraPos, pos, state.getShape(level, pos), color, fillAlpha)
    }

    /** Runs [action] with the pose stack sitting at [pos], as the game sets up its own outline. */
    private inline fun atBlock(
        poseStack: PoseStack,
        cameraPos: Vec3,
        pos: BlockPos,
        action: (PoseStack) -> Unit
    ) {
        poseStack.pushPose()
        poseStack.translate(
            pos.x - cameraPos.x,
            pos.y - cameraPos.y,
            pos.z - cameraPos.z
        )

        try {
            action(poseStack)
        } finally {
            poseStack.popPose()
        }
    }

    /** [by] out on every face, or in when it is negative. */
    private fun AABB.grow(by: Double): AABB = AABB(
        minX - by, minY - by, minZ - by,
        maxX + by, maxY + by, maxZ + by
    )

    /**
     * The six faces of [box], each wound both ways so that whichever winding the pipeline decides
     * to cull, the face is still there from the other side.
     */
    private fun VertexConsumer.fillBox(pose: PoseStack.Pose, box: AABB, color: Int) {
        val x1 = box.minX.toFloat()
        val y1 = box.minY.toFloat()
        val z1 = box.minZ.toFloat()
        val x2 = box.maxX.toFloat()
        val y2 = box.maxY.toFloat()
        val z2 = box.maxZ.toFloat()

        bothWays(pose, color, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1)
        bothWays(pose, color, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2)
        bothWays(pose, color, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1)
        bothWays(pose, color, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2)
        bothWays(pose, color, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2)
        bothWays(pose, color, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1)
    }

    private fun VertexConsumer.bothWays(
        pose: PoseStack.Pose,
        color: Int,
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        dx: Float, dy: Float, dz: Float
    ) {
        face(pose, color, ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz)
        face(pose, color, dx, dy, dz, cx, cy, cz, bx, by, bz, ax, ay, az)
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
