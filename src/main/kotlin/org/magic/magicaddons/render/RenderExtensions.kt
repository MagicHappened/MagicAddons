package org.magic.magicaddons.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.block.BlockRenderDispatcher
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.BlockStateModel
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.state.BlockState
import tech.thatgravyboat.skyblockapi.api.events.render.RenderWorldEvent

object RenderExtensions {

    fun RenderWorldEvent.renderSingleBlock(
        blockRenderer: BlockRenderDispatcher,
        blockPos: BlockPos,
        blockState: BlockState,
        overlay: Int,
        alpha: Float
    ){
        val pose = PoseStack()
        pose.pushPose()
        val camPos = this.cameraPosition
        pose.translate(
            blockPos.x - camPos.x,
            blockPos.y - camPos.y,
            blockPos.z - camPos.z
        )
        val model = blockRenderer.getBlockModel(blockState)
        val renderType = Sheets.translucentBlockItemSheet()
        val buffer = this.buffer.getBuffer(renderType)
        val blockColors = Minecraft.getInstance().blockColors
        val color = blockColors.getColor(blockState, null, blockPos, 0)

        val r = (color shr 16 and 255) / 255f
        val g = (color shr 8 and 255) / 255f
        val b = (color and 255) / 255f
        renderBlockModelWithAlpha(
            pose.last(),
            buffer,
            model,
            r,g,b,
            LightTexture.FULL_BRIGHT,
            overlay,
            alpha
        )
        pose.popPose()
    }



    fun renderBlockModelWithAlpha(
        pose: PoseStack.Pose,
        vertexConsumer: VertexConsumer,
        blockStateModel: BlockStateModel,
        f: Float,
        g: Float,
        h: Float,
        i: Int,
        j: Int,
        alpha: Float
    ) {
        for (blockModelPart in blockStateModel.collectParts(RandomSource.create(42L))) {
            for (direction in Direction.entries) {
                renderQuadListWithAlpha(pose, vertexConsumer, f, g, h, blockModelPart.getQuads(direction), i, j, alpha)
            }

            renderQuadListWithAlpha(pose, vertexConsumer, f, g, h, blockModelPart.getQuads(null as Direction?), i, j, alpha)
        }
    }

    private fun renderQuadListWithAlpha(
        pose: PoseStack.Pose,
        vertexConsumer: VertexConsumer,
        f: Float,
        g: Float,
        h: Float,
        list: MutableList<BakedQuad>,
        i: Int,
        j: Int,
        alpha: Float
    ) {
        for (bakedQuad in list) {
            val k: Float
            val l: Float
            val m: Float
            if (bakedQuad.isTinted) {
                k = Mth.clamp(f, 0.0f, 1.0f)
                l = Mth.clamp(g, 0.0f, 1.0f)
                m = Mth.clamp(h, 0.0f, 1.0f)
            } else {
                k = 1.0f
                l = 1.0f
                m = 1.0f
            }

            vertexConsumer.putBulkData(pose, bakedQuad, k, l, m, alpha, i, j)
        }
    }
}