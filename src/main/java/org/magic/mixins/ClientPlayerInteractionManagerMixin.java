package org.magic.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.interact.*;
import org.magic.magicaddons.util.ChatUtils;
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

    @Inject(method = "useItem", at = @At("TAIL"))
    private void onUseItem(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir){
        OnUseEvent event = new OnUseEvent(player);
        EventBus.post(event);
        // possibly add head hook and cancellation?
        //todo watering can detection.
    }

    @Inject(method = "useItemOn", at = @At("TAIL"))
    private void onUseItemOn(
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        OnBlockUseEvent event = new OnBlockUseEvent(player,hit, cir.getReturnValue());
        EventBus.post(event);
    }
    //todo just for fire since its placing skulls
    //todo also for using hoe on dirt + if watering can needs

    //todo block place event?


}
