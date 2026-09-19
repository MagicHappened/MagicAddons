package org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState

import org.magic.magicaddons.commands.debug.CropCollector
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.ClickEvent
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.magic.magicaddons.Common
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId
import net.minecraft.network.chat.Style
import java.time.Duration
import java.time.Instant
import org.magic.magicaddons.util.parseDurationToMs
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.events.interact.*

/**
 * What the beacon page and the tool in hand say about one plant, and what to do when they
 * disagree with what is recorded.
 */
object PlantDiagnostics {

    fun readDiagnosis(realItems: List<ItemStack>, listening: ScannedPlant?, hit: BlockPos?) {
        if (!baseSetting.value) return
        val identifyStack = realItems.firstOrNull() ?: return

        val stackId = identifyStack.getSkyBlockId()
        val useNameFallback = stackId == null

        var def: CropDefinition? = null

        if (useNameFallback) {
            if (identifyStack.getLore().any { it.string.contains("Base Crop") }) {
                def = CropRegistry.findByIdOrName(identifyStack.customName?.string ?: identifyStack.itemName.string)
            }
        } else {
            def = CropRegistry.findByIdOrName(stackId.id)
        }

        val beaconLore = realItems.firstOrNull { it.item == Items.BEACON }?.getLore()
        val saplingLore = realItems.firstOrNull { it.item == Items.JUNGLE_SAPLING }?.getLore()
        val bucketLore = realItems.firstOrNull { it.item == Items.WATER_BUCKET }?.getLore()

        if (beaconLore == null || saplingLore == null || bucketLore == null) {
            if (CropCollector.isActive()) ChatUtils.sendWithPrefix(
                "The diagnosis is missing a page: " +
                        listOfNotNull(
                            "status".takeIf { beaconLore == null },
                            "growth".takeIf { saplingLore == null },
                            "water".takeIf { bucketLore == null }
                        ).joinToString(", ")
            )
            return
        }

        val waterLevel = runCatching {
            bucketLore[0].siblings[1].string.trim().toDouble()
        }.getOrNull()

        val status = beaconLore.valueFor("Status") ?: runCatching {
            beaconLore[0].siblings[1].string
        }.getOrNull()

        // "Uncollectable" and its like sit on their own lines as often as on the status line
        val statusPage = beaconLore.joinToString(" ") { it.string }

        // these two are read out of a fixed position in the lore rather than by label, so an empty
        // result prints the page it came from
        if (waterLevel == null && CropCollector.isActive()) {
            ChatUtils.sendWithPrefix("Could not read the water level, the water page reads:")
            dumpLore(bucketLore)
        }

        if (status == null && CropCollector.isActive()) {
            ChatUtils.sendWithPrefix("Could not read the status, the status page reads:")
            dumpLore(beaconLore)
        }

        val age = saplingLore.valueFor("Age")

        // "Stage: 1/15", of which only the part before the slash is the stage
        val stageRaw = saplingLore.valueFor("Stage")?.substringBefore('/')?.trim()
            ?.let { raw ->
                when {
                    raw.equals("FULLY GROWN", ignoreCase = true) -> def?.maxStage
                    raw.equals("DEAD", ignoreCase = true) -> {
                        if (def?.skyblockId == SkyBlockItemId.item("DEAD_PLANT"))
                            return@let 1
                        return@let null
                    }

                    else -> raw.toIntOrNull()
                }
            }

        val nextStage = saplingLore.valueFor("Next Stage")

        if (nextStage?.contains(Regex("\\d")) ?: false) {
            if (!LocationAPI.isGuest) {
                val was = GreenhouseData.miscInfo.nextTickTime

                GreenhouseData.miscInfo.nextTickTime = Instant.now().plusMillis(nextStage.parseDurationToMs())
                GreenhouseData.lastCheckTime = Instant.now()

                // the one line of the old tick logging worth keeping: how far the countdown had
                // drifted by the moment the game stated it, for reading a session back later
                was?.let {
                    val movedS = Duration.between(it, GreenhouseData.miscInfo.nextTickTime).toSeconds()

                    Common.LOGGER.info("[tick] resynced from the game: countdown moved ${movedS}s")
                }

                GreenhouseData.realignWithGameTime()
            }
        }

        val target = listening?.takeIf { GreenhouseData.inOwnGarden() }?.let { disputeRecordWith(it, def, statusPage) }

        target?.let { element ->
            age?.parseDurationToMs()?.let { element.plant.age = it }
            stageRaw?.let { element.plant.growthStage = GrowthStageInfo.Known(it) }

            val plant = element.plant
            if (plant.cropDef.isMutation) {
                val stage = stageRaw ?: plant.highestStage
                val grown = stage != null && stage >= plant.cropDef.maxStage
                when {
                    grown && statusPage.contains("Uncollectable", ignoreCase = true) -> plant.placed = true
                    statusPage.contains("Harvestable", ignoreCase = true) -> Unit
                    stage != null && !grown && plant.placed -> plant.placed = false
                }
            }

            waterLevel?.let {
                element.plant.waterLevel = it
                element.plant.waterBestCase = null
                element.plant.waterPredictedInDebt = false
                element.plant.waterExact = true
            }
        }

        if (!CropCollector.isActive()) return

        if (def == null) {
            ChatUtils.sendWithPrefix(
                "No crop described for ${stackId?.id ?: "an unrecognised plant"}, nothing to match against."
            )
            return
        }

        if (stageRaw == null) {
            ChatUtils.sendWithPrefix(
                "Could not read what stage ${def.name} is at, the growth page reads:"
            )

            dumpLore(saplingLore)
            return
        }

        saplingLore.valueFor("Stage")
            ?.substringAfter('/', "")
            ?.trim()
            ?.toIntOrNull()
            ?.takeIf { it != def.maxStage }
            ?.let {
                ChatUtils.sendWithPrefix(
                    "${def.name} is described with ${def.maxStage} stages but the game says $it"
                )
            }

        if (hit == null) {
            ChatUtils.sendWithPrefix("Nothing was pointed at, so there is no plant to correct.")
            return
        }
        
        CropCollector.correct(def, stageRaw, hit)
    }

    
    private fun disputeRecordWith(element: ScannedPlant, toolCrop: CropDefinition?, statusPage: String): ScannedPlant? {
        val plant = element.plant
        val otherCrop = toolCrop != null && toolCrop != plant.cropDef
        val toolSaysGrowing = statusPage.contains("Growing", ignoreCase = true)

        if (plant.isPlacedMutation && (otherCrop || toolSaysGrowing)) plant.placed = false
        if (!otherCrop) return element

        val grid = GreenhouseData.getCurrentGrid() ?: return null
        val footprint = plant.cropDef.footprint
        val slots = buildSet {
            for (offsetX in 0 until footprint.width) {
                for (offsetY in 0 until footprint.height) {
                    add(plant.slot.x + offsetX to plant.slot.y + offsetY)
                }
            }
        }
        GreenhouseData.rescanSlots(grid, slots)

        val found = grid.layout.getSlot(plant.slot.x, plant.slot.y)?.let { grid.elementCoveringSlot(it) }
        return found?.takeIf { it.plant.cropDef == toolCrop }
    }

    private fun List<Component>.valueFor(label: String): String? =
        firstOrNull { it.string.trimStart().startsWith("$label:", ignoreCase = true) }
            ?.string
            ?.substringAfter(':')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun dumpLore(lore: List<Component>) {
        lore.forEachIndexed { index, line ->
            val pieces = line.siblings
                .mapIndexed { pieceIndex, piece -> "[$pieceIndex]${piece.string}" }
                .joinToString(" ")

            val whole = "[$index] ${line.string}    pieces: $pieces"

            ChatUtils.send(
                Component.literal("  $whole").withStyle(
                    Style.EMPTY
                        .withColor(ChatFormatting.GRAY)
                        .withClickEvent(ClickEvent.CopyToClipboard(whole))
                        .withHoverEvent(HoverEvent.ShowText(Component.literal("Click to copy")))
                )
            )
        }
    }
}
