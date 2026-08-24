package org.magic.magicaddons.features.foraging

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.Items
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.OnSystemChatEvent
import org.magic.magicaddons.events.render.OnHudRenderEvent
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.events.world.OnEntityRemoved
import org.magic.magicaddons.events.world.OnEntityUpdated
import org.magic.magicaddons.events.world.OnWorldTickEvent
import org.magic.magicaddons.features.HighlightFeature
import org.magic.magicaddons.util.ChatUtils
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

    // TODO replace the fixed hud position with a hud position editor
    private const val HUD_X: Int = 20
    private const val HUD_Y: Int = 20

    /** Fallback for hud text that carries no style of its own. */
    private const val HUD_TEXT_COLOR: Int = 0xFFFFFFFF.toInt()

    private const val MOB_HIGHLIGHT_COLOR: Int = 0xFFFFC0CB.toInt()
    private const val SPARKLING_HIGHLIGHT_COLOR: Int = 0xFFFFAA00.toInt()
    private const val TREASURE_HIGHLIGHT_COLOR: Int = 0xFF55FF55.toInt()

    /** Marks the rarer version of a mob, written on the name tag standing next to it. */
    private const val SPARKLING_TAG: String = "sparkling"

    /** The one unique that is worth reporting a run as done without. */
    private const val MACAW: String = "Macaw"

    private val catchPatterns = listOf(
        // "§a§lCAPTURE! §7You caught a §aTreefrog§7 and gained 2x §aTreefrog Shard§7!"
        Regex("You caught an? (.+?) and gained"),
        // "§e§lLOOT SHARE! §7You received a §aPolaris Shard§7 from §bAceMech§7 catching a §aPolaris§7!"
        Regex("catching an? (.+?)!"),
        // hideyho is found instead of caught, it has its own wording for both messages
        // "§a§lCAPTURE! §7You found the §9Hideyho§7, and as a reward it gave you 3x §9Hideyho Shard§7!"
        Regex("You found the (.+?), and as a reward"),
        // "§e§lLOOT SHARE! §7You received 3x §9Hideyho Shard§7 from §bMeowMeowLynn§7 finding the §9Hideyho§7!"
        Regex("finding the (.+?)!")
    )

    override val id: String = "SafariHelper"
    override val displayName: String = "Safari Helper"
    override val tooltipMessage: String = "Helpers for the safari island"
    override val category: String = "foraging"

    // above HighlightMobs so the zone specific coloring wins when both highlight the same entity
    override val highlightPriority: Int = 1

    private val onlyUncaught = BooleanSetting(
        key = "OnlyUncaught",
        displayName = "Only Uncaught",
        tooltip = "Only highlights the uniques that have not been caught yet during this safari visit.",
        value = false
    )

    private val mobHighlight = BooleanSetting(
        key = "MobHighlight",
        displayName = "Mob Highlight",
        tooltip = "Highlights the mobs and the grass treasure belonging to the safari zone you are in.",
        value = false,
        children = listOf(
            onlyUncaught
        )
    )

    private val sendToPartyChat = BooleanSetting(
        key = "SendToPartyChat",
        displayName = "Send To Party Chat",
        tooltip = "Sends the done message to the party chat instead of only to yourself.",
        value = false
    )

    private val ignoreMacaw = BooleanSetting(
        key = "IgnoreMacaw",
        displayName = "Ignore Macaw",
        tooltip = "Adds a second done message for having caught everything except the macaw.",
        value = false
    )

    private val doneMessage = BooleanSetting(
        key = "DoneMessage",
        displayName = "Done Message",
        tooltip = "Sends a message when all unique critters have been caught.",
        value = false,
        children = listOf(
            sendToPartyChat,
            ignoreMacaw
        )
    )

    private val uniqueTracking = BooleanSetting(
        key = "UniqueTracking",
        displayName = "Unique Tracking",
        tooltip = "Shows which unique mobs are still left to catch in the safari zone you are in.",
        value = false,
        children = listOf(
            doneMessage
        )
    )

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        tooltip = tooltipMessage,
        value = false,
        children = listOf(
            mobHighlight,
            uniqueTracking
        )
    )

    /** The zone the player is standing in, or null while not on the safari island. */
    var currentZone: SafariZone? = null
        private set

    /**
     * Uniques caught during this safari visit, lowercased. The server never tells us what was already
     * caught before we arrived, so this starts empty every time the island is entered.
     */
    private val caughtUniques = mutableSetOf<String>()

    /** Both done messages are worth sending once per safari visit. */
    private var doneMessageSent: Boolean = false
    private var doneWithoutMacawMessageSent: Boolean = false

    /**
     * Highlighted entities that the name tag next to them marks as sparkling. The renderer only hands
     * back the entity, so the answer has to be remembered while the entity info is still around.
     */
    private val sparklingEntities = mutableSetOf<Entity>()

    override fun highlightColor(entity: Entity): Int = when {
        entity in sparklingEntities -> SPARKLING_HIGHLIGHT_COLOR
        isTreasureDisplay(entity) -> TREASURE_HIGHLIGHT_COLOR
        else -> MOB_HIGHLIGHT_COLOR
    }

    @EventHandler
    fun onWorldTick(event: OnWorldTickEvent) {
        val zone = if (LocationAPI.island == SkyBlockIsland.SAFARI) {
            Minecraft.getInstance().player?.position()?.let { SafariZone.at(it) }
        } else {
            null
        }

        if (zone != currentZone) {
            currentZone = zone
            invalidateHighlights()
        }
    }

    @EventHandler
    fun onConfigChanged(event: ConfigChangedEvent) {
        invalidateHighlights()
    }

    @EventHandler
    fun onEntityAdded(event: OnEntityAdded) {
        handleEntitiesAdded(event.addedEntityList)
    }

    @EventHandler
    fun onEntityRemoved(event: OnEntityRemoved) {
        event.removedEntityList.forEach { sparklingEntities.remove(it.entity) }
        handleEntitiesRemoved(event.removedEntityList)
    }

    @EventHandler
    fun onEntityUpdated(event: OnEntityUpdated) {
        handleEntitiesUpdated(event.updatedEntityList)
    }

    @Subscription
    fun onIslandChange(event: IslandChangeEvent) {
        // a fresh visit starts with nothing caught, leaving drops the state we can no longer trust
        if (event.new == SkyBlockIsland.SAFARI || event.old == SkyBlockIsland.SAFARI) {
            caughtUniques.clear()
            doneMessageSent = false
            doneWithoutMacawMessageSent = false
        }
    }

    @EventHandler
    fun onSystemChat(event: OnSystemChatEvent) {
        if (!baseSetting.value) return
        if (currentZone == null) return

        // catches are shared with everyone nearby, so any player catching removes the unique
        val caught = catchPatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(event.text)?.groupValues?.get(1)
        } ?: return

        if (!caughtUniques.add(caught.lowercase())) return

        // that mob just stopped being interesting, drop the highlights it no longer deserves
        if (mobHighlight.value && onlyUncaught.value) {
            invalidateHighlights()
        }

        sendDoneMessages()
    }

    private fun sendDoneMessages() {
        if (!uniqueTracking.value || !doneMessage.value) return

        val remaining = SafariZone.entries.flatMap { remainingIn(it) }

        if (remaining.isEmpty()) {
            // catching everything says more than having caught everything but the macaw
            doneWithoutMacawMessageSent = true

            if (!doneMessageSent) {
                doneMessageSent = true
                announceDone("All safari uniques caught")
            }
            return
        }

        if (!ignoreMacaw.value || doneWithoutMacawMessageSent) return
        if (remaining.any { !it.equals(MACAW, ignoreCase = true) }) return

        doneWithoutMacawMessageSent = true
        announceDone("All safari uniques caught except the $MACAW")
    }

    private fun announceDone(message: String) {
        // the party has to be told by the server, the mod prefix has no business in their chat
        if (sendToPartyChat.value) {
            ChatUtils.sendCommand("pc $message")
            return
        }

        ChatUtils.sendWithPrefix(Component.literal(message).withStyle(ChatFormatting.GREEN))
    }

    @EventHandler
    fun onHudRender(event: OnHudRenderEvent) {
        if (!baseSetting.value || !uniqueTracking.value) return

        val zone = currentZone ?: return
        val client = Minecraft.getInstance()

        // only while free walking around the island
        if (client.options.hideGui || client.screen != null) return

        var y = HUD_Y
        hudLines(zone).forEach { line ->
            event.graphics.text(client.font, line, HUD_X, y, HUD_TEXT_COLOR, true)
            y += client.font.lineHeight + 1
        }
    }

    /** The uniques of [zone] that still have to be caught during this visit. */
    fun remainingIn(zone: SafariZone): List<String> =
        zone.uniqueMobs.map { it.displayName }.filterNot { isCaught(it) }

    private fun isCaught(mobName: String): Boolean = mobName.lowercase() in caughtUniques

    private fun hudLines(zone: SafariZone): List<Component> {
        val remaining = remainingIn(zone)

        if (remaining.isNotEmpty()) {
            val header = Component.literal("${zone.displayName} Biome: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("${remaining.size} left").withStyle(ChatFormatting.YELLOW))

            return listOf(header) + remaining.map { mob ->
                Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(mob).withStyle(ChatFormatting.GREEN))
            }
        }

        val unfinishedZones = SafariZone.entries.filter { it != zone && remainingIn(it).isNotEmpty() }

        if (unfinishedZones.isEmpty()) {
            return listOf(Component.literal("All safari uniques caught").withStyle(ChatFormatting.GREEN))
        }

        val header = Component.literal("${zone.displayName} Biome done").withStyle(ChatFormatting.GREEN)
            .append(Component.literal(", biomes left:").withStyle(ChatFormatting.GRAY))

        return listOf(header) + unfinishedZones.map { other ->
            Component.literal(" - ${other.displayName}: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("${remainingIn(other).size} left").withStyle(ChatFormatting.YELLOW))
        }
    }

    override fun shouldHighlight(info: EntityInfo): Boolean {
        val highlight = matchesHighlight(info)

        if (highlight && isSparkling(info)) {
            sparklingEntities.add(info.entity)
        } else {
            sparklingEntities.remove(info.entity)
        }

        return highlight
    }

    private fun matchesHighlight(info: EntityInfo): Boolean {
        if (!baseSetting.value) return false
        if (!mobHighlight.value) return false

        val zone = currentZone ?: return false
        val entity = info.entity

        // treasure is placed all over the island, only the grass of the current zone is worth showing
        if (isTreasureDisplay(entity)) {
            return SafariZone.at(entity.position()) == zone
        }

        val mob = zone.mobMatching(info) ?: return false

        return !onlyUncaught.value || !isCaught(mob.displayName)
    }

    private fun isSparkling(info: EntityInfo): Boolean =
        info.informationEntities?.any {
            it.customName?.string?.contains(SPARKLING_TAG, ignoreCase = true) == true
        } == true

    private fun isTreasureDisplay(entity: Entity): Boolean =
        entity is Display.ItemDisplay && entity.itemStack.item == Items.STRING
}


/*
Messages that must NOT count as a catch:

[CHAT] You threw a Critter Capsule at the Treefrog!
[CHAT] The Treefrog escaped your Critter Capsule!
[CHAT] FLOOR DROP! You found +5,926 Hunting Experience on the ground!
[CHAT] You hear the sound of massive footsteps echoing through the Icy Biome... What could it be?

Catches, all of them can repeat with a " (2)" suffix when the same message is sent twice:

[CHAT] §a§lCAPTURE! §7You caught a §aTreefrog§7 and gained 2x §aTreefrog Shard§7!
[CHAT] §e§lLOOT SHARE! §7You received 2x §aPolaris Shard§7 from §bAceMech§7 catching a §aPolaris§7!
[CHAT] §a§lCAPTURE! §7You found the §9Hideyho§7, and as a reward it gave you 3x §9Hideyho Shard§7!
[CHAT] §e§lLOOT SHARE! §7You received 3x §9Hideyho Shard§7 from §bMeowMeowLynn§7 finding the §9Hideyho§7!
 */
