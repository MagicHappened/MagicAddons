package org.magic.magicaddons.commands.features.farming

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.commands.CropWords
import org.magic.magicaddons.ui.screens.CropPreviewScreen
import org.magic.magicaddons.ui.screens.GreenhouseScreen
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

/** Opens the greenhouse screen, or with "preview" the crop preview, on a crop when one is named. */
object GreenhouseScreenCommand : AbstractCommand() {
    override val argument: String = "GreenhouseScreen"
    override val aliases: List<String> = listOf("gh")

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val preview = LiteralArgumentBuilder.literal<FabricClientCommandSource>("preview")
            .executes {
                ScreenUtil.setScreen(CropPreviewScreen(null))
                1
            }
            .then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("crop", StringArgumentType.word())
                    .suggests { _, builder -> CropWords.suggest(builder) }
                    .executes {
                        val word = StringArgumentType.getString(it, "crop")
                        val def = CropWords.find(word)
                        if (def == null) {
                            ChatUtils.sendWithPrefix("No crop called $word.")
                            return@executes 0
                        }
                        ScreenUtil.setScreen(CropPreviewScreen(null, def))
                        1
                    }
            )

        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                ScreenUtil.setScreen(GreenhouseScreen())
                1
            }
            .then(preview)
    }
}
