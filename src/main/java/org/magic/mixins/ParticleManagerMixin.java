package org.magic.mixins;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.world.AddParticleEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    @Inject(
            method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void addParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        AddParticleEvent event = new AddParticleEvent(parameters, x, y, z, velocityX, velocityY, velocityZ);
        EventBus.post(event);
        if (event.getCanceled()) {
            cir.cancel();
        }
    }
}