package org.magic.magicaddons.features.mining

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.SystemChatEvent
import org.magic.magicaddons.events.interact.BlockUseEvent
import org.magic.magicaddons.events.interact.UseEvent
import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.ui.hud.ConfigTarget
import org.magic.magicaddons.ui.hud.HudContent
import org.magic.magicaddons.ui.hud.HudElement
import org.magic.magicaddons.ui.hud.HudLine
import org.magic.magicaddons.util.ServerClock
import org.magic.magicaddons.util.compat.McCompat
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockCategory
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.api.profile.hunting.AttributeAPI
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import kotlin.math.ceil


object PickaxeAbilityCooldown : Feature() {

    init {
        EventBus.register(this)
        SkyBlockAPI.eventBus.register(this)
    }

    private val BASE_COOLDOWN_SECONDS: Map<String, Int> = mapOf(
        "Mining Speed Boost" to 120,
        "Pickobulus" to 50,
        "Maniac Miner" to 120,
        "Tunnel Vision" to 120,
        "Gemstone Infusion" to 120,
        "Sheer Force" to 120
    )

    private val FUEL_TANK_REDUCTION: List<Pair<String, Double>> = listOf(
        "perfectly" to 0.10,
        "gemstone" to 0.06,
        "titanium" to 0.04,
        "mithril" to 0.02
    )

    private const val COOLDOWN_ATTRIBUTE_ID: String = "attribute:e8"

    private const val ATTRIBUTE_REDUCTION_PER_LEVEL: Double = 0.01

    private val ABILITY_USED: Regex = Regex("You used your (.+) Pickaxe Ability!")

    private const val SKY_MALL_DAY: String = "New day! Your Sky Mall buff changed!"
    private const val NEW_BUFF: String = "New buff:"
    private const val SKY_MALL_COOLDOWN_BUFF: String = "Pickaxe Ability cooldown"
    private const val SKY_MALL_REDUCTION: Double = 0.20

    private const val SKY_MALL_BUFF_MS: Long = 20 * 60 * 1000

    private const val MAYHEM_COOLDOWN_LINE: String = "MAYHEM! Your Pickaxe Ability cooldown was reduced from your Mineshaft Mayhem perk!"
    private const val MAYHEM_REDUCTION: Double = 0.25

    private val MINING_ISLANDS: Set<SkyBlockIsland> = setOf(SkyBlockIsland.DWARVEN_MINES, SkyBlockIsland.CRYSTAL_HOLLOWS, SkyBlockIsland.MINESHAFT)

    private val PICKAXE_CATEGORIES: Set<SkyBlockCategory> = setOf(SkyBlockCategory.PICKAXE, SkyBlockCategory.DRILL, SkyBlockCategory.GAUNTLET)

    /** How long after a click its chat line may still arrive; a click with nothing after it was not a use. */
    private const val CHAT_WINDOW_MS: Long = 3_000

    private const val READY_TITLE_FADE: Int = 3
    private const val READY_TITLE_STAY: Int = 20

    /** A moment on both clocks, so a cooldown can be counted on either whatever the setting was then. */
    private class Stamp(val realMs: Long, val serverMs: Long) {
        fun elapsedMs(onServerTime: Boolean): Long =
            if (onServerTime) ServerClock.nowMs() - serverMs else System.currentTimeMillis() - realMs
    }

    private fun stampNow(): Stamp = Stamp(System.currentTimeMillis(), ServerClock.nowMs())

    /** A click with a pickaxe, and what its cooldown will be cut by once the chat names the ability. */
    private class PendingUse(val at: Stamp, val itemFactor: Double) {
        fun awaitingChat(): Boolean = at.elapsedMs(onServerTime = false) <= CHAT_WINDOW_MS
    }

    private var pendingUse: PendingUse? = null

    /** Whether the next "New buff:" line is Sky Mall's. */
    private var skyMallBuffNext: Boolean = false

    /** When Sky Mall last announced the pickaxe cooldown buff, null once it announced another. */
    private var skyMallCooldownSince: Long? = null

    /** Whether Mineshaft Mayhem picked the cooldown buff in the mineshaft the player is in. */
    private var mayhemCooldown: Boolean = false

    private var abilityName: String? = null
    private var usedAt: Stamp? = null
    private var cooldownMs: Long? = null
    private var warnedReady: Boolean = true

    @EventHandler
    fun onItemUse(event: UseEvent) = noteClick(event.item)

    @EventHandler
    fun onBlockUse(event: BlockUseEvent) = noteClick(event.item)

    private fun noteClick(item: ItemStack) {
        if (!baseSetting.value || !isPickaxe(item)) return

        pendingUse = PendingUse(stampNow(), 1.0 - fuelTankReduction(item))
    }

