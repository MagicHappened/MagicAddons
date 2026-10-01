package org.magic.mixins;

import net.minecraft.client.DeltaTracker;
//? if >=26.2 {
/*import net.minecraft.client.gui.Hud;
*///?} else {
import net.minecraft.client.gui.Gui;
//?}
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.render.HudRenderEvent;
import org.magic.magicaddons.features.farming.greenhousePresets.render.PlannerSlotHighlight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.2 {
/*@Mixin(Hud.class)
*///?} else {
@Mixin(Gui.class)
//?}
public class GuiMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        EventBus.post(new HudRenderEvent(graphics, deltaTracker));
    }

    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void onExtractHotbarSlot(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack stack, int seed, CallbackInfo ci) {
        PlannerSlotHighlight.fillBehind(graphics, stack, x, y);
    }
}
