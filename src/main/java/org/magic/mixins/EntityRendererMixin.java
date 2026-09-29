package org.magic.mixins;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.magic.magicaddons.features.farming.greenhousePresets.shrunkPlants.ShorterCaneCrops;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void hideShrunkPlantStands(Entity entity, Frustum culler, double cameraX, double cameraY, double cameraZ, CallbackInfoReturnable<Boolean> cir) {
        if (ShorterCaneCrops.INSTANCE.isStandHidden(entity.getId())) cir.setReturnValue(false);
    }
}
