package org.magic.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.level.block.state.BlockState;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.chat.SystemChatEvent;
import org.magic.magicaddons.events.interact.BlockDestroyedEvent;
import org.magic.magicaddons.events.interact.BlockPlacedEvent;
import org.magic.magicaddons.events.interact.BlockChangedEvent;
import org.magic.magicaddons.events.world.AddParticleEvent;
import org.magic.magicaddons.events.world.SetTimePacketEvent;
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
            method = "handleSetTime",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onSetTime(ClientboundSetTimePacket packet, CallbackInfo ci){
        SetTimePacketEvent event = new SetTimePacketEvent(packet);
        EventBus.post(event);
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
        if (!(level instanceof BlockEventBufferAccess blockEventBuffer)) return;

        BlockPos pos = packet.getPos();
        BlockState newState = packet.getBlockState();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        BlockState expectedPlaceState = blockEventBuffer.magicaddons$getPendingPlaces().get(pos);
        BlockState expectedBreakState = blockEventBuffer.magicaddons$getPendingBreaks().get(pos);
        if (expectedPlaceState != null) {

            blockEventBuffer.magicaddons$getPendingPlaces().remove(pos);

            if (!newState.isAir() && newState.is(expectedPlaceState.getBlock())) {
                EventBus.post(new BlockPlacedEvent(pos, player, newState));
            }
            return;
        }

        if (expectedBreakState != null) {

            blockEventBuffer.magicaddons$getPendingBreaks().remove(pos);

            if (newState.isAir() || newState != expectedBreakState) {
                EventBus.post(new BlockDestroyedEvent(pos, player, newState));
            }
            return;
        }
        BlockState currentState = level.getBlockState(pos);
        if (currentState.equals(packet.getBlockState())) return;
        EventBus.post(new BlockChangedEvent(packet));
    }

    @Inject(
            method = "handleSystemChat(Lnet/minecraft/network/protocol/game/ClientboundSystemChatPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        EventBus.post(new SystemChatEvent(packet.content(), packet.overlay()));
    }

}