    private fun isPickaxe(item: ItemStack): Boolean =
        item.getData(DataTypes.CATEGORY) in PICKAXE_CATEGORIES ||
                item.getLore().any { it.string.contains("Breaking Power") }

    private fun fuelTankReduction(item: ItemStack): Double {
        val tank = item.getData(DataTypes.FUEL_TANK)?.lowercase() ?: return 0.0
        return FUEL_TANK_REDUCTION.firstOrNull { (word, _) -> tank.contains(word) }?.second ?: 0.0
    }

    @EventHandler
    fun onChat(event: SystemChatEvent) {
        if (event.overlay) return
        trackBuffs(event.text)
        if (!baseSetting.value) return

        val name = ABILITY_USED.find(event.text)?.groupValues?.get(1) ?: return
        val use = pendingUse?.takeIf { it.awaitingChat() } ?: return
        pendingUse = null

        val base = BASE_COOLDOWN_SECONDS[name] ?: return

        abilityName = name
        usedAt = use.at
        cooldownMs = (base * 1000 * use.itemFactor * playerFactor()).toLong()
        warnedReady = false
    }

    /** Follows Sky Mall's daily buff and Mineshaft Mayhem's pick, whether or not the feature is on. */
    private fun trackBuffs(text: String) {
        when {
            text.startsWith(SKY_MALL_DAY) -> skyMallBuffNext = true
            text.startsWith("New day!") -> skyMallBuffNext = false
            text.startsWith(NEW_BUFF) && skyMallBuffNext -> {
                skyMallBuffNext = false
                skyMallCooldownSince = if (text.contains(SKY_MALL_COOLDOWN_BUFF)) System.currentTimeMillis() else null
            }
            text.startsWith(MAYHEM_COOLDOWN_LINE) -> mayhemCooldown = true
        }
    }

