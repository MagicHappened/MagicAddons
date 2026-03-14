package org.magic.magicaddons.commands.features

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.command.CommandSource
import net.minecraft.text.Text
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.util.ChatUtils

object ToggleFeature : AbstractCommand() {
    override val argument: String = "toggle"
    override val description: String = "Toggle a specific feature"
    val mainCommand: LiteralArgumentBuilder<FabricClientCommandSource> = literal<FabricClientCommandSource>("toggle")
        .executes  {
            it.source.sendError(Text.literal("Missing feature to toggle."))
            return@executes 1
        }
    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        buildFeatureLiterals()
        return mainCommand
    }

    fun buildFeatureLiterals(){
        FeatureManager.features.forEach { feature ->
            mainCommand.then(
                literal<FabricClientCommandSource>(feature.id)
                    .executes {
                        feature.enabled = !feature.enabled
                        it.source.sendFeedback(ChatUtils.buildWithPrefix(feature.displayName + " Feature "+ if (feature.enabled) "Enabled" else "Disabled"))
                        return@executes 1
                    }
            )
        }
    }
}