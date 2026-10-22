package org.magic.magicaddons.commands.farming

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.data.greenhouse.crops.CropDrop
import org.magic.magicaddons.features.farming.fortune.FarmingFortune
import org.magic.magicaddons.features.farming.fortune.FieldScope
import org.magic.magicaddons.features.farming.fortune.FortuneAudit
import org.magic.magicaddons.features.farming.fortune.FortuneCrops
import org.magic.magicaddons.features.farming.fortune.FortuneField
import org.magic.magicaddons.features.farming.fortune.FortuneReadings
import org.magic.magicaddons.features.farming.fortune.FortuneSnapshot
import org.magic.magicaddons.features.farming.fortune.FortuneStat
import org.magic.magicaddons.ui.screens.FortuneSourcesScreen
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil

object FortuneCommand : AbstractCommand() {
    override val argument: String = "fortune"

    private const val FIELD_ARGUMENT: String = "field"
    private const val CROP_ARGUMENT: String = "crop"
    private const val VALUE_ARGUMENT: String = "value"

    private val fieldSuggestions = SuggestionProvider<FabricClientCommandSource> { _, builder ->
        FortuneField.entries.map { it.name }.filter { it.lowercase().startsWith(builder.remainingLowerCase) }.forEach(builder::suggest)
        builder.buildFuture()
    }

    private val cropSuggestions = SuggestionProvider<FabricClientCommandSource> { _, builder ->
        FortuneCrops.representatives.map { it.name }.filter { it.lowercase().startsWith(builder.remainingLowerCase) }.forEach(builder::suggest)
        builder.buildFuture()
    }

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                report(FarmingFortune.snapshot())
                1
            }
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("sources").executes {
                    ScreenUtil.setScreen(FortuneSourcesScreen())
                    1
                }
            )
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("set").then(
                    RequiredArgumentBuilder.argument<FabricClientCommandSource, String>(FIELD_ARGUMENT, StringArgumentType.word())
                        .suggests(fieldSuggestions)
                        .then(
                            RequiredArgumentBuilder.argument<FabricClientCommandSource, Double>(VALUE_ARGUMENT, DoubleArgumentType.doubleArg(0.0))
                                .executes { set(it, crop = null) }
                        )
                        .then(
                            RequiredArgumentBuilder.argument<FabricClientCommandSource, String>(CROP_ARGUMENT, StringArgumentType.word())
                                .suggests(cropSuggestions)
                                .then(
                                    RequiredArgumentBuilder.argument<FabricClientCommandSource, Double>(VALUE_ARGUMENT, DoubleArgumentType.doubleArg(0.0))
                                        .executes { set(it, crop = StringArgumentType.getString(it, CROP_ARGUMENT)) }
                                )
                        )
                )
            )

    private fun set(context: CommandContext<FabricClientCommandSource>, crop: String?): Int {
        val fieldName = StringArgumentType.getString(context, FIELD_ARGUMENT)
        val field = FortuneField.byName(fieldName)
        if (field == null) {
            ChatUtils.sendWithPrefix("No fortune source called $fieldName. Sources: ${FortuneField.entries.joinToString(", ") { it.name }}")
            return 0
        }
        val value = DoubleArgumentType.getDouble(context, VALUE_ARGUMENT)
        field.maxLevel?.let { max ->
            if (value > max) {
                ChatUtils.sendWithPrefix("${field.displayName} goes up to $max ${field.unit}.")
                return 0
            }
        }

        if (field.scope == FieldScope.PerCrop) {
            val drop = crop?.let { name -> CropDrop.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } }
            if (drop == null) {
                ChatUtils.sendWithPrefix("${field.displayName} needs a crop: /ma fortune set ${field.name} <crop> <${field.unit}>")
                return 0
            }
            FortuneCrops.byStatName(FortuneCrops.statName(drop)).forEach { FortuneReadings.set(field, it, value, FortuneReadings.ORIGIN_MANUAL) }
            ChatUtils.sendWithPrefix("${field.displayName} for ${FortuneCrops.statName(drop)} set to ${FortuneAudit.format(value)} ${field.unit}.")
            return 1
        }

        if (crop != null) {
            ChatUtils.sendWithPrefix("${field.displayName} is not per crop: /ma fortune set ${field.name} <${field.unit}>")
            return 0
        }
        FortuneReadings.set(field, null, value, FortuneReadings.ORIGIN_MANUAL)
        ChatUtils.sendWithPrefix("${field.displayName} set to ${FortuneAudit.format(value)} ${field.unit}.")
        return 1
    }

    private fun report(snapshot: FortuneSnapshot) {
        val message = ChatUtils.buildWithPrefix("Farming Fortune: ${format(snapshot.general)}")

        snapshot.sources.filter { it.stat == FortuneStat.General }.groupBy { it.name }.forEach { (name, sources) ->
            val details = sources.mapNotNull { it.detail }.joinToString(", ")
            message.append(line(name, sources.sumOf { it.value }, details.takeIf { it.isNotEmpty() }, ChatFormatting.GREEN))
        }

        message.append(Component.literal("\nPer crop:").withStyle(ChatFormatting.GRAY))
        FortuneAudit.lines(snapshot).drop(1).forEach { audit ->
            val color = when (audit.verdict) {
                FortuneAudit.Verdict.Matches -> ChatFormatting.GREEN
                FortuneAudit.Verdict.Differs -> ChatFormatting.RED
                FortuneAudit.Verdict.NotInTab -> ChatFormatting.GRAY
            }
            message.append(Component.literal("\n  ${audit.text}").withStyle(color))
        }

        if (snapshot.unknown.isNotEmpty()) {
            message.append(Component.literal("\nNot counted yet (see /ma fortune sources):").withStyle(ChatFormatting.GRAY))
            snapshot.unknown.forEach { message.append(Component.literal("\n  $it").withStyle(ChatFormatting.DARK_GRAY)) }
        }

        ChatUtils.send(message)
    }

    private fun line(name: String, value: Double, detail: String?, color: ChatFormatting): Component {
        val text = Component.literal("\n  $name: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(format(value)).withStyle(color))
        detail?.let { text.append(Component.literal(" ($it)").withStyle(ChatFormatting.DARK_GRAY)) }
        return text
    }

    private fun format(value: Double): String =
        if (value % 1.0 == 0.0) "%+,d".format(value.toLong()) else "%+,.1f".format(value)
}
