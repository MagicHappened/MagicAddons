package org.magic.magicaddons.commands.features

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.util.ChatUtils

object ToggleFeature : AbstractCommand() {
    override val argument: String = "toggle"
    override val description: String = "Toggle a specific feature"
    val mainCommand: LiteralArgumentBuilder<FabricClientCommandSource> = literal<FabricClientCommandSource>(argument)
        .executes  {
            it.source.sendError(ChatUtils.buildWithPrefix("Missing feature to toggle."))
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
                        feature.baseSetting.value = !feature.baseSetting.value
                        it.source.sendFeedback(ChatUtils.buildWithPrefix(feature.displayName + " Feature "+ if (feature.baseSetting.value) "Enabled" else "Disabled"))
                        MagicAddonsConfigJsonHandler.save() //hopefully fixes?
                        return@executes 1
                    }
            )
        }
    }
}