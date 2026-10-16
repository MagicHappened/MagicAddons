package org.magic.mixins;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import org.magic.magicaddons.features.farming.greenhousePresets.shrunkPlants.ShorterCaneCrops;
import org.magic.magicaddons.features.mining.XpOrbHider;
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

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void hideXpOrbs(Entity entity, Frustum culler, double cameraX, double cameraY, double cameraZ, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ExperienceOrb && XpOrbHider.INSTANCE.shouldHideXpOrbs()) cir.setReturnValue(false);
    }
}
