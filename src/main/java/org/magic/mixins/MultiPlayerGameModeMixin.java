package org.magic.mixins;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.interact.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Inject(method = "attack", at = @At("HEAD") , cancellable = true)
    private static void onAttackEntity(Player player, Entity entity, CallbackInfo ci){
        AttackEntityEvent event = new AttackEntityEvent(player, entity);
        EventBus.post(event);
        if (event.getCanceled()){
            ci.cancel();
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"))
    private void onUseItem(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir){
        UseEvent event = new UseEvent(player);
        EventBus.post(event);
    }

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void onUseItemOn(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        BlockUseEvent event = new BlockUseEvent(player,hit);
        EventBus.post(event);
    }

    @Inject(
            method = "interact",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onInteractAt(Player player, Entity entity, EntityHitResult entityHitResult, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir){
        InteractEntityEvent event = new InteractEntityEvent(player, entity, interactionHand);
        EventBus.post(event);
    }
}
