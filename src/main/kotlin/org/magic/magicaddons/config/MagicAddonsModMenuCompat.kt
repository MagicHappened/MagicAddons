package org.magic.magicaddons.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.magic.magicaddons.ui.screens.ConfigScreen

class MagicAddonsModMenuCompat : ModMenuApi {


    override fun getModConfigScreenFactory(): ConfigScreenFactory<Screen> {
        return ConfigScreenFactory { parent: Screen ->
            ConfigScreen(Component.literal("Magic Addons Config"), parent)
        }
    }
}