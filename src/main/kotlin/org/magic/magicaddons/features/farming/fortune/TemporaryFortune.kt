package org.magic.magicaddons.features.farming.fortune

import java.time.Duration
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.crops.CropDrop
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.SystemChatEvent
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.info.TabWidget
import tech.thatgravyboat.skyblockapi.api.events.info.TabWidgetChangeEvent
import tech.thatgravyboat.skyblockapi.api.profile.items.accessory.AccessoryBagAPI

object TemporaryFortune {

    const val PESTHUNTER: String = "Pesthunter"
    const val CENTURY_CAKE: String = "Century Cake"
    const val CACAO_TRUFFLE: String = "Dark Cacao Truffle"

    const val CENTURY_CAKE_FORTUNE: Double = 5.0

    private val PHILIP_MESSAGE: Regex = Regex("""Pesthunter Phillip: In exchange for [\d,]+ Pests, I've given you \+(\d+) Farming Fortune for (\d+)m!""")
    private val CENTURY_CAKES_LINE: Regex = Regex("""Century Cakes: (?:(\d+)h)?\s*(?:(\d+)m)?\s*\((\d+)/(\d+)\)""")
    private val CONTEST_TIME_LINE: Regex = Regex("""Jacob's Contest: (?:(\d+)h)?\s*(?:(\d+)m)?\s*(?:(\d+)s)?\s*(left|until|ago)?""")
    private const val NOT_BOOSTED_MARK: Char = '○'
    private val ANITA_CONTEST_MESSAGE: Regex = Regex("""Your .+? is giving you \+(\d+) (.+?) Fortune during the contest!""")
    private val CONTEST_LENGTH: Duration = Duration.ofMinutes(20)

    private val ANITA_ACCESSORIES: Map<String, Double> = mapOf("ANITA_TALISMAN" to 5.0, "ANITA_RING" to 15.0, "ANITA_ARTIFACT" to 25.0)
    private var lastContestLines: List<String> = emptyList()

    private const val HYPERCHARGE_RARE_PER_LEVEL: Double = 0.03
    private const val HYPERCHARGE_EPIC_PER_LEVEL: Double = 0.04
    private const val HYPERCHARGE_LEGENDARY_PER_LEVEL: Double = 0.05
    private const val OVERDRIVE_RARE_PER_LEVEL: Double = 5.0
    private const val OVERDRIVE_EPIC_PER_LEVEL: Double = 6.0
    private const val OVERDRIVE_LEGENDARY_PER_LEVEL: Double = 7.0
    private const val CHIP_RARE_MAX_LEVEL: Int = 10
    private const val CHIP_EPIC_MAX_LEVEL: Int = 15

    private val CONTEST_CROP_NAMES: List<String> = (FortuneCrops.representatives.map { FortuneCrops.statName(it) } + "Melon").sortedByDescending { it.length }

    class Contest(val crops: List<CropDrop>, val boosted: CropDrop?, val endsAt: Long?, val isActive: Boolean)

    var contest: Contest? = null
        private set

    var cakeCount: Pair<Int, Int>? = null
        private set

    fun hyperchargeMultiplier(): Double {
        val level = FortuneReadings.get(FortuneField.Hypercharge)?.level?.toInt() ?: return 1.0
        return 1.0 + level * chipBand(level, HYPERCHARGE_RARE_PER_LEVEL, HYPERCHARGE_EPIC_PER_LEVEL, HYPERCHARGE_LEGENDARY_PER_LEVEL)
    }

    fun overdriveFortune(crop: CropDrop): Double {
        val active = contest?.takeIf { it.isActive } ?: return 0.0
        if (crop !in active.crops) return 0.0
        val level = FortuneReadings.get(FortuneField.Overdrive)?.level?.toInt() ?: return 0.0
        return level * chipBand(level, OVERDRIVE_RARE_PER_LEVEL, OVERDRIVE_EPIC_PER_LEVEL, OVERDRIVE_LEGENDARY_PER_LEVEL)
    }

    fun anitaContestFortune(crop: CropDrop): Double {
        val active = contest?.takeIf { it.isActive } ?: return 0.0
        val boosted = active.boosted ?: return 0.0
        if (FortuneCrops.statName(crop) != FortuneCrops.statName(boosted)) return 0.0
        return anitaAccessoryBonus() ?: 0.0
    }

    private fun anitaAccessoryBonus(): Double? {
        val bag = AccessoryBagAPI.getItems()
        if (bag.isEmpty()) return FortuneReadings.get(FortuneField.AnitaAccessory)?.level
        return bag.mapNotNull { ANITA_ACCESSORIES[ItemFortune.rawId(it.item)] }.maxOrNull()
    }