    /**
     * A mineshaft's Mayhem pick is gone once the player leaves it. Only leaving clears it, since the
     * pick may be announced before the arrival in the mineshaft is.
     */
    @Subscription
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.old == SkyBlockIsland.MINESHAFT) mayhemCooldown = false
    }

    /** The reductions that belong to the player rather than the item, multiplied together. */
    private fun playerFactor(): Double = attributeFactor() * skyMallFactor() * mayhemFactor()

    private fun skyMallFactor(): Double {
        val since = skyMallCooldownSince ?: return 1.0
        val active = System.currentTimeMillis() - since < SKY_MALL_BUFF_MS && LocationAPI.island in MINING_ISLANDS
        return if (active) 1.0 - SKY_MALL_REDUCTION else 1.0
    }

    private fun mayhemFactor(): Double =
        if (mayhemCooldown && LocationAPI.island == SkyBlockIsland.MINESHAFT) 1.0 - MAYHEM_REDUCTION else 1.0

    private fun attributeLevel(): Int? =
        AttributeAPI.attributeMap.entries.firstOrNull { it.key.id == COOLDOWN_ATTRIBUTE_ID }?.value?.level

    private fun attributeFactor(): Double = 1.0 - (attributeLevel() ?: 0) * ATTRIBUTE_REDUCTION_PER_LEVEL

    fun debugLines(held: ItemStack): List<String> {
        val now = System.currentTimeMillis()
        fun percent(factor: Double) = "%.1f%%".format((1.0 - factor) * 100)
        fun seconds(ms: Long) = "%.1fs".format(ms / 1000.0)

        val tank = held.getData(DataTypes.FUEL_TANK)
        val itemFactor = 1.0 - if (isPickaxe(held)) fuelTankReduction(held) else 0.0
        val total = itemFactor * playerFactor()

        return buildList {
            add("Feature on: ${baseSetting.value}, ready warning: ${readyWarning.value}")
            add("Island: ${LocationAPI.island ?: "none"}, mining island: ${LocationAPI.island in MINING_ISLANDS}")
            add("Held: ${held.hoverName.string}")
            add("  category: ${held.getData(DataTypes.CATEGORY)?.name ?: "none"}, breaking power line: ${held.getLore().any { it.string.contains("Breaking Power") }}, counts as pickaxe: ${isPickaxe(held)}")
            add("  fuel tank: ${tank ?: "none"} -> -${percent(itemFactor)}")
            add("E8 attribute: level ${attributeLevel()?.toString() ?: "unknown"} -> -${percent(attributeFactor())}")
            add(
                "Sky Mall: cooldown buff " + (skyMallCooldownSince?.let { "seen ${seconds(now - it)} ago" } ?: "not seen") +
                        ", expecting its buff line: $skyMallBuffNext -> -${percent(skyMallFactor())}"
            )
            add("Mineshaft Mayhem: cooldown picked: $mayhemCooldown -> -${percent(mayhemFactor())}")
            add("Player factor ${"%.4f".format(playerFactor())}, with held item ${"%.4f".format(total)}")
            BASE_COOLDOWN_SECONDS.forEach { (name, base) -> add("  $name: ${base}s -> ${seconds((base * 1000 * total).toLong())}") }
            add("Pending click: " + (pendingUse?.let { "${seconds(now - it.at.realMs)} ago, item factor ${"%.4f".format(it.itemFactor)}" } ?: "none"))
            add(
                "Timer: " + (abilityName?.let { name -> "$name, " + (msLeft()?.let { if (it > 0) "${seconds(it)} left" else "ready" } ?: "not started") } ?: "none") +
                        ", ready warned: $warnedReady"
            )
            add(
                "Clock: ${if (useServerTime.value) "server" else "real"} time" + (usedAt?.let {
                    ", since use ${seconds(it.elapsedMs(onServerTime = false))} real, ${seconds(it.elapsedMs(onServerTime = true))} server"
                } ?: "")
            )
        }
    }

    @EventHandler
    fun onWorldTick(event: WorldTickEvent) {
        val left = msLeft() ?: return
        if (warnedReady || left > 0) return
        warnedReady = true

        if (!readyWarning.value || Minecraft.getInstance().player == null) return
        McCompat.showTitle(
            Component.literal("${abilityName ?: "Pickaxe Ability"} Ready!").withStyle(ChatFormatting.GREEN),
            READY_TITLE_FADE, READY_TITLE_STAY, READY_TITLE_FADE
        )
    }

    /** What is left of the cooldown on the chosen clock, below zero once it has run out; null before any use. */
    private fun msLeft(): Long? {
        val cooldown = cooldownMs ?: return null
        val used = usedAt ?: return null
        return cooldown - used.elapsedMs(useServerTime.value)
    }

    private fun secondsLeft(): Int? = msLeft()?.takeIf { it > 0 }?.let { ceil(it / 1000.0).toInt() }

    val hud: HudElement = object : HudElement("pickaxe_ability", "Pickaxe Ability") {
        override val defaultX: Int = 20
        override val defaultY: Int = 60
        override val shadow: Boolean = true

        override val configTarget: ConfigTarget
            get() = ConfigTarget(PickaxeAbilityCooldown, listOf(baseSetting))

        override fun content(): HudContent? {
            if (!baseSetting.value) return null
            if (miningIslandsOnly.value && LocationAPI.island !in MINING_ISLANDS) return null

            val secondsLeft = secondsLeft()
            val waitingOnChat = secondsLeft == null && pendingUse?.awaitingChat() == true
            val label = abilityName ?: if (waitingOnChat) DEFAULT_LABEL else return null
            val value = when {
                waitingOnChat -> Component.literal("-").withStyle(ChatFormatting.GRAY)
                secondsLeft != null -> Component.literal("${secondsLeft}s").withStyle(ChatFormatting.YELLOW)
                else -> Component.literal("Ready").withStyle(ChatFormatting.GREEN)
            }
            return HudContent(listOf(HudLine.Pair(Component.literal(label).withStyle(ChatFormatting.GOLD), value)))
        }

        override fun sample(): HudContent = HudContent(
            listOf(
                HudLine.Pair(
                    Component.literal("Pickobulus").withStyle(ChatFormatting.GOLD),
                    Component.literal("32s").withStyle(ChatFormatting.YELLOW)
                )
            )
        )
    }

    private const val DEFAULT_LABEL: String = "Pickaxe Ability"

    override val id: String = "PickaxeAbilityCooldown"
    override val displayName: String = "Pickaxe Ability Cooldown"
    override val description: String = "Shows your pickaxe ability cooldown as a HUD element"
    override val category: String = "mining"

    private val readyWarning = BooleanSetting(
        key = "ReadyWarning",
        displayName = "Ready Warning",
        description = "Puts \"<ability> Ready!\" on screen when the cooldown runs out",
        value = false
    )

    private val useServerTime = BooleanSetting(
        key = "UseServerTime",
        displayName = "Use Server Time",
        description = "Counts the cooldown down by the server's ticks instead of real time, so a lagging " +
                "server slows the timer the way it slows the cooldown. Time away from a server, such as " +
                "switching lobbies, still counts as real time",
        value = false
    )

    private val miningIslandsOnly = BooleanSetting(
        key = "MiningIslandsOnly",
        displayName = "Display only in mining islands",
        description = "",
        value = true
    )

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false,
        children = listOf(readyWarning, useServerTime, miningIslandsOnly)
    )
}
