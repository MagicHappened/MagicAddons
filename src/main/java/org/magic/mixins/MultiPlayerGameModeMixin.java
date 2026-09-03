package org.magic.mixins;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.interact.OnAttackEntityEvent;
import org.magic.magicaddons.events.interact.OnInteractEntityEvent;
import org.magic.magicaddons.events.world.OnStartDestroyBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {


    @Inject(method = "attack", at = @At("HEAD") , cancellable = true)
    private static void onAttackEntity(Player player, Entity entity, CallbackInfo ci){
        OnAttackEntityEvent event = new OnAttackEntityEvent(player, entity);
        EventBus.post(event);
        if (event.getCanceled()){
            ci.cancel();
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void onInteractEntity(Player player, Entity entity, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir){
        OnInteractEntityEvent event = new OnInteractEntityEvent(player, entity, hand);
        EventBus.post(event);
        if (event.getCanceled()){
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private static void onStartDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir){
        OnStartDestroyBlock event =  new OnStartDestroyBlock(pos, direction);
        EventBus.post(event);
        if (event.getCanceled()){
            cir.cancel();
        }
    }



}
