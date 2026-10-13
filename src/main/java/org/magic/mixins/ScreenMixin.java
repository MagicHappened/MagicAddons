package org.magic.mixins;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import org.magic.magicaddons.commands.MainCommand;
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

    @Inject(method = "clickCommandAction", at = @At("HEAD"), cancellable = true)
    private static void onClickCommand(LocalPlayer player, String command, Screen screen, CallbackInfo ci) {
        if (MainCommand.INSTANCE.runClickedInternalCommand(command)) ci.cancel();
    }
}
