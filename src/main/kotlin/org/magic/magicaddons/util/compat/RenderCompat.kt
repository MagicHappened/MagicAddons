package org.magic.magicaddons.util.compat

import net.minecraft.world.phys.Vec3
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.util.Mth
import net.minecraft.world.phys.shapes.VoxelShape

object RenderCompat {

    private const val OUTLINE_WIDTH: Float = 3f

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

    class OutlineItem(val offset: Vec3, val shape: VoxelShape, val color: Int)

    fun outlineAll(collector: SubmitNodeCollector, poseStack: PoseStack, items: List<OutlineItem>) {
        //? if >=26.2 {
        /*items.forEach { item ->
            poseStack.pushPose()
            poseStack.translate(item.offset.x, item.offset.y, item.offset.z)
            collector.submitShapeOutline(poseStack, item.shape, RenderTypes.LINES, item.color, OUTLINE_WIDTH, false)
            poseStack.popPose()
        }
        *///?} else {
        collector.submitCustomGeometry(poseStack, RenderTypes.LINES) { transform, consumer ->
            items.forEach { item ->
                consumer.edges(transform, item.shape.move(item.offset.x, item.offset.y, item.offset.z), item.color)
            }
        }
        //?}
    }

    //? if <26.2 {
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
                .setLineWidth(OUTLINE_WIDTH)
            this.addVertex(pose, x1.toFloat(), y1.toFloat(), z1.toFloat())
                .setColor(color)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(OUTLINE_WIDTH)
        }
    }
    //?}
}
