package org.magic.mixins;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.magic.magicaddons.util.TablistUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onPlayerList", at = @At("TAIL"))
    private void onPlayerListUpdate(PlayerListS2CPacket packet, CallbackInfo ci) {
        TablistUtils.markTabListDirty();
    }

    @Inject(method = "onGameJoin", at = @At("HEAD"))
    private void onGameJoin(CallbackInfo ci) {
        TablistUtils.markTabListDirty();
    }
}
