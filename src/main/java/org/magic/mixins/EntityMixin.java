package org.magic.mixins;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void isGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;

        // cir.setReturnValue(true); set glowing true


    }
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void getTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        // color option for glowing
    }
}
