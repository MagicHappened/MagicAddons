package org.magic.mixins;

import net.minecraft.client.Minecraft;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.world.OnWorldTickEvent;
import org.magic.magicaddons.util.EntityUtils;
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
}
