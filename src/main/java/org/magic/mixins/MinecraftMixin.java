package org.magic.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.world.LevelUnloadingEvent;
import org.magic.magicaddons.events.world.WorldTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        EventBus.post(new WorldTickEvent());
    }

    /** Every level swap comes through here: a respawn, a server switch, a login. The old level is torn down after. */
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void onLevelReplaced(ClientLevel level, CallbackInfo ci) {
        EventBus.post(new LevelUnloadingEvent());
    }

    /** Every disconnect comes through here, closing the game included. */
    @Inject(method = "clearClientLevel", at = @At("HEAD"))
    private void onLevelCleared(Screen screen, CallbackInfo ci) {
        EventBus.post(new LevelUnloadingEvent());
    }
}
