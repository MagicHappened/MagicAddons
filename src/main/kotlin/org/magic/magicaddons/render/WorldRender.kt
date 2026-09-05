package org.magic.magicaddons.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.QuadInstance
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.core.Direction
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.Minecraft
import org.magic.magicaddons.util.compat.RenderCompat
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
 * Draws single blocks into the world, to show a player what a plot should look like. The camera
 * comes from the frame being drawn, or the marks lag behind the player as they walk.
 */
object WorldRender {


    /** Full brightness: a plan is a hint laid over the world, not a block lit by it. */
    private const val FULL_BRIGHT: Int = 0xF000F0

    private const val NO_OVERLAY: Int = 0xA0000

    private val RANDOM: RandomSource = RandomSource.create(0)

    /** Every side a model files its quads under, the unculled ones included. */
    private val QUAD_SIDES: List<Direction?> = Direction.entries + null

    /** Pulls an outlined box off its faces, so two marked blocks side by side stay two boxes. */
    private const val OUTLINE_INSET: Double = 0.012

    /** Pushes a filled box past the block it covers, or it fights the block's own faces for depth. */
    private const val FILL_EXPAND: Double = 0.002

    /** The least a box can measure and still be seen, for entities that occupy nothing at all. */
    private const val MIN_BOX: Double = 0.08

    /**
     * Marks whatever stands at a position: its shape filled, its edges drawn on top. Neither is depth
     * tested, so a block behind another still reads.
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

    /**
     * Marks a world-space box the way mark marks a block, for entities. Anything too thin to see is
     * opened out to the least that can be.
     */
    fun markBox(
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        cameraPos: Vec3,
        box: AABB,
        color: Int,
        fillAlpha: Int
    ) {
        val visible = if (box.xsize < MIN_BOX || box.ysize < MIN_BOX || box.zsize < MIN_BOX) {
            box.grow(MIN_BOX / 2)
        } else {
            box
        }

        poseStack.pushPose()
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z)

