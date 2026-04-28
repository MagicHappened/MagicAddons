package org.magic.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.level.block.state.BlockState;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.interact.OnBlockDestroyedEvent;
import org.magic.magicaddons.events.interact.OnBlockPlacedEvent;
import org.magic.magicaddons.events.world.AddParticleEvent;
import org.magic.magicaddons.util.ChatUtils;
import org.magic.misc.BlockEventBufferAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(
            method = "handleParticleEvent(Lnet/minecraft/network/protocol/game/ClientboundLevelParticlesPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        AddParticleEvent event = new AddParticleEvent(packet);
        EventBus.post(event);
        if (event.getCanceled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handleBlockUpdate(Lnet/minecraft/network/protocol/game/ClientboundBlockUpdatePacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {

        ClientLevel level = Minecraft.getInstance().level;
        if (!(level instanceof BlockEventBufferAccess buffer)) return;

        BlockPos pos = packet.getPos();
        BlockState newState = packet.getBlockState();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        BlockState expectedPlaceState = buffer.magicaddons$getPendingPlaces().get(pos);
        BlockState expectedBreakState = buffer.magicaddons$getPendingBreaks().get(pos);
        if (expectedPlaceState != null) {

            buffer.magicaddons$getPendingPlaces().remove(pos);

            if (!newState.isAir() && newState.is(expectedPlaceState.getBlock())) {
                EventBus.post(new OnBlockPlacedEvent(pos, player, newState));
            }

            return;
        }

        if (expectedBreakState != null) {

            buffer.magicaddons$getPendingBreaks().remove(pos);

            if (newState.isAir() || newState != expectedBreakState) {
                EventBus.post(new OnBlockDestroyedEvent(pos, player, newState));
            }
        }
    }



}
