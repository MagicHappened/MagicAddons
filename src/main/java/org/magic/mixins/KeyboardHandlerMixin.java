package org.magic.mixins;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.magic.magicaddons.commands.debug.CropCollector;
import org.magic.magicaddons.ui.screens.CollectScreen;
import org.magic.magicaddons.util.compat.McCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    /**
     * G opens the collector's checklist while a run is live, cancelled so nothing else reads the
     * key. It steps aside whenever a screen is open, and does nothing at all with no run going.
     */
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void magicaddons$openCollectChecklist(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;
        if (event.key() != GLFW.GLFW_KEY_G) return;

        if (McCompat.INSTANCE.currentScreen() != null) return;
        if (!CropCollector.INSTANCE.isActive()) return;

        McCompat.INSTANCE.setScreen(new CollectScreen());
        ci.cancel();
    }
}
