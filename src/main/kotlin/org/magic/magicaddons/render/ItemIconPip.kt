package org.magic.magicaddons.render

import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.util.LightCoordsUtil
//? if >=26.2 {
/*import net.minecraft.client.renderer.SubmitNodeCollector
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource
//?}
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import org.joml.Matrix3x2f

data class ItemIconRenderState(
    val item: TrackingItemStackRenderState,
    private val bX0: Int,
    private val bY0: Int,
    private val bX1: Int,
    private val bY1: Int,
    private val size: Float,
    private val matrix: Matrix3x2f
) : PictureInPictureRenderState {

    override fun x0(): Int = bX0
    override fun y0(): Int = bY0
    override fun x1(): Int = bX1
    override fun y1(): Int = bY1
    override fun scale(): Float = size
    override fun pose(): Matrix3x2f = matrix
    override fun scissorArea(): ScreenRectangle? = null

    override fun bounds(): ScreenRectangle? =
        ScreenRectangle(bX0, bY0, bX1 - bX0, bY1 - bY0).transformMaxBounds(matrix)
}

//? if >=26.2 {
/*class ItemIconRenderer : PictureInPictureRenderer<ItemIconRenderState>() {
*///?} else {
class ItemIconRenderer(
    bufferSource: MultiBufferSource.BufferSource
) : PictureInPictureRenderer<ItemIconRenderState>(bufferSource) {
//?}

    override fun getRenderStateClass(): Class<ItemIconRenderState> = ItemIconRenderState::class.java

    override fun getTextureLabel(): String = "magicaddons_item_icon"

    private var drawnIdentity: Any? = null
    private var drawnSize: Float = 0f
    private var drawnMatrix: Matrix3x2f? = null

    override fun textureIsReadyToBlit(state: ItemIconRenderState): Boolean =
        drawnIdentity != null &&
                drawnIdentity == state.item.modelIdentity &&
                drawnSize == state.scale() &&
                drawnMatrix == state.pose()

    override fun getTranslateY(height: Int, guiScale: Int): Float = height / 2f

    //? if >=26.2 {
    /*override fun renderToTexture(
        state: ItemIconRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector
    ) {
        val lighting = Minecraft.getInstance().gameRenderer.lighting()
        lighting.setupFor(if (state.item.usesBlockLight()) Lighting.Entry.ITEMS_3D else Lighting.Entry.ITEMS_FLAT)
    *///?} else {
    override fun renderToTexture(
        state: ItemIconRenderState,
        poseStack: PoseStack
    ) {
        val gameRenderer = Minecraft.getInstance().gameRenderer
        gameRenderer.lighting.setupFor(if (state.item.usesBlockLight()) Lighting.Entry.ITEMS_3D else Lighting.Entry.ITEMS_FLAT)

        // 26.1.2's gui hands out no collector here, so the item goes through the game's own submit
        // node storage, as vanilla's oversized items do
        val features = gameRenderer.featureRenderDispatcher
        val collector = features.submitNodeStorage
    //?}

        // the gui's y runs down, so the item is flipped the way vanilla flips its oversized items
        poseStack.scale(1f, -1f, -1f)
        state.item.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0)

        drawnIdentity = state.item.modelIdentity
        drawnSize = state.scale()
        drawnMatrix = Matrix3x2f(state.pose())

        //? if <26.2 {
        features.renderAllFeatures()
        //?}
    }
}
