package org.magic.magicaddons.commands.features

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.util.ChatUtils

object ToggleFeature : AbstractCommand() {
    override val argument: String = "toggle"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val command = literal<FabricClientCommandSource>(argument)
            .executes {
                it.source.sendError(ChatUtils.buildWithPrefix("Missing feature to toggle."))
                return@executes 0
            }

        FeatureManager.features.forEach { feature ->
            command.then(
                literal<FabricClientCommandSource>(feature.id)
                    .executes {
                        feature.baseSetting.value = !feature.baseSetting.value
                        it.source.sendFeedback(ChatUtils.buildWithPrefix(feature.displayName + " Feature "+ if (feature.baseSetting.value) "Enabled" else "Disabled"))
                        MagicAddonsConfigJsonHandler.save()
                        return@executes 1
                    }
            )
        }

        return command
    }
}