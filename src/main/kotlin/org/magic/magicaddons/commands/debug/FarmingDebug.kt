package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.commands.CropWords
import org.magic.magicaddons.data.greenhouse.crops.MissingCropData
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseSpawnLog
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.VersionChecker

object FarmingDebug : AbstractCommand() {

    /** The plant dex word that lists every gap instead of one crop. */
    private const val MISSING_WORD: String = "missing"

    override val argument: String = "farming"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val collect = LiteralArgumentBuilder.literal<FabricClientCommandSource>("collect")
                    .executes {
                        CropCollector.scan()
                        return@executes 1
                    }
                    .then(
                        LiteralArgumentBuilder.literal<FabricClientCommandSource>("guide")
                            .executes {
                                CropCollector.sendGuide()
                                return@executes 1
                            }
                    )
                    .then(
                        LiteralArgumentBuilder.literal<FabricClientCommandSource>("finish")
                            .executes {
                                CropCollector.finish()
                                return@executes 1
                            }
                    )
                    .then(
                        LiteralArgumentBuilder.literal<FabricClientCommandSource>("quit")
                            .executes {
                                CropCollector.quit()
                                return@executes 1
                            }
                    )

        val farming = LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                it.source.sendError(ChatUtils.buildWithPrefix("missing farming debug argument"))
                return@executes 0
            }
            .then(
                cropDataGapsCommand()
            )
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("spawnLog")
                    .executes {
                        GreenhouseSpawnLog.toggle()
                        return@executes 1
                    }
            )
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("lostPlants")
                    .executes {
                        ChatUtils.sendWithPrefix(
                            "lost plants ${allowed(GreenhouseSpawnLog.toggleLostPlantsMessages())} send why they were lost"
                        )
                        return@executes 1
                    }
            )

        if (VersionChecker.onBeta()) farming.then(collect)
        return farming
    }

    private fun allowed(on: Boolean): String = if (on) "now" else "no longer"

    /** The dex, and under it "missing" then every crop as a command word. */
    private fun cropDataGapsCommand(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>("plantDex")
            .executes {
                sendCropDataGaps()
                return@executes 1
            }
            .then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, String>(
                    "crop",
                    StringArgumentType.word()
                ).suggests { _, builder ->
                    CropWords.suggest(builder, listOf(MISSING_WORD))
                }.executes {
                    val word = StringArgumentType.getString(it, "crop")
                    val def = CropWords.find(word)

                    when {
                        word.equals(MISSING_WORD, ignoreCase = true) -> dumpMissingPlants()
                        def != null -> sendCropDataGapsFor(def)
                        else -> ChatUtils.sendWithPrefix("No crop called $word.")
                    }
                    return@executes 1
                }
            )

    /** Every crop still missing something, one line per tier with the crops in its hover. */
    private fun dumpMissingPlants() {
        val gaps = MissingCropData.gapsByTier()

        if (gaps.isEmpty()) {
            ChatUtils.sendWithPrefix("Nothing missing. The dex is complete.")
            return
        }

        ChatUtils.sendWithPrefix(
            Component.literal("Currently missing plants (hover):").withStyle(ChatFormatting.GOLD)
        )

        gaps.forEach { (tier, crops) ->
            val hover = Component.empty()
            crops.forEachIndexed { index, gap ->
                if (index > 0) hover.append(Component.literal("\n"))
                hover.append(Component.literal(gap.crop.name).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" -> ${gap.missingParts.joinToString("; ")}").withStyle(ChatFormatting.GRAY))
            }

            ChatUtils.send(
                Component.literal("  ${tier.heading} (${crops.size})")
                    .withStyle(ChatFormatting.YELLOW)
                    .withStyle { it.withHoverEvent(HoverEvent.ShowText(hover)) }
            )
        }
    }

    private fun sendCropDataGapsFor(def: CropDefinition) {
        val missing = MissingCropData.missingStagesSummary(def)

        if (missing == null) {
            ChatUtils.sendWithPrefix(
                Component.literal("${def.name}: all ${def.maxStage} stages recorded")
                    .withStyle(ChatFormatting.GREEN)
            )
            return
        }

        ChatUtils.sendWithPrefix(
            Component.literal("${def.name}: ${MissingCropData.recordedPercent(def)}% of ${def.maxStage} stages")
                .withStyle(ChatFormatting.GOLD)
        )
        ChatUtils.send(
            Component.literal("  $missing").withStyle(ChatFormatting.GRAY)
        )
    }


    private fun sendCropDataGaps() {
        val report = MissingCropData.cropDataReport()

        ChatUtils.sendWithPrefix(
            Component.literal(
                "Plant dex: ${report.percent}% recorded (${report.recordedStages} of ${report.totalStages} stages)"
            ).withStyle(ChatFormatting.GOLD)
        )

        if (report.listing.isEmpty()) {
            ChatUtils.sendWithPrefix("Nothing missing. The dex is complete.")
            return
        }

        Minecraft.getInstance().keyboardHandler.clipboard = report.listing

        ChatUtils.send(sendWithCopyClipboard(report.listing, "${report.incompleteCrops} crops incomplete"))
    }

    private fun sendWithCopyClipboard(text: String, what: String): Component {
        val lines = text.count { it == '\n' } + 1

        return ChatUtils.buildWithPrefix(
            Component.literal("Click to copy $lines lines ($what)").withStyle(
                Style.EMPTY
                    .withColor(ChatFormatting.YELLOW)
                    .withClickEvent(ClickEvent.CopyToClipboard(text))
                    .withHoverEvent(HoverEvent.ShowText(Component.literal("Click to copy")))
            )
        )
    }
}
