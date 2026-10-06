package org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState

import java.time.Instant
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.greenhouse.GrowthTickEvent
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.MiscGreenhouseInfo
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.handlers.ModFiles.LoadResult
import org.magic.magicaddons.util.ChatUtils

object OtherProfiles {

    class Profile(val name: String, val misc: MiscGreenhouseInfo, val grids: List<GreenhouseGrid>)

    var profiles: List<Profile> = emptyList()
        private set

    fun reload() {
        val activeProfileId = GreenhouseProfiles.activeProfileId
        profiles = GreenhouseProfiles.profileIds().filter { it != activeProfileId }.mapNotNull { profileId ->
            val name = GreenhouseProfiles.fruitNameOf(profileId) ?: profileId.toString().take(8)
            val contents = when (val result = GreenhouseProfiles.readGreenhouseFile(profileId)) {
                is LoadResult.NoFile -> return@mapNotNull null
                is LoadResult.Loaded -> result.value
                is LoadResult.Unreadable -> {
                    Common.LOGGER.error("Greenhouse data for $name could not be read, left out of the other profiles", result.cause)
                    ChatUtils.sendWithPrefix("Greenhouse data for $name could not be read, so it is left out of your other profiles.")
                    return@mapNotNull null
                }
            }
            Profile(name, contents.miscInfo ?: return@mapNotNull null, contents.greenhouses ?: return@mapNotNull null)
        }
    }

    fun advanceTicks() {
        val now = Instant.now()

        profiles.forEach { profile ->
            val misc = profile.misc
            val nextTick = misc.nextTickTime ?: return@forEach
            if (!nextTick.isBefore(now)) return@forEach

            val cropGrowth = misc.cropGrowthValue ?: return@forEach
            val upgrade = misc.cropSpeedUpgradeValue ?: return@forEach

            val uniques = profile.grids
                .flatMap { it.layout.plants }
                .filter { it.cropDef.isBaseCrop && !it.isHalted }
                .map { GreenhouseData.UniqueCropKey.from(it.cropDef) }
                .toSet()
            val tickMs = GreenhouseTickTime.stageTimeMs(uniques.size, cropGrowth, upgrade, misc.greenhouseSpeedAttribute ?: 0)

            val overdueMs = now.toEpochMilli() - nextTick.toEpochMilli()
            val elapsedTicks = (overdueMs / tickMs).toInt() + 1

            misc.nextTickTime = nextTick.plusMillis(elapsedTicks * tickMs)
            profile.grids.forEach { grid ->
                grid.state.ticksSinceLastScan += elapsedTicks
                grid.simulateGreenhouse(elapsedTicks)
            }
            EventBus.post(GrowthTickEvent(elapsedTicks, tickMs, profile.name, isActiveProfile = false))
        }
    }
}