    private fun chipBand(level: Int, rare: Double, epic: Double, legendary: Double): Double = when {
        level <= CHIP_RARE_MAX_LEVEL -> rare
        level <= CHIP_EPIC_MAX_LEVEL -> epic
        else -> legendary
    }

    fun notePesthunter(baseFortune: Double, remaining: Duration) {
        FortuneReadings.setTemporary(PESTHUNTER, baseFortune * hyperchargeMultiplier(), System.currentTimeMillis() + remaining.toMillis())
    }

    fun clearPesthunter() = FortuneReadings.clearTemporary(PESTHUNTER)

    @EventHandler
    fun onChat(event: SystemChatEvent) {
        PHILIP_MESSAGE.find(event.text)?.let { notePesthunter(it.groupValues[1].toDouble(), Duration.ofMinutes(it.groupValues[2].toLong())) }
        ANITA_CONTEST_MESSAGE.find(event.text)?.let { match ->
            val crop = FortuneCrops.byStatName(match.groupValues[2]).firstOrNull() ?: return@let
            FortuneReadings.set(FortuneField.AnitaAccessory, null, match.groupValues[1].toDouble())
            val now = System.currentTimeMillis()
            val current = contest?.takeIf { (it.endsAt ?: 0) > now }
            contest = Contest(current?.crops?.ifEmpty { null } ?: listOf(crop), crop, current?.endsAt ?: (now + CONTEST_LENGTH.toMillis()), isActive = true)
        }
    }

    @Subscription
    fun onTabWidget(event: TabWidgetChangeEvent) {
        when (event.widget) {
            TabWidget.ACTIVE_EFFECTS -> readActiveEffects(event.new)
            TabWidget.JACOBS_CONTEST -> readContest(event.new)
            else -> Unit
        }
    }

    private fun readActiveEffects(lines: List<String>) {
        val match = lines.firstNotNullOfOrNull { CENTURY_CAKES_LINE.find(it) } ?: return
        val hours = match.groupValues[1].toLongOrNull() ?: 0
        val minutes = match.groupValues[2].toLongOrNull() ?: 0
        val count = match.groupValues[3].toInt()
        val total = match.groupValues[4].toInt()
        cakeCount = count to total
        val remaining = Duration.ofHours(hours).plusMinutes(minutes)
        if (count >= total) {
            FortuneReadings.setTemporary(CENTURY_CAKE, CENTURY_CAKE_FORTUNE * hyperchargeMultiplier(), System.currentTimeMillis() + remaining.toMillis())
        }
    }

    private fun readContest(lines: List<String>) {
        if (lines.isEmpty()) return
        if (lines != lastContestLines) Common.LOGGER.info("Jacob's contest widget: {}", lines)
        lastContestLines = lines
        val time = lines.firstNotNullOfOrNull { CONTEST_TIME_LINE.find(it) }
        val remaining = time?.let {
            Duration.ofHours(it.groupValues[1].toLongOrNull() ?: 0).plusMinutes(it.groupValues[2].toLongOrNull() ?: 0).plusSeconds(it.groupValues[3].toLongOrNull() ?: 0)
        }
        val isActive = lines.any { it.trim().startsWith("ACTIVE") } || time?.groupValues?.get(4) == "left"
        val crops = mutableListOf<CropDrop>()
        var boosted: CropDrop? = null
        lines.filter { CONTEST_TIME_LINE.find(it) == null }.forEach { line ->
            val text = line.trim()
            val statName = CONTEST_CROP_NAMES.firstOrNull { name -> Regex("\\b${Regex.escape(name)}\\b").containsMatchIn(text) } ?: return@forEach
            val lineCrops = FortuneCrops.byStatName(statName)
            crops += lineCrops
            val marker = text.substringBefore(statName)
            if (marker.isNotBlank() && NOT_BOOSTED_MARK !in marker) boosted = lineCrops.first()
        }
        val endsAt = remaining?.let { System.currentTimeMillis() + it.toMillis() }
        val previous = contest
        val sameContest = previous != null && previous.crops.containsAll(crops) && previous.crops.size > crops.size &&
                (endsAt == null || previous.endsAt == null || kotlin.math.abs(previous.endsAt - endsAt) < CONTEST_LENGTH.toMillis())
        contest = if (sameContest) Contest(previous.crops, boosted ?: previous.boosted, endsAt ?: previous.endsAt, isActive) else Contest(crops, boosted, endsAt, isActive)
    }
}
