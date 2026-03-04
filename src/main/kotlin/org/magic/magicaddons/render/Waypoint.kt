package org.magic.magicaddons.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.util.Identifier
import org.magic.magicaddons.Common

object WaypointRenderer {

    val BOX_PIPELINE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Identifier.of(Common.MOD_ID, "pipeline/thick_wireframe"))
            .build()
    )

    val BOX_PIPELINE_NO_DEPTH: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Identifier.of(Common.MOD_ID, "pipeline/thick_wireframe_no_depth"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )
}


/*
object Waypoint {
    fun renderBox(
        context: WorldRenderContext,
        pos1: WorldPosition,
        pos2: WorldPosition,
        color: Int = 0xFF0000FF.toInt(),
        noDepth: Boolean = false
    ) {
        val matrices = context.matrices() ?: return
        val camera = MinecraftClient.getInstance().gameRenderer.camera
        val camX = camera.pos.x.toFloat()
        val camY = camera.pos.y.toFloat()
        val camZ = camera.pos.z.toFloat()

        matrices.push()
        matrices.translate(-camX.toDouble(), -camY.toDouble(), -camZ.toDouble())

        val matrix = matrices.peek().positionMatrix

        val pipeline = if (noDepth) WaypointRenderer.BOX_PIPELINE_NO_DEPTH else WaypointRenderer.BOX_PIPELINE
        val buffer: BufferBuilder =

        val minX = minOf(pos1.x, pos2.x)
        val minY = minOf(pos1.y, pos2.y)
        val minZ = minOf(pos1.z, pos2.z)

        val maxX = maxOf(pos1.x, pos2.x) + 1f
        val maxY = maxOf(pos1.y, pos2.y) + 1f
        val maxZ = maxOf(pos1.z, pos2.z) + 1f

        fun line(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float) {
            buffer.vertex(matrix, x1, y1, z1)
                .color(color)
                .next()
            buffer.vertex(matrix, x2, y2, z2)
                .color(color)
                .next()
        }

        // Bottom face
        line(minX, minY, minZ, maxX, minY, minZ)
        line(maxX, minY, minZ, maxX, minY, maxZ)
        line(maxX, minY, maxZ, minX, minY, maxZ)
        line(minX, minY, maxZ, minX, minY, minZ)

        // Top face
        line(minX, maxY, minZ, maxX, maxY, minZ)
        line(maxX, maxY, minZ, maxX, maxY, maxZ)
        line(maxX, maxY, maxZ, minX, maxY, maxZ)
        line(minX, maxY, maxZ, minX, maxY, minZ)

        // Vertical edges
        line(minX, minY, minZ, minX, maxY, minZ)
        line(maxX, minY, minZ, maxX, maxY, minZ)
        line(maxX, minY, maxZ, maxX, maxY, maxZ)
        line(minX, minY, maxZ, minX, maxY, maxZ)


        matrices.pop()
    }







}
*/