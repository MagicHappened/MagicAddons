package org.magic.magicaddons.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.QuadInstance
import net.minecraft.util.RandomSource
import net.minecraft.core.Direction
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
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

    /** Full brightness: a plan is a hint laid over the world, not a block lit by it. */
    private const val FULL_BRIGHT: Int = 0xF000F0

    private const val NO_OVERLAY: Int = 0xA0000

    private val RANDOM: RandomSource = RandomSource.create(0)

    /** Every side a model files its quads under, the unculled ones included. */
    private val QUAD_SIDES: List<Direction?> = Direction.entries + null

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
        }

        outline(poseStack, collector, cameraPos, pos, shape, color)
    }

    /** The edges of [shape], each of its boxes drawn as its own box. */
    fun outline(
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        cameraPos: Vec3,
        pos: BlockPos,
        shape: VoxelShape,
        color: Int
    ) {
        if (shape.isEmpty) return

        atBlock(poseStack, cameraPos, pos) { pose ->
            // one call per box, so two marked blocks side by side stay two boxes rather than
            // merging into one long one the way a single combined shape would
            shape.toAabbs().forEach { box ->
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
     * Draws [state] at [pos] as it would look if it were there, washed with [tint] and see through,
     * so the player can see which block to put down rather than only that one is missing. The box
     * around it is drawn in [outlineColor].
     *
     * The model's own quads are handed to the game to write, with a colour set straight onto each
     * one. Going through the tint array instead does nothing to ordinary ground: a tint only
     * reaches quads that ask to be tinted, which is how grass and leaves take a biome colour, and
     * dirt never asks. Writing the colour onto the quad reaches every one of them.
     *
     * That colour multiplies the texture rather than replacing it, so [tint] wants to be pale. A
     * saturated one holds the channels it lacks near zero, which drains the block of its own colour
     * instead of washing over it, and a brown block ends up unrecognisably dark.
     */
    fun ghost(
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        cameraPos: Vec3,
        pos: BlockPos,
        state: BlockState,
        tint: Int,
        outlineColor: Int,
        alpha: Int
    ) {
        val parts = mutableListOf<BlockStateModelPart>()

        Minecraft.getInstance().modelManager.blockStateModelSet.get(state)
            .collectParts(RANDOM, parts)

        val quadColor = ARGB.color(alpha, tint)

        if (parts.isNotEmpty()) {
            atBlock(poseStack, cameraPos, pos) { pose ->
                collector.submitCustomGeometry(
                    pose,
                    RenderTypes.translucentMovingBlock()
                ) { transform, consumer ->
                    val quadInstance = QuadInstance()

                    parts.forEach { part ->
                        QUAD_SIDES.forEach { side ->
                            part.getQuads(side).forEach { quad ->
                                quadInstance.setColor(quadColor)
                                quadInstance.setLightCoords(FULL_BRIGHT)
                                quadInstance.setOverlayCoords(NO_OVERLAY)

                                consumer.putBakedQuad(transform, quad, quadInstance)
                            }
                        }
                    }
                }
            }
        }

        // the box around it says this is a plan rather than something already standing there
        val level = Minecraft.getInstance().level ?: return

        outline(poseStack, collector, cameraPos, pos, state.getShape(level, pos), outlineColor)
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
