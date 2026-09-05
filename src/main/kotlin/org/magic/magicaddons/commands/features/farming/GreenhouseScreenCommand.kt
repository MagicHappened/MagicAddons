package org.magic.magicaddons.commands.features.farming

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.StringRange
import com.mojang.brigadier.suggestion.Suggestion
import com.mojang.brigadier.suggestion.Suggestions
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.ui.screens.CropPreviewScreen
import org.magic.magicaddons.ui.screens.GreenhouseScreen
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil
import java.util.concurrent.CompletableFuture

/** Opens the greenhouse screen, or with "preview" the crop preview, on a crop when one is named. */
object GreenhouseScreenCommand : AbstractCommand() {
    override val argument: String = "GreenhouseScreen"
    override val description: String = "Opens the Greenhouse Screen"
    override val aliases: List<String> = listOf("gh")

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val preview = LiteralArgumentBuilder.literal<FabricClientCommandSource>("preview")
            .executes {
                ScreenUtil.setScreen(CropPreviewScreen(null))
                1
            }
            .then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("crop", StringArgumentType.word())
                    .suggests { _, builder ->
                        val typed = builder.remainingLowerCase
                        val suggestions = CropRegistry.all.map { cropWord(it) }.distinct()
                            .filter { it.lowercase().startsWith(typed) }
                            .map { Suggestion(StringRange.between(builder.start, builder.input.length), it) }
                        CompletableFuture.completedFuture(Suggestions(StringRange.between(builder.start, builder.input.length), suggestions))
                    }
                    .executes {
                        val word = StringArgumentType.getString(it, "crop")
                        val def = CropRegistry.all.firstOrNull { def -> cropWord(def).equals(word, ignoreCase = true) }
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
                ScreenUtil.setScreen(GreenhouseScreen(Component.literal("GreenhouseScreen")))
                1
            }
            .then(preview)
    }

    /** A crop's name as one word, the way it is typed in chat. */
    private fun cropWord(def: CropDefinition): String = def.name.filter { c -> c.isLetterOrDigit() }
}
