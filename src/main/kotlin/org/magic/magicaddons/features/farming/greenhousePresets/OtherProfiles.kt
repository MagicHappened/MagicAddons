package org.magic.magicaddons.features.farming.greenhousePresets

import org.magic.magicaddons.data.greenhouse.Codecs.GREENHOUSE_GRID_CODEC
import org.magic.magicaddons.data.greenhouse.Codecs.MISC_GREENHOUSE_INFO_CODEC
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.MiscGreenhouseInfo
import org.magic.magicaddons.data.handlers.CodecStorage
import org.magic.magicaddons.data.handlers.DataHandler
import java.time.Instant
import java.util.UUID

/**
 * The greenhouses of the profiles not being played, read from their files and moved on by their
 * own clocks, so their plants can still warn. Nothing here is written back.
 */
object OtherProfiles {

    class Profile(val id: UUID, val name: String, val misc: MiscGreenhouseInfo, val grids: List<GreenhouseGrid>)

    var profiles: List<Profile> = emptyList()
        private set

    /** Reads every profile folder but the active one. */
    fun reload() {
        val active = DataHandler.activeProfile
        profiles = DataHandler.profileIds().filter { it != active }.mapNotNull { id ->
            val file = DataHandler.greenhouseFile(id)
            val misc = CodecStorage.load(file, MISC_GREENHOUSE_INFO_CODEC, wrapperKey = "misc_info") ?: return@mapNotNull null
            val grids = CodecStorage.load(file, GREENHOUSE_GRID_CODEC.listOf(), wrapperKey = "greenhouses") ?: return@mapNotNull null
            Profile(id, DataHandler.profileName(id) ?: id.toString().take(8), misc, grids)
        }
    }

    /** Moves each profile's plants on by however many of its ticks have passed; nobody is there to see them. */
    fun advance() {
        val now = Instant.now()

        profiles.forEach { profile ->
            val misc = profile.misc
            val nextTick = misc.nextTickTime ?: return@forEach
            if (!nextTick.isBefore(now)) return@forEach

            val cropGrowth = misc.cropGrowthValue ?: return@forEach
            val upgrade = misc.cropSpeedUpgradeValue ?: return@forEach

            val uniques = profile.grids
                .flatMap { it.layout.elementInstances }
                .filter { it.cropDef.isBaseCrop }
                .map { GreenhouseData.UniqueCropKey.from(it.cropDef) }
                .toSet()
            val tickMs = GreenhouseData.computeGrowthStageTimeMs(uniques.size, cropGrowth, upgrade, misc.greenhouseSpeedAttribute ?: 0)

            val overdueMs = now.toEpochMilli() - nextTick.toEpochMilli()
            val elapsedTicks = (overdueMs / tickMs).toInt() + 1

            misc.nextTickTime = nextTick.plusMillis(elapsedTicks * tickMs)
            profile.grids.forEach { grid ->
                grid.state.pendingGrowthTicks = (grid.state.pendingGrowthTicks ?: 0) + elapsedTicks
                grid.predictGrowth(elapsedTicks, tickMs)
            }
        }
    }
}
