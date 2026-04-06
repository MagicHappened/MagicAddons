package org.magic.mixins;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.interact.OnAttackEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD") , cancellable = true)
    private static void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci){
        OnAttackEntityEvent event = new OnAttackEntityEvent(player,target);
        EventBus.post(event);
        if (event.getCanceled()){
            ci.cancel();
        }
    }
}
