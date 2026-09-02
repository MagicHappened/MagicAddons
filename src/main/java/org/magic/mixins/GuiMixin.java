package org.magic.mixins;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.render.OnHudRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        EventBus.post(new OnHudRenderEvent(graphics, deltaTracker));
    }
}
