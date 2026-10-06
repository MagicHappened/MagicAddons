package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.commands.CropWords
import org.magic.magicaddons.data.greenhouse.crops.MissingCropData
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseSpawnLog
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseTickTime
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.PlantDiagnostics
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.PlayerUtils

object FarmingDebug : AbstractCommand() {

    private const val MISSING_WORD: String = "missing"

    private const val SKULL_HASH_SHOWN_LENGTH: Int = 8

    override val argument: String = "farming"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val collect = LiteralArgumentBuilder.literal<FabricClientCommandSource>("collect")
                    .executes {
                        CropCollector.scanGreenhouse()
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
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("scanStatus")
                    .executes {
                        sendScanStatus()
                        return@executes 1
                    }
            )
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("exportGreenhouseData")
                    .executes {
                        GreenhouseDataExport.copyServerUpload()
                        return@executes 1
                    }
                    .then(
                        LiteralArgumentBuilder.literal<FabricClientCommandSource>("serverUpload").executes {
                            GreenhouseDataExport.copyServerUpload()
                            return@executes 1
                        }
                    )
                    .then(
                        LiteralArgumentBuilder.literal<FabricClientCommandSource>("current").executes {
                            GreenhouseDataExport.copyCurrentGreenhouse()
                            return@executes 1
                        }
                    )
                    .then(
                        LiteralArgumentBuilder.literal<FabricClientCommandSource>("all").executes {
                            GreenhouseDataExport.copyAllGreenhouses()
                            return@executes 1
                        }
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("combinedMutationOutput")
                    .executes {
                        PlantDiagnostics.toggleCombinedMutationOutput()
                        return@executes 1
                    }
            )
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("shards")
                    .executes {
                        sendGreenhouseShardLevels()
                        return@executes 1
                    }
            )

        return farming.then(collect)
    }

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
                    CropWords.suggestCrops(builder, listOf(MISSING_WORD))
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

    private fun sendGreenhouseShardLevels() {
        fun levelText(level: Int?): String = level?.let { "level $it" } ?: "not found"

        val speedShard = levelText(GreenhouseTickTime.shardLevelOf(GreenhouseData.GREENHOUSE_SPEED_ATTRIBUTE_ID))
        val floraShard = if (GreenhouseTickTime.isFloraAttributeIdKnown) levelText(GreenhouseTickTime.floraShardLevel()) else "no flora id yet"

        ChatUtils.sendWithPrefix("Greenhouse speed shard: $speedShard\nFlora shard: $floraShard")
    }

    private fun sendScanStatus() {
        val status = GreenhouseData.scanStatus()

        if (status == null) {
            ChatUtils.sendWithPrefix("Not standing in a greenhouse.")
            return
        }

        val lastScan = status.lastScanAgoMs?.let { "last full scan ${seconds(it)} ago" } ?: "never scanned"
        val waiting = status.deferredForMs?.let { "waiting for ${seconds(it)}" } ?: if (status.isSettled) "settled" else "not settled"

        ChatUtils.sendWithPrefix(
            Component.literal("Scan on ${status.plotName}: $waiting, $lastScan").withStyle(ChatFormatting.GOLD)
        )

        val lastChanged = status.lastChangedStand?.let { ". Last stand to change: ${describeStand(it)}" } ?: ""
        ChatUtils.send(
            Component.literal("  Quiet for ${status.quietForMs}ms of ${status.quietNeededMs}ms$lastChanged").withStyle(ChatFormatting.GRAY)
        )

        if (status.movingStands.isEmpty()) {
            ChatUtils.send(Component.literal("  No stand is moving").withStyle(ChatFormatting.GRAY))
            return
        }

        ChatUtils.send(Component.literal("  Stands still moving: ${status.movingStands.size}").withStyle(ChatFormatting.YELLOW))
        status.movingStands.forEach { moving ->
            val distance = "%.3f".format(moving.distanceToTarget)
            ChatUtils.send(
                Component.literal("   ${describeStand(moving.stand)}, $distance blocks from its target, for ${seconds(moving.movingForMs)}")
                    .withStyle(ChatFormatting.GRAY)
            )
        }
    }

    private fun seconds(milliseconds: Long): String = "%.1fs".format(milliseconds / 1000.0)

    private fun describeStand(stand: ArmorStand): String {
        val carried = stand.customName?.string?.let { "\"$it\"" }
            ?: PlayerUtils.getSkullHash(stand)?.let { "skull ${it.take(SKULL_HASH_SHOWN_LENGTH)}" }
            ?: EntityUtils.heldItem(stand)?.second
            ?: "empty stand"
        val position = stand.blockPosition()

        return "$carried at ${position.x} ${position.y} ${position.z}"
    }

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

            ChatUtils.send(ChatUtils.buildStyled("  ${tier.heading} (${crops.size})", ChatFormatting.YELLOW, hover))
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

        val lines = report.listing.count { it == '\n' } + 1

        ChatUtils.send(
            ChatUtils.buildWithPrefix(
                ChatUtils.buildStyled(
                    "Click to copy $lines lines (${report.incompleteCrops} crops incomplete)",
                    ChatFormatting.YELLOW,
                    Component.literal("Click to copy"),
                    ClickEvent.CopyToClipboard(report.listing),
                )
            )
        )
    }
}
