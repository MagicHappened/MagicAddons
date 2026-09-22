package org.magic.magicaddons.features.farming.greenhousePresets.playerActions

import java.time.Duration
import java.time.Instant
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.ScannedPlant
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Chloronite
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.AttackEntityEvent
import org.magic.magicaddons.events.interact.BlockBreakEvent
import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhousePresets
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.lookups.StatsWidget
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.location.ServerDisconnectEvent

object BreakProtection {

    private const val CHLORONITE: String = "Chloronite"

    // message templates for each break prevention
    private const val PREVENTED: String = "Prevented breaking %s"
    private const val PREVENTED_PEST_DEBUFF: String = "Prevented breaking %s, the pest debuff is active"
    private const val PREVENTED_UNDER_FORTUNE: String = "Prevented breaking %s, %,d %s is under your %,d threshold"
    private const val PREVENTED_WIDGET_OFF: String =
        "Prevented breaking %s, the Stats widget isn't enabled so %s can't be read"
    private const val PREVENTED_STAT_MISSING: String =
        "Prevented breaking %s, %s isn't in your Stats widget so it can't be read"

    private const val HINT_WIDGET_OFF: String = "Stats widget isn't enabled! %s will not work without it!"
    private const val HINT_STAT_MISSING: String =
        "Stats widget doesn't have %s for %s! The feature will not work until it's added to the stats widget."

    // dont spam messages because we use startBreaking event
    private const val MESSAGE_COOLDOWN_MS: Long = 1_000

    /** the delay after joining the garden when the break prevention setting is enabled but fortune stats are disabled to warn the user to enable them*/
    private val HINT_DELAY: Duration = Duration.ofSeconds(5)

    // map for each slot that have sent its message to avoid repeats
    private val lastMessageAt = mutableMapOf<Triple<String, Int, Int>, Long>()

    // avoid spamming the missing widget hint
    private val sentHints = mutableSetOf<String>()


    private var hintDueAt: Instant? = null

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val plant = GreenhouseData.scannedPlantAtBlock(event.pos) ?: return
        if (refusesBreak(plant)) event.canceled = true
    }

    @EventHandler
    fun onAttackEntity(event: AttackEntityEvent) {
        val stand = event.target as? ArmorStand ?: return
        val plant = GreenhouseData.scannedPlantAtStand(stand) ?: return
        if (refusesBreak(plant)) event.canceled = true
    }

    private fun refusesBreak(scanned: ScannedPlant): Boolean {
        val message = messageForPlantBreak(scanned) ?: return false

        sendMessageForPlant(scanned.plant, message)
        return true
    }

    /** the message for why a plant was prevented from breaking */
    private fun messageForPlantBreak(scanned: ScannedPlant): String? {
        val plant = scanned.plant
        val crop = plant.cropDef.name

        
        if (plant.cropDef.isMutation || plant.cropDef.isBaseCrop) {
            if (GreenhousePresets.preventBreakingNonHarvestable() && !GreenhousePresets.isHarvestable(plant)) {
                return PREVENTED.format(crop)
            }
        }

        if (!plant.cropDef.isMutation) return null

        // farming fortune rules shouldnt apply to chloronite drops (maybe it will be needed for the base crop drop of chloronite?)
        // the meta for farming probably will never be chloronite itself for crop drops, if that changes its here to change.
        if (crop == CHLORONITE) {
            if (!GreenhousePresets.preventBreakingChloroniteUnderMiningFortune()) return null

            return fortunePreventionMessage(crop, StatsWidget.MINING_FORTUNE, "mining fortune", GreenhousePresets.miningFortuneThreshold)
        }

        if (GreenhousePresets.preventBreakingDuringPestDebuff() && GreenhouseData.pestDebuffActive) {
            return PREVENTED_PEST_DEBUFF.format(crop)
        }
        if (!GreenhousePresets.preventBreakingUnderFarmingFortune()) return null

        return fortunePreventionMessage(crop, StatsWidget.FARMING_FORTUNE, "farming fortune", GreenhousePresets.farmingFortuneThreshold)
    }

    /** returns a reason to prevent a fortune crop from breaking, null when its allowed to be broken */
    private fun fortunePreventionMessage(crop: String, stat: String, statInWords: String, threshold: Int): String? {
        if (StatsWidget.isShown != true) return PREVENTED_WIDGET_OFF.format(crop, statInWords)

        val fortune = StatsWidget.value(stat) ?: return PREVENTED_STAT_MISSING.format(crop, stat)
        if (fortune >= threshold) return null

        return PREVENTED_UNDER_FORTUNE.format(crop, fortune, statInWords, threshold)
    }

    private fun sendMessageForPlant(plant: Plant, message: String) {
        val key = Triple(GreenhouseData.getCurrentGrid()?.layout?.id.orEmpty(), plant.slot.x, plant.slot.y)
        val now = System.currentTimeMillis()
        if (now - (lastMessageAt[key] ?: 0L) < MESSAGE_COOLDOWN_MS) return

        lastMessageAt[key] = now
        ChatUtils.sendWithPrefix(message)
    }

    @Subscription
    fun onDisconnect(event: ServerDisconnectEvent) {
        sentHints.clear()
        hintDueAt = null
    }

    @EventHandler
    fun onTick(event: WorldTickEvent) {
        if (!GreenhouseData.inOwnGarden()) {
            hintDueAt = null
            return
        }

        // dont send a message too early
        if (StatsWidget.isShown == null) {
            hintDueAt = null
            return
        }

        // armed from the tick rather than the island change, so it also covers the mod starting up
        // with the player already standing in their garden
        val due = hintDueAt ?: Instant.now().plus(HINT_DELAY).also { hintDueAt = it }
        if (Instant.now().isBefore(due)) return

        if (GreenhousePresets.preventBreakingUnderFarmingFortune()) {
            hintIfUnreadable(StatsWidget.FARMING_FORTUNE, GreenhousePresets.farmingFortuneSettingName)
        }
        if (GreenhousePresets.preventBreakingChloroniteUnderMiningFortune()) {
            hintIfUnreadable(StatsWidget.MINING_FORTUNE, GreenhousePresets.miningFortuneSettingName)
        }
    }

    private fun hintIfUnreadable(stat: String, settingName: String) {
        val hint = when {
            StatsWidget.isShown != true -> HINT_WIDGET_OFF.format(settingName)
            StatsWidget.value(stat) == null -> HINT_STAT_MISSING.format(stat, settingName)
            else -> return
        }

        if (sentHints.add(hint)) ChatUtils.sendWithPrefix(hint)
    }
}
