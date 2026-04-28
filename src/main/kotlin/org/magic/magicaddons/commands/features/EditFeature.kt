package org.magic.magicaddons.commands.features

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.ui.screens.FeatureEditScreen
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
                    if (feature.baseSetting.children == null) {
                        it.source.sendError(ChatUtils.buildWithPrefix("Feature ${feature.displayName} does not have sub settings."))
                        return@executes 0
                    }

                    ScreenUtil.setScreen(FeatureEditScreen(feature, null))
                    1
                }

            command.then(featureNode)
        }

        return command
    }
}