        try {
            collector.submitCustomGeometry(poseStack, RenderTypes.debugFilledBox()) { transform, consumer ->
                consumer.fillBox(transform, visible.grow(FILL_EXPAND), ARGB.color(fillAlpha, color))
            }

            RenderCompat.outline(collector, poseStack, Shapes.create(visible), color)
        } finally {
            poseStack.popPose()
        }
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
                RenderCompat.outline(
                    collector,
                    pose,
                    Shapes.create(box.grow(-OUTLINE_INSET)),
                    color
                )
            }
        }
    }

    /**
     * Draws a block as it would look if it were there, tinted and see through. The colour is written
     * onto each quad, since the tint array only reaches quads that ask for it, and it multiplies the texture.
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

        // seeded from the block's own position, as the chunk renderer seeds its own: a shared
        // generator picked a different fire variant every frame, which flickers
        RANDOM.setSeed(Mth.getSeed(pos))

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

    /** Draws a block exactly as it is: full colour, no outline. What the crop preview is made of. */
    fun solid(
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        cameraPos: Vec3,
        pos: BlockPos,
        state: BlockState
    ) {
        val parts = mutableListOf<BlockStateModelPart>()

        RANDOM.setSeed(Mth.getSeed(pos))

        Minecraft.getInstance().modelManager.blockStateModelSet.get(state)
            .collectParts(RANDOM, parts)

        if (parts.isEmpty()) return

        // a stem is yellow and grass is green because of tints the model does not carry itself;
        // without asking for them here every tinted block came out white
        val colors = Minecraft.getInstance().blockColors
        val level = Minecraft.getInstance().level
        val tintColors = mutableMapOf<Int, Int>()

        atBlock(poseStack, cameraPos, pos) { pose ->
            collector.submitCustomGeometry(
                pose,
                RenderTypes.translucentMovingBlock()
            ) { transform, consumer ->
                val quadInstance = QuadInstance()

                parts.forEach { part ->
                    QUAD_SIDES.forEach { side ->
                        part.getQuads(side).forEach { quad ->
                            val material = quad.materialInfo()

                            val color = if (material.isTinted) {
                                tintColors.getOrPut(material.tintIndex()) {
                                    val source = colors.getTintSource(state, material.tintIndex())
                                    val rgb = when {
                                        source == null -> 0xFFFFFF
                                        level != null -> source.colorInWorld(state, level, pos)
                                        else -> source.color(state)
                                    }

                                    ARGB.color(0xFF, rgb)
                                }
                            } else {
                                -1
                            }

                            quadInstance.setColor(color)
                            quadInstance.setLightCoords(FULL_BRIGHT)
                            quadInstance.setOverlayCoords(NO_OVERLAY)

                            consumer.putBakedQuad(transform, quad, quadInstance)
                        }
                    }
                }
            }
        }
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

    /** The six faces of a box, wound both ways so the face survives whichever winding is culled. */
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


    /**
     * Everything a plan draws in one frame, handed to the collector as one batch a render type.
     * A batch a block made the buffer source flush and rebind its target on every switch between
     * boxes, lines and blocks, which cost more than the drawing itself.
     */
    class Batch(private val cameraPos: Vec3) {
        private class Fill(val pos: BlockPos, val boxes: List<AABB>, val color: Int)
        private class Ghost(val pos: BlockPos, val state: BlockState, val color: Int)
        private class Outline(val pos: BlockPos, val shape: VoxelShape, val color: Int)

        private val fills = mutableListOf<Fill>()
        private val ghosts = mutableListOf<Ghost>()
        private val outlines = mutableListOf<Outline>()

        /** Marks whatever stands at a position: its shape filled, its edges drawn on top. */
        fun mark(pos: BlockPos, shape: VoxelShape, color: Int, fillAlpha: Int) {
            if (shape.isEmpty) return
            fills.add(Fill(pos, shape.toAabbs(), ARGB.color(fillAlpha, color)))
            outline(pos, shape, color)
        }

        fun outline(pos: BlockPos, shape: VoxelShape, color: Int) {
            if (shape.isEmpty) return
            outlines.add(Outline(pos, shape, color))
        }

        /** A block as it would look if it were there, tinted and see through, boxed as a plan. */
        fun ghost(pos: BlockPos, state: BlockState, tint: Int, outlineColor: Int, alpha: Int) {
            ghosts.add(Ghost(pos, state, ARGB.color(alpha, tint)))
            val level = Minecraft.getInstance().level ?: return
            outline(pos, state.getShape(level, pos), outlineColor)
        }

        fun submit(poseStack: PoseStack, collector: SubmitNodeCollector) {
            if (fills.isEmpty() && ghosts.isEmpty() && outlines.isEmpty()) return

            poseStack.pushPose()
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z)

            try {
                if (fills.isNotEmpty()) {
                    val batch = fills.toList()
                    collector.submitCustomGeometry(poseStack, RenderTypes.debugFilledBox()) { transform, consumer ->
                        batch.forEach { fill ->
                            fill.boxes.forEach { box ->
                                consumer.fillBox(transform, box.move(fill.pos.x.toDouble(), fill.pos.y.toDouble(), fill.pos.z.toDouble()).grow(FILL_EXPAND), fill.color)
                            }
                        }
                    }
                }

                if (ghosts.isNotEmpty()) {
                    val batch = ghosts.toList()
                    collector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock()) { transform, consumer ->
                        val stack = PoseStack()
                        stack.mulPose(transform.pose())
                        val quadInstance = QuadInstance()
                        val parts = mutableListOf<BlockStateModelPart>()
                        val colors = Minecraft.getInstance().blockColors
                        val level = Minecraft.getInstance().level

                        batch.forEach { ghost ->
                            parts.clear()
                            RANDOM.setSeed(Mth.getSeed(ghost.pos))
                            Minecraft.getInstance().modelManager.blockStateModelSet.get(ghost.state).collectParts(RANDOM, parts)
                            if (parts.isEmpty()) return@forEach

                            stack.pushPose()
                            stack.translate(ghost.pos.x.toDouble(), ghost.pos.y.toDouble(), ghost.pos.z.toDouble())
                            val pose = stack.last()
                            parts.forEach { part ->
                                QUAD_SIDES.forEach { side ->
                                    part.getQuads(side).forEach { quad ->
                                        // a stem is yellow and grass green only through a tint the model
                                        // does not carry itself; without it every tinted block came out white
                                        val material = quad.materialInfo()
                                        val color = if (material.isTinted) {
                                            val source = colors.getTintSource(ghost.state, material.tintIndex())
                                            val rgb = when {
                                                source == null -> 0xFFFFFF
                                                level != null -> source.colorInWorld(ghost.state, level, ghost.pos)
                                                else -> source.color(ghost.state)
                                            }
                                            ARGB.multiply(ghost.color, ARGB.color(0xFF, rgb))
                                        } else {
                                            ghost.color
                                        }
                                        quadInstance.setColor(color)
                                        quadInstance.setLightCoords(FULL_BRIGHT)
                                        quadInstance.setOverlayCoords(NO_OVERLAY)
                                        consumer.putBakedQuad(pose, quad, quadInstance)
                                    }
                                }
                            }
                            stack.popPose()
                        }
                    }
                }

                if (outlines.isNotEmpty()) {
                    // one call per box, so two marked blocks side by side stay two boxes
                    val edges = outlines.flatMap { outline ->
                        outline.shape.toAabbs().map { box ->
                            RenderCompat.OutlineItem(
                                Vec3(outline.pos.x.toDouble(), outline.pos.y.toDouble(), outline.pos.z.toDouble()),
                                Shapes.create(box.grow(-OUTLINE_INSET)),
                                outline.color
                            )
                        }
                    }
                    RenderCompat.outlineAll(collector, poseStack, edges)
                }
            } finally {
                poseStack.popPose()
            }
        }
    }
}
