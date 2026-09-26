package org.magic.magicaddons.features.foraging.safarihelper

import java.time.Duration
import java.time.Instant
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import org.magic.magicaddons.util.compat.McCompat
import java.util.UUID
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.Items
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.SystemChatEvent
import org.magic.magicaddons.events.world.EntityAddedEvent
import org.magic.magicaddons.events.world.EntityRemovedEvent
import org.magic.magicaddons.events.world.EntityUpdatedEvent
import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.features.HighlightFeature
import org.magic.magicaddons.features.misc.HighlightMarkers
import org.magic.magicaddons.ui.hud.ConfigTarget
import org.magic.magicaddons.ui.hud.HudContent
import org.magic.magicaddons.ui.hud.HudElement
import org.magic.magicaddons.ui.hud.HudLine
import org.magic.magicaddons.ui.hud.HudSituation
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.EntityUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

object SafariHelper : HighlightFeature() {

    init {
        EventBus.register(this)
        SkyBlockAPI.eventBus.register(this)
    }

    val hud: HudElement = object : HudElement("safari", "Safari Uniques") {
        override val defaultX: Int = 20
        override val defaultY: Int = 20
        override val shadow: Boolean = true
        override val situations: Set<HudSituation> = setOf(HudSituation.SAFARI)

        override val configTarget: ConfigTarget
            get() = ConfigTarget(SafariHelper, listOf(baseSetting, uniqueTracking))

        override fun content(): HudContent? {
            if (!baseSetting.value || !uniqueTracking.value) return null
            if (LocationAPI.island != SkyBlockIsland.SAFARI) return null

            return HudContent(hudLines().map { HudLine.Text(it) })
        }

        override fun sample(): HudContent = HudContent(buildList {
            fun zone(name: String, player: String?, mobs: List<String>) {
                val heading = Component.literal(name).withStyle(ChatFormatting.GOLD)
                player?.let { heading.append(Component.literal(" ($it)").withStyle(ChatFormatting.AQUA)) }
                add(HudLine.Text(heading.append(Component.literal(":").withStyle(ChatFormatting.GRAY))))
                mobs.forEach { mob ->
                    add(
                        HudLine.Text(
                            Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)
                                .append(Component.literal(mob).withStyle(ChatFormatting.GREEN))
                        )
                    )
                }
            }

            zone("Forest", "MagicHappened", listOf("Macaw", "Woodchucker", "Treefrog"))
            zone("Ice", null, listOf("Wumpa"))
        })
    }

    private const val MOB_HIGHLIGHT_COLOR: Int = 0xFFFFC0CB.toInt()
    private const val SPARKLING_HIGHLIGHT_COLOR: Int = 0xFFFFAA00.toInt()
    private const val TREASURE_HIGHLIGHT_COLOR: Int = 0xFF55FF55.toInt()

    private const val SPARKLING_TAG: String = "sparkling"

    private const val SPARKLING_TITLE_FADE: Int = 5
    private const val SPARKLING_TITLE_STAY: Int = 40

    private val SPARKLING_PLING_PITCHES: List<Float> = listOf(1.0f, 1.0f, 1.0f)
    private const val SPARKLING_PLING_GAP: Int = 4

    private const val MACAW: String = "Macaw"

    private val catchPatterns = listOf(
        // "§a§lCAPTURE! §7You caught a §aTreefrog§7 and gained 2x §aTreefrog Shard§7!", with
        // sparklings saying "received" and rewards after the shards
        Regex("You caught an? (.+?) and (?:gained|received)"),
        // "§e§lLOOT SHARE! §7You received a §aPolaris Shard§7 from §bAceMech§7 catching a §aPolaris§7!"
        Regex("catching an? (.+?)!"),
        // hideyho is a little different
        // "§a§lCAPTURE! §7You found the §9Hideyho§7, and as a reward it gave you 3x §9Hideyho Shard§7!"
        Regex("You found the (.+?), and as a reward"),
        // "§e§lLOOT SHARE! §7You received 3x §9Hideyho Shard§7 from §bMeowMeowLynn§7 finding the §9Hideyho§7!"
        Regex("finding the (.+?)!")
    )

    override val id: String = "SafariHelper"
    override val displayName: String = "Safari Helper"
    override val description: String = "Helpers for the safari island"
    override val category: String = "foraging"

    override val highlightPriority: Int = 1

    private val onlyUncaught = BooleanSetting(
        key = "OnlyUncaught",
        displayName = "Only Uncaught",
        description = "Only highlights the uniques that have not been caught yet during this safari visit.",
        value = false
    )

    private val throughWallsSetting = BooleanSetting(
        key = "ThroughWalls",
        displayName = "Through Walls",
        description = "§cThis feature might be considered as a cheat and is therefore used at your own risk.",
        value = false,
        needsExtensionPack = true,
        children = listOf(HighlightMarkers.linkSetting())
    )

    override val throughWalls: Boolean get() = throughWallsSetting.value

    private val sparklingWarning = BooleanSetting(
        key = "SparklingWarning",
        displayName = "Sparkling Warning",
        description = "Puts a title on screen, sends a chat message and plays a few plings the first " +
                "time a sparkling is seen, on top of its glow. Once for each sparkling.",
        value = false
    )

    private val mobHighlight = BooleanSetting(
        key = "MobHighlight",
        displayName = "Mob Highlight",
        description = "Highlights the mobs and the grass treasure belonging to the safari zone you are in.",
        value = false,
        children = listOf(
            throughWallsSetting,
            onlyUncaught,
            sparklingWarning
        )
    )

    private val sendToPartyChat = BooleanSetting(
        key = "SendToPartyChat",
        displayName = "Send To Party Chat",
        description = "Sends the done message to the party chat instead of only to yourself.",
        value = false
    )

    private val ignoreMacaw = BooleanSetting(
        key = "IgnoreMacaw",
        displayName = "Ignore Macaw",
        description = "Adds a second done message for having caught everything except the macaw.",
        value = false
    )

    private val ownZoneOnly = BooleanSetting(
        key = "OwnZoneOnly",
        displayName = "Only Own Zone",
        description = "Only sends the zone done message for your own zone",
        value = false
    )

    private val zoneMessages = BooleanSetting(
        key = "ZoneMessages",
        displayName = "Zone Specific Messages",
        description = "Adds a done message for every safari zone that is finished before the last one.",
        value = false,
        children = listOf(
            ownZoneOnly
        )
    )

    private val doneMessage = BooleanSetting(
        key = "DoneMessage",
        displayName = "Done Message",
        description = "Sends a message when all unique critters have been caught.",
        value = false,
        children = listOf(
            sendToPartyChat,
            ignoreMacaw,
            zoneMessages
        )
    )

    private val shortenOtherZones = BooleanSetting(
        key = "ShortenOtherZones",
        displayName = "Only List My Zone",
        description = "Writes out the mobs left in your own zone only. Every other zone is one line " +
                "saying how many it has left, above yours so it is read first.",
        value = false
    )

    private val uniqueTracking = BooleanSetting(
        key = "UniqueTracking",
        displayName = "Unique Tracking",
        description = "Shows which unique mobs are still left to catch in the safari zone you are in.",
        value = false,
        children = listOf(
            doneMessage,
            shortenOtherZones
        )
    )

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false,
        children = listOf(
            mobHighlight,
            uniqueTracking
        )
    )

    var currentZone: SafariZone? = null
        private set

    private val caughtUniques = mutableSetOf<String>()

    /** when the island was arrived on, which every done message counts from */
    private var arrivedOnIslandAt: Instant? = null

    private val catchesByPlayer = mutableMapOf<String, MutableList<SafariZone>>()

    /** each players assigned zone */
    private val playerZones = mutableMapOf<String, SafariZone>()

    private val zoneOfMob: Map<String, SafariZone> by lazy {
        SafariZone.entries
            .flatMap { zone -> zone.uniqueMobs.map { it.displayName.lowercase() to zone } }
            .toMap()
    }

    private const val CATCHES_BEFORE_PLAYER_GUESS: Int = 2

    private val CATCHER_REGEX = Regex("""from (\S+) (?:catching|finding)""")

    private class DoneMessages(val done: String, val doneWithoutMacaw: String) {
        var doneSent: Boolean = false
        var doneWithoutMacawSent: Boolean = false

        fun reset() {
            doneSent = false
            doneWithoutMacawSent = false
        }
    }

    private val safariDoneMessages = DoneMessages(
        done = "All unique critters caught",
        doneWithoutMacaw = "All unique critters caught (no macaw)"
    )

    private val zoneDoneMessageMap: Map<SafariZone, DoneMessages> = SafariZone.entries.associateWith { zone ->
        DoneMessages(
            done = "All uniques caught in ${zone.displayName}",
            doneWithoutMacaw = "All uniques caught in ${zone.displayName} (no macaw)"
        )
    }

    private val designatedZone: SafariZone? get() = localPlayerName()?.let { playerZones[it] }

    private val sparklingEntities = mutableSetOf<Entity>()

    private val warnedSparklings = mutableSetOf<UUID>()

    private val pendingPlings = ArrayDeque<Pair<Int, Float>>()

    private fun warnOfSparkling(entity: Entity, mobName: String) {
        if (!sparklingWarning.value || !warnedSparklings.add(entity.uuid)) return

        McCompat.showTitle(
            Component.literal("Sparkling $mobName Detected!").withStyle(ChatFormatting.YELLOW),
            SPARKLING_TITLE_FADE, SPARKLING_TITLE_STAY, SPARKLING_TITLE_FADE
        )
        ChatUtils.sendWithPrefix("Sparkling $mobName detected!")

        SPARKLING_PLING_PITCHES.forEachIndexed { index, pitch -> pendingPlings.addLast(index * SPARKLING_PLING_GAP to pitch) }
    }

    /** Plays whatever pling is due this tick and counts the rest down. */
    private fun playDuePlings() {
        if (pendingPlings.isEmpty()) return

        val player = Minecraft.getInstance().player ?: run { pendingPlings.clear(); return }
        val due = pendingPlings.filter { it.first <= 0 }
        due.forEach { (_, pitch) -> player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1f, pitch) }
        pendingPlings.removeAll(due)

        for (index in pendingPlings.indices) pendingPlings[index] = pendingPlings[index].let { (ticks, pitch) -> (ticks - 1) to pitch }
    }

    override fun highlightColor(entity: Entity): Int = when {
        entity in sparklingEntities -> SPARKLING_HIGHLIGHT_COLOR
        isTreasureDisplay(entity) -> TREASURE_HIGHLIGHT_COLOR
        else -> MOB_HIGHLIGHT_COLOR
    }

    @EventHandler
    fun onWorldTick(event: WorldTickEvent) {
        val zone = if (LocationAPI.island == SkyBlockIsland.SAFARI) {
            Minecraft.getInstance().player?.position()?.let { SafariZone.at(it) }
        } else {
            null
        }

        if (zone != currentZone) {
            currentZone = zone
            invalidateHighlights()
        }

        playDuePlings()
    }

    @EventHandler
    fun onConfigChanged(event: ConfigChangedEvent) {
        invalidateHighlights()
    }

    @EventHandler
    fun onEntityAdded(event: EntityAddedEvent) {
        handleEntitiesAdded(event.addedEntityList)
    }

    @EventHandler
    fun onEntityRemoved(event: EntityRemovedEvent) {
        event.removedEntityList.forEach { sparklingEntities.remove(it.entity) }
        handleEntitiesRemoved(event.removedEntityList)
    }

    @EventHandler
    fun onEntityUpdated(event: EntityUpdatedEvent) {
        handleEntitiesUpdated(event.updatedEntityList)
    }

    @Subscription
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.new == SkyBlockIsland.SAFARI || event.old == SkyBlockIsland.SAFARI) {
            caughtUniques.clear()
            safariDoneMessages.reset()
            zoneDoneMessageMap.values.forEach { it.reset() }
            catchesByPlayer.clear()
            playerZones.clear()
            arrivedOnIslandAt = if (event.new == SkyBlockIsland.SAFARI) Instant.now() else null
        }
    }

    @EventHandler
    fun onSystemChat(event: SystemChatEvent) {
        if (!baseSetting.value) return
        if (currentZone == null) return

        val caught = catchPatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(event.text)?.groupValues?.get(1)
        } ?: return

        noteCatcher(event.text, caught)

        if (!caughtUniques.add(normalizeCaughtName(caught))) return

        if (mobHighlight.value && onlyUncaught.value) {
            invalidateHighlights()
        }

        sendDoneMessages()
    }

    private fun sendDoneMessages() {
        if (!uniqueTracking.value || !doneMessage.value) return

        val safariMessage = claim(safariDoneMessages, SafariZone.entries.flatMap { remainingIn(it) })
        safariMessage?.let { announceDone(it) }

        SafariZone.entries.forEach { zone ->
            val messages = zoneDoneMessageMap.getValue(zone)
            val message = claim(messages, remainingIn(zone)) ?: return@forEach

            if (safariMessage != null || !zoneMessages.value) return@forEach
            if (ownZoneOnly.value && zone != designatedZone) return@forEach

            announceDone(zoneDoneText(zone, withoutMacaw = message == messages.doneWithoutMacaw))
        }
    }

    /** saves the person catching so can attribute them to a zone. */
    private fun noteCatcher(text: String, caught: String) {
        val zone = zoneOfMob[normalizeCaughtName(caught)] ?: return
        val player = CATCHER_REGEX.find(text)?.groupValues?.get(1) ?: localPlayerName() ?: return

        if (player in playerZones) return

        val zones = catchesByPlayer.getOrPut(player) { mutableListOf() }
        zones.add(zone)

        if (zones.size < CATCHES_BEFORE_PLAYER_GUESS) return

        val counts = zones.groupingBy { it }.eachCount()
        val most = counts.values.max()

        counts.filterValues { it == most }.keys.singleOrNull()?.let { playerZones[player] = it }
    }

    private fun localPlayerName(): String? =
        Minecraft.getInstance().user.name.takeIf { it.isNotBlank() }

    /** the players in a zone, shouldn't be more than 1 realistically */
    private fun playersIn(zone: SafariZone): List<String> =
        playerZones.filterValues { it == zone }.keys.sorted()

    private fun claim(messages: DoneMessages, remaining: List<String>): String? {
        if (remaining.isEmpty()) {
            // catching everything says more than having caught everything but the macaw
            messages.doneWithoutMacawSent = true

            if (messages.doneSent) return null

            messages.doneSent = true
            return messages.done
        }

        if (!ignoreMacaw.value || messages.doneWithoutMacawSent) return null
        if (remaining.any { !it.equals(MACAW, ignoreCase = true) }) return null

        messages.doneWithoutMacawSent = true
        return messages.doneWithoutMacaw
    }

    /** A finished zone, naming whoever was placed in it, or said plainly when nobody was. */
    private fun zoneDoneText(zone: SafariZone, withoutMacaw: Boolean): String {
        val players = playersIn(zone)
        val tail = if (withoutMacaw) " (no macaw)" else ""

        if (players.isEmpty()) return "All uniques caught in ${zone.displayName}$tail"

        return "${players.joinToString(", ")} finished ${zone.displayName}$tail"
    }

    private fun announceDone(message: String) {
        val timed = message + timeOnIsland()

        if (sendToPartyChat.value) {
            ChatUtils.sendCommand("pc $timed")
            return
        }

        ChatUtils.sendWithPrefix(Component.literal(timed).withStyle(ChatFormatting.GREEN))
    }

    /** how long the visit has run, or nothing when the arrival was never seen */
    private fun timeOnIsland(): String {
        val arrived = arrivedOnIslandAt ?: return ""

        return " (${ChatUtils.shortDuration(Duration.between(arrived, Instant.now()).toMillis())})"
    }

    /** remaining uniques for the safari zone (for hud) */
    fun remainingIn(zone: SafariZone): List<String> =
        zone.uniqueMobs.map { it.displayName }.filterNot { isCaught(it) }

    private fun isCaught(mobName: String): Boolean = mobName.lowercase() in caughtUniques

    private fun normalizeCaughtName(mobName: String): String =
        mobName.lowercase().removePrefix("$SPARKLING_TAG ")

    /** returns the hud lines needed */
    private fun hudLines(): List<Component> {
        if (SafariZone.entries.all { remainingIn(it).isEmpty() }) {
            return listOf(Component.literal("All safari uniques caught").withStyle(ChatFormatting.GREEN))
        }
        // first the other players if collapsed, then self
        val localPlayerZone = designatedZone.takeIf { shortenOtherZones.value }


        val order = SafariZone.entries.sortedBy { it == localPlayerZone }

        return order.flatMap { zone ->
            if (localPlayerZone != null && zone != localPlayerZone) shortZoneLine(zone) else fullZoneLines(zone)
        }
    }

    /** all components for a safari zone */
    private fun fullZoneLines(zone: SafariZone): List<Component> {
        val remaining = remainingIn(zone)
        val heading = Component.literal(zone.displayName).withStyle(ChatFormatting.GOLD)
            .append(playerSuffix(zone))

        if (remaining.isEmpty()) {
            return listOf(heading.append(Component.literal(": done").withStyle(ChatFormatting.GREEN)))
        }

        return listOf(heading.append(Component.literal(":").withStyle(ChatFormatting.GRAY))) +
                remaining.map { mob ->
                    Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal(mob).withStyle(ChatFormatting.GREEN))
                }
    }

    /** for shortened option */
    private fun shortZoneLine(zone: SafariZone): List<Component> {
        val remaining = remainingIn(zone)
        val heading = Component.literal(zone.displayName).withStyle(ChatFormatting.GOLD)
            .append(playerSuffix(zone))

        val tail = if (remaining.isEmpty()) {
            Component.literal(": done").withStyle(ChatFormatting.GREEN)
        } else {
            Component.literal(": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("${remaining.size} left").withStyle(ChatFormatting.YELLOW))
        }

        return listOf(heading.append(tail))
    }

    /** players suffix for a zone or none if unassigned */
    private fun playerSuffix(zone: SafariZone): Component {
        val players = playersIn(zone)
        if (players.isEmpty()) return Component.empty()

        return Component.literal(" (${players.joinToString(", ")})").withStyle(ChatFormatting.AQUA)
    }

    override fun highlightTarget(info: EntityInfo): Entity? {
        val sparkling = isSparkling(info)
        val highlight = matchesHighlight(info, sparkling)
        val target = if (highlight) visiblePartOf(info) else info.entity

        if (highlight && sparkling) {
            sparklingEntities.add(target)
            currentZone?.mobMatching(info)?.let { warnOfSparkling(info.entity, it.displayName) }
        } else {
            sparklingEntities.remove(target)
            sparklingEntities.remove(info.entity)
        }

        return target.takeIf { highlight }
    }

    private fun matchesHighlight(info: EntityInfo, sparkling: Boolean): Boolean {
        if (!baseSetting.value) return false
        if (!mobHighlight.value) return false

        val zone = currentZone ?: return false
        val entity = info.entity

        // treasure is placed all over the island, only the grass of the current zone is worth showing
        if (isTreasureDisplay(entity)) {
            return SafariZone.at(entity.position()) == zone
        }

        val mob = zone.mobMatching(info) ?: return false

        // a sparkling stays worth catching after its unique is done, it is far rarer than the unique
        return sparkling || !onlyUncaught.value || !isCaught(mob.displayName)
    }
    
    override fun markOf(info: EntityInfo): EntityUtils.HighlightMark? {
        if (isTreasureDisplay(info.entity)) return null

        val mob = currentZone?.mobMatching(info) ?: return null

        return EntityUtils.HighlightMark(mob.displayName)
    }

    private fun isSparkling(info: EntityInfo): Boolean =
        info.informationEntities?.any {
            it.customName?.string?.contains(SPARKLING_TAG, ignoreCase = true) == true
        } == true



    private fun visiblePartOf(info: EntityInfo): Entity {
        val entity = info.entity
        if (!entity.isInvisible) return entity

        return info.informationEntities?.firstOrNull { EntityUtils.carriedSkullHash(it) != null }
            ?: entity.passengers.firstOrNull { !it.isInvisible }
            ?: entity.vehicle?.takeIf { !it.isInvisible }
            ?: entity
    }

    private fun isTreasureDisplay(entity: Entity): Boolean =
        entity is Display.ItemDisplay && entity.itemStack.item == Items.STRING
}


/*
examples for regex
[CHAT] §a§lCAPTURE! §7You caught a §aTreefrog§7 and gained 2x §aTreefrog Shard§7!
[CHAT] §e§lLOOT SHARE! §7You received 2x §aPolaris Shard§7 from §bAceMech§7 catching a §aPolaris§7!
[CHAT] §a§lCAPTURE! §7You found the §9Hideyho§7, and as a reward it gave you 3x §9Hideyho Shard§7!
[CHAT] §e§lLOOT SHARE! §7You received 3x §9Hideyho Shard§7 from §bMeowMeowLynn§7 finding the §9Hideyho§7!
[CHAT] §a§lCAPTURE! §7You caught a §6SPARKLING §aWoodchucker§7 and received a §5Rainbow Feather§7 and 20x §aWoodchucker Shard§7!
 */
