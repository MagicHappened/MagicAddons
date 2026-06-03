package org.magic.mixins;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.magic.misc.FakeEntityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "getModelTint", at = @At("HEAD"), cancellable = true)
    private void onGetModelTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir){
        if (state instanceof FakeEntityState fakeState){
            if (fakeState.magicaddons$isFakeEntity()){
                cir.setReturnValue(0x40FF0000);
            }
        }
    }

}
