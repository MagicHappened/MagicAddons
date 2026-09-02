package org.magic.magicaddons.util.compat

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.util.Mth
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * The one piece of drawing the two versions disagree about: outlining a shape.
 *
 * 26.2 asks the collector for it and hands over a line width. 26.1.2 has no such call, so the edges
 * are walked and written out here instead, which is what vanilla's own shape renderer does; the
 * line is then whatever width the render type carries, since there is nowhere to ask for one.
 */
object RenderCompat {

    //? if >=26.2 {
    /*/** How thick a mark's edges are drawn, where the version lets that be asked for. */
    private const val OUTLINE_WIDTH: Float = 3f
    *///?}

    /** Draws the edges of [shape], already positioned by [poseStack], in [color]. */
    fun outline(
        collector: SubmitNodeCollector,
        poseStack: PoseStack,
        shape: VoxelShape,
        color: Int
    ) {
        //? if >=26.2 {
        /*collector.submitShapeOutline(poseStack, shape, RenderTypes.LINES, color, OUTLINE_WIDTH, false)
        *///?} else {
        collector.submitCustomGeometry(poseStack, RenderTypes.LINES) { transform, consumer ->
            consumer.edges(transform, shape, color)
        }
        //?}
    }

    //? if <26.2 {
    /** A line per edge, the line's own direction as its normal, as vanilla writes them. */
    private fun VertexConsumer.edges(pose: PoseStack.Pose, shape: VoxelShape, color: Int) {
        shape.forAllEdges { x0, y0, z0, x1, y1, z1 ->
            var nx = (x1 - x0).toFloat()
            var ny = (y1 - y0).toFloat()
            var nz = (z1 - z0).toFloat()

            val length = Mth.sqrt(nx * nx + ny * ny + nz * nz)
            if (length > 0f) {
                nx /= length
                ny /= length
                nz /= length
            }

            this.addVertex(pose, x0.toFloat(), y0.toFloat(), z0.toFloat())
                .setColor(color)
                .setNormal(pose, nx, ny, nz)
            this.addVertex(pose, x1.toFloat(), y1.toFloat(), z1.toFloat())
                .setColor(color)
                .setNormal(pose, nx, ny, nz)
        }
    }
    //?}
}
