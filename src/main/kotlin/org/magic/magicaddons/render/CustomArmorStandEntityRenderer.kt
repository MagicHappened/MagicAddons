package org.magic.magicaddons.render

import net.minecraft.client.renderer.entity.ArmorStandRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes


class CustomArmorStandEntityRenderer(context: EntityRendererProvider.Context) : ArmorStandRenderer(context) {

    override fun getRenderType(
        armorStandRenderState: ArmorStandRenderState,
        bl: Boolean,
        bl2: Boolean,
        bl3: Boolean
    ): RenderType {
        val identifier = this.getTextureLocation(armorStandRenderState)
        return RenderTypes.entityTranslucent(identifier, false)
    }

    override fun getModelTint(state: ArmorStandRenderState): Int {
        return 0x10FF0000
    }
}