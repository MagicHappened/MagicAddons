package org.magic.magicaddons.commands.features

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import net.minecraft.network.chat.Component
import org.magic.magicaddons.ui.screens.ConfigScreen
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

object EditFeature : AbstractCommand() {
    override val argument: String = "edit"
    override val description: String = "Edit a feature specifically"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val command = LiteralArgumentBuilder.literal<FabricClientCommandSource>("edit")
        command.executes {
            it.source.sendError(ChatUtils.buildWithPrefix("Must provide a feature to edit"))
            return@executes 0
        }
        FeatureManager.features.forEach { feature ->
            val featureNode = LiteralArgumentBuilder.literal<FabricClientCommandSource>(feature.id)
                .executes {
                    ScreenUtil.setScreen(ConfigScreen(Component.literal("Magic Addons Config"), null).apply { showFeature(feature) })
                    1
                }

            command.then(featureNode)
        }

        return command
    }
}