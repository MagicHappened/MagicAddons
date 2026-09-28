package org.magic.magicaddons.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.render.pip.GuiEntityRenderer
import net.minecraft.client.renderer.state.gui.GuiRenderState
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState
//? if >=26.2 {
/*import net.minecraft.client.renderer.SubmitNodeCollector
*///?}

//? if >=26.2 {
/*class BakedEntityIcon : GuiEntityRenderer(Minecraft.getInstance().entityRenderDispatcher) {
*///?} else {
class BakedEntityIcon : GuiEntityRenderer(
    Minecraft.getInstance().renderBuffers().bufferSource(),
    Minecraft.getInstance().entityRenderDispatcher
) {
//?}

    var isBaked: Boolean = false
        private set

    override fun textureIsReadyToBlit(state: GuiEntityRenderState): Boolean = isBaked

    //? if >=26.2 {
    /*override fun renderToTexture(
        state: GuiEntityRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector
    ) {
        super.renderToTexture(state, poseStack, collector)
    *///?} else {
    override fun renderToTexture(state: GuiEntityRenderState, poseStack: PoseStack) {
        super.renderToTexture(state, poseStack)
    //?}
        isBaked = true
    }

    fun submit(state: GuiEntityRenderState, guiState: GuiRenderState) {
        val minecraft = Minecraft.getInstance()

        //? if >=26.2 {
        /*prepare(state, guiState, minecraft.gameRenderer.featureRenderDispatcher(), minecraft.window.guiScale)
        *///?} else {
        prepare(state, guiState, minecraft.window.guiScale)
        //?}
    }
}
