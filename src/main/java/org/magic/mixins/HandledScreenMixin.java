package org.magic.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.magic.magicaddons.features.FeatureManager;
import org.magic.magicaddons.features.api.SlotRenderable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    @Inject(
            method = "drawSlot",
            at = @At("TAIL")
    )
    private void afterDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        for (SlotRenderable feature : FeatureManager.INSTANCE.getSlotRenderables()) {
            feature.onSlotRender(context, slot, (HandledScreen<?>) (Object) this);
        }

    }

}
