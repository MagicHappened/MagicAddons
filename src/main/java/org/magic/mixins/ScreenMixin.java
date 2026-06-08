package org.magic.mixins;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.magic.magicaddons.events.EventBus;
import org.magic.magicaddons.events.ui.InventoryClosedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoveScreen(CallbackInfo ci) {
        Screen self = (Screen)(Object)this;
        if (!(self instanceof InventoryScreen)) return;

        InventoryClosedEvent event = new InventoryClosedEvent();
        EventBus.post(event);
    }
}
