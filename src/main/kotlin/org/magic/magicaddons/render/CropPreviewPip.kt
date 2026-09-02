package org.magic.magicaddons.render

import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf

/** One armor stand of a previewed plant, extracted for drawing, and where it sits in the scene. */
data class StandInScene(
    val state: EntityRenderState,
    val x: Double,
    val y: Double,
    val z: Double
)

/**
 * A whole plant handed to the gui to draw in three dimensions: its blocks, its stands, and how the
 * viewer has it turned.
 *
 * The gui pipeline draws each of these into a texture of its own with a real depth buffer, which
 * is what lets a head hide behind the cane it belongs to; drawing the parts as separate gui
 * elements could never interleave them.
 */
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

/**
 * Draws a [CropPreviewRenderState] the way the gui draws the player in the inventory: into its own
 * texture, submitted by the crop preview screen each frame.
 *
 * The scene is built from the same pieces the in-world holograms use: blocks go through
 * [WorldRender.solid] and stands through the entity render dispatcher, both against the scene's
 * centre so the plant turns about its own middle.
 */
class CropPreviewRenderer(
    bufferSource: MultiBufferSource.BufferSource
) : PictureInPictureRenderer<CropPreviewRenderState>(bufferSource) {

    override fun getRenderStateClass(): Class<CropPreviewRenderState> =
        CropPreviewRenderState::class.java

    override fun getTextureLabel(): String = "magicaddons_crop_preview"

    /**
     * The base class parks the origin at the texture's bottom edge, which is right for an entity
     * whose origin is its feet and wrong for a scene whose offsets are taken from its centre; the
     * plant showed up half-sunk through the bottom of its box.
     */
    override fun getTranslateY(height: Int, guiScale: Int): Float = height / 2f

    override fun renderToTexture(
        state: CropPreviewRenderState,
        poseStack: PoseStack
    ) {
        val gameRenderer = Minecraft.getInstance().gameRenderer

        gameRenderer.lighting.setupFor(Lighting.Entry.ENTITY_IN_UI)

        // the gui has no collector of its own to hand out here, so the scene goes through the
        // game's own submit node storage and is drawn at the end of this method, which is how
        // vanilla's entity preview does it
        val features = gameRenderer.featureRenderDispatcher
        val collector = features.submitNodeStorage

        // the gui's y runs down, so the scene is flipped the way the inventory flips its player,
        // then tilted and turned by however the viewer has dragged it
        poseStack.mulPose(
            Quaternionf()
                .rotationZ(Math.PI.toFloat())
                .rotateX(Math.toRadians(state.pitchDeg.toDouble()).toFloat())
                .rotateY(Math.toRadians(state.yawDeg.toDouble()).toFloat())
        )

        state.blocks.forEach { (pos, blockState) ->
            WorldRender.solid(poseStack, collector, state.sceneCenter, pos, blockState)
        }

        val dispatcher = Minecraft.getInstance().entityRenderDispatcher
        val camera = CameraRenderState()

        state.stands.forEach { stand ->
            dispatcher.submit(stand.state, camera, stand.x, stand.y, stand.z, poseStack, collector)
        }

        features.renderAllFeatures()
    }
}
