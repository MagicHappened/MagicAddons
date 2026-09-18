package org.magic.mixins;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** reads the text a text display shows */
@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {

    @Invoker("getText")
    Component magicaddons$getText();
}
