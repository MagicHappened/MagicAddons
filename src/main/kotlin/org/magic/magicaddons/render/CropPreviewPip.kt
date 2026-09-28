package org.magic.magicaddons.render

import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
//? if >=26.2 {
/*import net.minecraft.client.renderer.SubmitNodeCollector
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource
//?}
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf

data class StandInScene(
    val state: EntityRenderState,
    val x: Double,
    val y: Double,
    val z: Double
)

data class CropPreviewRenderState(
    val blocks: Map<BlockPos, BlockState>,
    val stands: List<StandInScene>,
    val sceneCenter: Vec3,
    val yawDeg: Float,
    val pitchDeg: Float,
    private val bX0: Int,
    private val bY0: Int,
    private val bX1: Int,
    private val bY1: Int,
    private val pixelsPerBlock: Float,
    private val scissor: ScreenRectangle?
) : PictureInPictureRenderState {

    override fun x0(): Int = bX0
    override fun y0(): Int = bY0
    override fun x1(): Int = bX1
    override fun y1(): Int = bY1
    override fun scale(): Float = pixelsPerBlock
    override fun scissorArea(): ScreenRectangle? = scissor

    override fun bounds(): ScreenRectangle? =
        PictureInPictureRenderState.getBounds(bX0, bY0, bX1, bY1, scissor)
}

//? if >=26.2 {
/*class CropPreviewRenderer : PictureInPictureRenderer<CropPreviewRenderState>() {
*///?} else {
class CropPreviewRenderer(
    bufferSource: MultiBufferSource.BufferSource
) : PictureInPictureRenderer<CropPreviewRenderState>(bufferSource) {
//?}

    override fun getRenderStateClass(): Class<CropPreviewRenderState> =
        CropPreviewRenderState::class.java

    override fun getTextureLabel(): String = "magicaddons_crop_preview"

    override fun getTranslateY(height: Int, guiScale: Int): Float = height / 2f

    //? if >=26.2 {
    /*override fun renderToTexture(
        state: CropPreviewRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector
    ) {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI)
    *///?} else {
    override fun renderToTexture(
        state: CropPreviewRenderState,
        poseStack: PoseStack
    ) {
        val gameRenderer = Minecraft.getInstance().gameRenderer

        gameRenderer.lighting.setupFor(Lighting.Entry.ENTITY_IN_UI)

        // 26.1.2's gui hands out no collector here, so the scene goes through the game's own submit
        // node storage, as vanilla's entity preview does
        val features = gameRenderer.featureRenderDispatcher
        val collector = features.submitNodeStorage
    //?}

        poseStack.mulPose(
            Quaternionf()
                .rotationZ(Math.PI.toFloat())
                .rotateX(Math.toRadians(state.pitchDeg.toDouble()).toFloat())
                .rotateY(Math.toRadians(state.yawDeg.toDouble()).toFloat())
        )

        state.blocks.forEach { (pos, blockState) ->
            WorldRenderer.submitSolidBlock(poseStack, collector, state.sceneCenter, pos, blockState)
        }

        val dispatcher = Minecraft.getInstance().entityRenderDispatcher
        val camera = CameraRenderState()

        state.stands.forEach { stand ->
            dispatcher.submit(stand.state, camera, stand.x, stand.y, stand.z, poseStack, collector)
        }

        //? if <26.2 {
        features.renderAllFeatures()
        //?}
    }
}
