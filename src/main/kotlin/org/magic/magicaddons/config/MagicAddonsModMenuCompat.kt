package org.magic.magicaddons.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.magic.magicaddons.config.ui.screen.ConfigScreen

class MagicAddonsModMenuCompat : ModMenuApi {


    override fun getModConfigScreenFactory(): ConfigScreenFactory<Screen> {
        return ConfigScreenFactory { parent: Screen ->
            ConfigScreen(Text.literal("Magic Addons Config"), parent)
        }
    }
}