package org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState

import java.time.Duration
import java.time.Instant
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.debug.CropCollector
import org.magic.magicaddons.data.greenhouse.crops.*
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets.baseSetting
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.parseDurationToMs
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore

object PlantDiagnostics {

    private const val WATER_LABEL: String = "Water"

    fun readDiagnosticTool(realItems: List<ItemStack>, listening: ScannedPlant?, hit: BlockPos?) {
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
            if (CropCollector.isCollectorActive()) ChatUtils.sendWithPrefix(
                "The diagnosis is missing a page: " +
                        listOfNotNull(
                            "status".takeIf { beaconLore == null },
                            "growth".takeIf { saplingLore == null },
                            "water".takeIf { bucketLore == null }
                        ).joinToString(", ")
            )
            return
        }

        val waterLevel = bucketLore
            .firstOrNull { it.string.trimStart().startsWith(WATER_LABEL, ignoreCase = true) }
            ?.string
            ?.substringAfter(':')
            ?.substringBefore('/')
            ?.trim()
            ?.toDoubleOrNull()

        val status = beaconLore.valueFor("Status") ?: runCatching {
            beaconLore[0].siblings[1].string
        }.getOrNull()

        val statusPage = beaconLore.joinToString(" ") { it.string }

        if (waterLevel == null && CropCollector.isCollectorActive()) {
            sendLoreOnHover("Could not read the water level, hover to see details", bucketLore)
        }

        if (status == null && CropCollector.isCollectorActive()) {
            sendLoreOnHover("Could not read the status, hover to see details", beaconLore)
        }

        val age = saplingLore.valueFor("Age")

        // "Stage: 1/15"
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
            if (GreenhouseData.inOwnGreenhouse()) {
                val was = GreenhouseData.miscInfo.nextTickTime

                GreenhouseData.miscInfo.nextTickTime = Instant.now().plusMillis(nextStage.parseDurationToMs())
                GreenhouseData.lastCheckTime = Instant.now()

                // how far the countdown had drifted by the moment the game stated it, for reading
                // a session back later
                was?.let {
                    val movedS = Duration.between(it, GreenhouseData.miscInfo.nextTickTime).toSeconds()

                    Common.LOGGER.info("[tick] resynced from the game: countdown moved ${movedS}s")
                }

                GreenhouseData.realignWithGameTime()
            }
        }

        val target = listening?.takeIf { GreenhouseData.inOwnGarden() }?.let { disputeRecordWith(it, def, statusPage) }

        target?.let { element ->
            age?.parseDurationToMs()?.let { element.plant.appearedAt = System.currentTimeMillis() - it }
            stageRaw?.let { element.plant.growthStage = PlantStage.Known(it) }

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

        if (!CropCollector.isCollectorActive()) return

        if (def == null) {
            ChatUtils.sendWithPrefix(
                "No crop described for ${stackId?.id ?: "an unrecognised plant"}, nothing to match against."
            )
            return
        }

        if (stageRaw == null) {
            sendLoreOnHover("Could not read what stage ${def.name} is at, hover to see details", saplingLore)
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
        
        CropCollector.correctEntries(def, stageRaw, hit)
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

    private fun sendLoreOnHover(message: String, lore: List<Component>) {
        val dump = lore.mapIndexed { index, line ->
            val pieces = line.siblings
                .mapIndexed { pieceIndex, piece -> "[$pieceIndex]${piece.string}" }
                .joinToString(" ")

            "[$index] ${line.string}    pieces: $pieces"
        }.joinToString("\n")

        ChatUtils.sendWithCopyableHover(message, dump)
    }
}
