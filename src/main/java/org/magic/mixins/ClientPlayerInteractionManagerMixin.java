package org.magic.mixins;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.interact.OnAttackEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {



    @Inject(method = "attack", at = @At("HEAD") , cancellable = true)
    private static void onAttackEntity(Player player, Entity entity, CallbackInfo ci){
        OnAttackEntityEvent event = new OnAttackEntityEvent(player, entity);
        EventBus.post(event);
        if (event.getCanceled()){
            ci.cancel();
        }
    }
}
