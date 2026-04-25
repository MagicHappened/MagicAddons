package org.magic.mixins;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.interact.OnAttackEntityEvent;
import org.magic.magicaddons.events.interact.OnStartDestroyBlockEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attack", at = @At("HEAD") , cancellable = true)
    private static void onAttackEntity(Player player, Entity entity, CallbackInfo ci){
        OnAttackEntityEvent event = new OnAttackEntityEvent(player, entity);
        EventBus.post(event);
        if (event.getCanceled()){
            ci.cancel();
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> cir){
        OnStartDestroyBlockEvent event = new OnStartDestroyBlockEvent(blockPos);
        EventBus.post(event);
        if (event.getCanceled()){
            cir.cancel();
        }
    }

    @Inject(method = "destroyBlock", at = @At("TAIL"))
    private void onBreakBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir){
        //todo event
    }

    //todo block place event?


}
