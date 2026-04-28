package org.magic.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.world.OnWorldTickEvent;
import org.magic.misc.BlockEventBufferAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        EventBus.post(new OnWorldTickEvent());
    }
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void onSetLevel(ClientLevel level, CallbackInfo ci) {

        if (Minecraft.getInstance().level instanceof BlockEventBufferAccess buffer) {
            buffer.magicaddons$getPendingBreaks().clear();
            buffer.magicaddons$getPendingPlaces().clear();
        }
    }
}
