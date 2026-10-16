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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.magic.magicaddons.features.misc.SkipResourceReloadOnClose;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        EventBus.post(new WorldTickEvent());
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void onLevelReplaced(ClientLevel level, CallbackInfo ci) {
        EventBus.post(new LevelUnloadingEvent());
    }

    @Inject(method = "clearClientLevel", at = @At("HEAD"))
    private void onLevelCleared(Screen screen, CallbackInfo ci) {
        EventBus.post(new LevelUnloadingEvent());
    }

    @Inject(method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"), cancellable = true)
    private void skipReloadWhileClosing(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        if (SkipResourceReloadOnClose.isEnabled() && !((Minecraft) (Object) this).isRunning()) {
            cir.setReturnValue(CompletableFuture.completedFuture(null));
            SkipResourceReloadOnClose.logPreventedReload();
        }
    }
}
