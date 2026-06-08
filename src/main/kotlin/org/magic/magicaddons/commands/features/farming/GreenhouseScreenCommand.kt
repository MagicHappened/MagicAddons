package org.magic.magicaddons.commands.features.farming

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.ui.screens.GreenhouseScreen
import org.magic.magicaddons.util.ScreenUtil

object GreenhouseScreenCommand : AbstractCommand() {
    var tempGuiScale: Int? = null

    override val argument: String = "GreenhouseScreen"
    override val description: String = "Opens the Greenhouse Screen"
    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument).executes {
            tempGuiScale = Minecraft.getInstance().options.guiScale().get();
            Minecraft.getInstance().options.guiScale().set(2);
            ScreenUtil.setScreen(GreenhouseScreen(Component.literal("GreenhouseScreen")))
            return@executes 1
        }
    }
}