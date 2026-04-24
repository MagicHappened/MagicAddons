package org.magic.magicaddons.commands.features.farming

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.resources.Resource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.ui.screens.GreenhouseScreen
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

object GreenhouseScreenCommand : AbstractCommand() {
    override val argument: String = "GreenhouseScreen"
    override val description: String = "Opens the Greenhouse Screen"
    // todo for now make it from base MagicAddons but later on maybe change it
    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument).executes {
            ScreenUtil.setScreen(GreenhouseScreen(Component.literal("GreenhouseScreen")))
            /*
            val resourceManager = Minecraft.getInstance().resourceManager
            val resources = resourceManager.listResources("") { true }

            resources.forEach { (identifier, resource) ->
                if (identifier.namespace != "magicaddons") return@forEach
                ChatUtils.sendWithPrefix("Resource: ${resource.metadata()}")
                ChatUtils.sendWithPrefix("Identifier: $identifier \nNamespace: ${identifier.namespace} || Path: ${identifier.path}")
            }
            */
            return@executes 1
        }
    }
}