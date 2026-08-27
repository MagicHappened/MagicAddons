package org.magic.magicaddons.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
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

    /** Full brightness: these are hints laid over the world, not blocks lit by it. */
    private const val FULL_BRIGHT: Int = 0xF000F0

    private const val NO_OVERLAY: Int = 0xA0000

    /** Thick enough to read from across a plot without hiding what it surrounds. */
    private const val OUTLINE_WIDTH: Float = 3f

    /**
     * Pulls every box in off its faces a little.
     *
     * Two marked blocks side by side share a face, and drawn flush their edges land on top of each
     * other and read as one long box rather than as two. A gap this small is invisible on its own
     * and enough to tell them apart.
     */
    private const val BOX_INSET: Double = 0.012

    private val RANDOM: RandomSource = RandomSource.create(0)

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

        val boxes = shape.toAabbs().map { it.inset() }

        atBlock(poseStack, cameraPos, pos) { pose ->
            collector.submitCustomGeometry(pose, RenderTypes.debugFilledBox()) { transform, consumer ->
                boxes.forEach { consumer.fillBox(transform, it, ARGB.color(fillAlpha, color)) }
            }

            collector.submitCustomGeometry(pose, RenderTypes.LINES) { transform, consumer ->
                boxes.forEach { consumer.outlineBox(transform, it, color) }
            }
        }
    }

    /**
     * Draws [state] at [pos] as it would look if it were there, see through and tinted, so a plan
     * reads as a plan. The box it would occupy is marked around it as well.
     *
     * The parts of a block model are collected the same way the world collects them, so a ghost has
     * the shape the real block would have.
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
        val parts = mutableListOf<BlockStateModelPart>()

        Minecraft.getInstance().modelManager.blockStateModelSet.get(state)
            .collectParts(RANDOM, parts)

        val tint = ARGB.color(fillAlpha, color)

        if (parts.isNotEmpty()) {
            atBlock(poseStack, cameraPos, pos) { pose ->
                collector.submitBlockModel(
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

    private fun AABB.inset(): AABB = AABB(
        minX + BOX_INSET, minY + BOX_INSET, minZ + BOX_INSET,
        maxX - BOX_INSET, maxY - BOX_INSET, maxZ - BOX_INSET
    )

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

    /** The twelve edges of [box], as its own box rather than joined to whatever sits beside it. */
    private fun VertexConsumer.outlineBox(pose: PoseStack.Pose, box: AABB, color: Int) {
        val x1 = box.minX.toFloat()
        val y1 = box.minY.toFloat()
        val z1 = box.minZ.toFloat()
        val x2 = box.maxX.toFloat()
        val y2 = box.maxY.toFloat()
        val z2 = box.maxZ.toFloat()

        edge(pose, color, x1, y1, z1, x2, y1, z1)
        edge(pose, color, x2, y1, z1, x2, y1, z2)
        edge(pose, color, x2, y1, z2, x1, y1, z2)
        edge(pose, color, x1, y1, z2, x1, y1, z1)

        edge(pose, color, x1, y2, z1, x2, y2, z1)
        edge(pose, color, x2, y2, z1, x2, y2, z2)
        edge(pose, color, x2, y2, z2, x1, y2, z2)
        edge(pose, color, x1, y2, z2, x1, y2, z1)

        edge(pose, color, x1, y1, z1, x1, y2, z1)
        edge(pose, color, x2, y1, z1, x2, y2, z1)
        edge(pose, color, x2, y1, z2, x2, y2, z2)
        edge(pose, color, x1, y1, z2, x1, y2, z2)
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

    private fun VertexConsumer.edge(
        pose: PoseStack.Pose,
        color: Int,
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float
    ) {
        val dx = bx - ax
        val dy = by - ay
        val dz = bz - az
        val length = Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()

        addVertex(pose, ax, ay, az).setColor(color).setNormal(pose, dx / length, dy / length, dz / length)
        addVertex(pose, bx, by, bz).setColor(color).setNormal(pose, dx / length, dy / length, dz / length)
    }
}
