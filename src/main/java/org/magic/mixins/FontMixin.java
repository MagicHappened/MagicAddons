package org.magic.mixins;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FontDescription;
import org.magic.magicaddons.ui.fonts.ModFont;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** every draw and every width measurement passes through getGlyphSource, so this one point puts the mod's font on all of them */
@Mixin(Font.class)
public class FontMixin {

    @ModifyVariable(method = "getGlyphSource", at = @At("HEAD"), argsOnly = true)
    private FontDescription magicaddons$substituteModFont(FontDescription description) {
        return ModFont.substitute(description);
    }
}
