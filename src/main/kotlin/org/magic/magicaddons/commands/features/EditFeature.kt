package org.magic.magicaddons.commands.features

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.ui.screens.ConfigScreen
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

object EditFeature : AbstractCommand() {
    override val argument: String = "edit"

    private const val SETTING_ARGUMENT: String = "setting"

    fun commandFor(featureId: String, settingKey: String? = null): String =
        "/${Common.MOD_NAME} edit $featureId" + (settingKey?.let { " $it" } ?: "")

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val command = LiteralArgumentBuilder.literal<FabricClientCommandSource>("edit")
        command.executes {
            it.source.sendError(ChatUtils.buildWithPrefix("Must provide a feature to edit"))
            return@executes 0
        }
        FeatureManager.availableFeatures.forEach { feature ->
            val featureNode = LiteralArgumentBuilder.literal<FabricClientCommandSource>(feature.id)
                .executes {
                    ScreenUtil.setScreen(ConfigScreen(null).apply { showFeature(feature) })
                    1
                }
                .then(
                    RequiredArgumentBuilder.argument<FabricClientCommandSource, String>(SETTING_ARGUMENT, StringArgumentType.word())
                        .suggests { _, builder ->
                            feature.settingPaths().keys.forEach { builder.suggest(it) }
                            builder.buildFuture()
                        }
                        .executes {
                            val path = feature.pathToSetting(StringArgumentType.getString(it, SETTING_ARGUMENT))
                            ScreenUtil.setScreen(ConfigScreen(null).apply { if (path == null) showFeature(feature) else showSetting(feature, path) })
                            1
                        }
                )

            command.then(featureNode)
        }

        return command
    }
}