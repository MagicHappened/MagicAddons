package org.magic.mixins;

import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.magic.misc.FakeEntityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandRenderer.class)
public class ArmorStandRendererMixin {

    @Inject(
            method = "getRenderType(Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onGetRenderType(
            ArmorStandRenderState armorStandRenderState,
            boolean bl, boolean bl2, boolean bl3, CallbackInfoReturnable<RenderType> cir){
        if (armorStandRenderState instanceof FakeEntityState state){
            if (state.magicaddons$isFakeEntity()){
                Identifier identifier = ((ArmorStandRenderer)(Object)this).getTextureLocation(armorStandRenderState);
                cir.setReturnValue(RenderTypes.entityTranslucent(identifier));
            }
        }
    }
}
