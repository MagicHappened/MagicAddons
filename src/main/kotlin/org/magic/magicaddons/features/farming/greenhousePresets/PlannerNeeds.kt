package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.commands.internal.MainInternal
import org.magic.magicaddons.commands.internal.farming.GetPlannerItemCommand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.SystemChatEvent
import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.compat.McCompat
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId
import java.time.Duration
import java.time.Instant
import org.magic.magicaddons.features.farming.greenhousePresets.lookups.StorageBridge

/**
 * Names what a planner still needs as clickable items. A click runs the command and moves the line
 * to the bottom of chat; the line is recounted once the sacks answer or the opened menu closes.
 */
object PlannerNeeds {

    /** cooldown for the plants / soils qol message. */
    private val COOLDOWN: Duration = Duration.ofSeconds(30)

    /** how long a click waits for an answer */
    private val ANSWER_WINDOW: Duration = Duration.ofSeconds(30)

    private val INVENTORY_RECOUNT_DELAY: Duration = Duration.ofMillis(500)

    private const val BUILDER_COMMAND: String = "/call builder"


    private val ON_COOLDOWN_REGEX: Regex = Regex(".*This command is on cooldown.*")

    /** response regexes for a gfs command */
    private val SACKS_ANSWER_REGEXES: List<Regex> = listOf(
        Regex("Moved [\\d,]+ .+ from your Sacks to your inventory\\."),
        Regex("You have no .+ in your Sacks!")
    )

    private val NOT_IN_SACKS: Set<Block> = setOf(Blocks.DIRT, Blocks.NETHERRACK, Blocks.SOUL_SAND)

    private val SEEDS: SkyBlockId = SkyBlockItemId.item("SEEDS")

    /**
     * A crop put down as a seed rather than as itself: the seed it takes, what the sacks give for
     * it, how many seeds one of those makes, and the sack's name for it. A pumpkin makes four.
     */
    private class Seeding(val seed: SkyBlockId, val source: SkyBlockId, val seedsPerSource: Int, val sackName: String)

    private val SEEDINGS: Map<SkyBlockId, Seeding> = mapOf(
        SkyBlockItemId.item("WHEAT") to Seeding(SEEDS, SEEDS, 1, "seeds"),
        SkyBlockItemId.item("PUMPKIN") to Seeding(SkyBlockItemId.item("PUMPKIN_SEEDS"), SkyBlockItemId.item("PUMPKIN"), 4, "pumpkin"),
        SkyBlockItemId.item("MELON") to Seeding(SkyBlockItemId.item("MELON_SEEDS"), SkyBlockItemId.item("MELON"), 1, "melon")
    )

    /** map of each message a greenhouse sent to not repeat them */
    private val messageMap = mutableMapOf<String, Instant>()

    /** the requested item and its properties for the planner */
    private class RequestedItem(val label: String, val command: String?, val hover: Component)

    /** the line last sent, which greenhouse and phase it is for, and how to count it again */
    private class LastPlannerMessage(val key: String, var count: () -> List<RequestedItem>, var body: Component, var line: Component)

    /** what to wait for before recounting */
    private enum class Answer { SACKS, MENU }

    private var lastMessage: LastPlannerMessage? = null
    private var awaiting: Answer? = null
    private var awaitingSince: Instant? = null

    private var resendMessageAt: Instant? = null

    private var menuOpenedFromClick: Boolean = false

    private var standingInGreenhouse: String? = null

    fun forgetSentMessage(grid: GreenhouseGrid) {
        messageMap.keys.removeAll { it.startsWith("${grid.layout.id}|") }
    }

    fun arriveAt(grid: GreenhouseGrid?) {
        val id = grid?.layout?.id
        if (standingInGreenhouse == id) return

        standingInGreenhouse = id
        if (id == null) return

        val now = Instant.now()
        messageMap.keys.removeAll { it.startsWith("$id|") && now.isAfter(messageMap.getValue(it).plus(COOLDOWN)) }
    }

    /** Names the soil still to place, once per visit to the greenhouse. */
    fun tellSoil(grid: GreenhouseGrid, blocks: Map<Block, Int>) {
        val count = { soilNeeds(blocks) }

        if (quiet(grid, "soil")) {
            if (blocks.isEmpty()) retractLine(grid, "soil") else refreshCount(grid, "soil", count)
            return
        }
        if (blocks.isEmpty()) return

        send(grid, "soil", count)
    }

    private fun soilNeeds(blocks: Map<Block, Int>): List<RequestedItem> = blocks.mapNotNull { (block, count) ->
        val asked = if (block == Blocks.FARMLAND) Blocks.DIRT else block
        val label = asked.name.string
        val left = count - heldBlocks(asked)
        val name = label.lowercase()

        when {
            left <= 0 -> null
            else -> fromStorage(label, left) { it.item == asked.asItem() }
                ?: if (asked in NOT_IN_SACKS) {
                    RequestedItem(label, BUILDER_COMMAND, Component.literal("Click here to buy $left $name from the Builder!"))
                } else {
                    RequestedItem(label, "/gfs $name $left", sackHover(left, name))
                }
        }
    }

    /** Names the plants still to put down, once per visit to the greenhouse. */
    fun tellPlants(grid: GreenhouseGrid, crops: Map<CropDefinition, Int>) {
        val count = { plantNeeds(crops) }

        // the soil is all down once plants are asked for, so its line has nothing left to name
        retractLine(grid, "soil")

        if (quiet(grid, "plants")) {
            if (crops.isEmpty()) retractLine(grid, "plants") else refreshCount(grid, "plants", count)
            return
        }
        if (crops.isEmpty()) return

        send(grid, "plants", count)
    }

    private fun plantNeeds(crops: Map<CropDefinition, Int>): List<RequestedItem> = crops.mapNotNull { (def, count) ->
        val seeding = def.skyblockId?.let { SEEDINGS[it] }

        if (seeding != null) return@mapNotNull seedNeed(def, count, seeding)

        val label = def.name
        val id = def.skyblockId
        val left = count - (id?.let { heldItems(it) } ?: 0)
        val name = label.lowercase()

        when {
            left <= 0 -> null
            else -> fromStorage(label, left) { id != null && it.getSkyBlockId() == id }
                ?: if (id == null) {
                    RequestedItem(label, null, unbuyableHover(label, left))
                } else {
                    RequestedItem(label, "/gfs $name $left", sackHover(left, name))
                }
        }
    }

    /**
     * The need for a crop put down as a seed: what is held as seeds counts as is, what is held as
     * the thing the seeds are made from counts for as many seeds as it makes, and the sacks are
     * asked for just enough of that thing to make the rest.
     */
    private fun seedNeed(def: CropDefinition, count: Int, seeding: Seeding): RequestedItem? {
        val held = heldItems(seeding.seed) + heldItems(seeding.source) * seeding.seedsPerSource
        val left = count - held
        if (left <= 0) return null

        val label = if (seeding.seed == seeding.source) "Seeds" else "${def.name} Seeds"
        val sources = (left + seeding.seedsPerSource - 1) / seeding.seedsPerSource

        return fromStorage(label, left) { it.getSkyBlockId() == seeding.seed || it.getSkyBlockId() == seeding.source }
            ?: RequestedItem(label, "/gfs ${seeding.sackName} $sources", seedSackHover(left, sources, seeding))
    }

    /** The sack hover for a seed need, saying what the sacks give and what that makes when they differ. */
    private fun seedSackHover(seeds: Int, sources: Int, seeding: Seeding): Component =
        if (seeding.seedsPerSource == 1 && seeding.seed == seeding.source) sackHover(seeds, seeding.sackName)
        else Component.literal("Click here to get $sources ${seeding.sackName} from sacks, for $seeds seeds!")

    /** the need pointed at storage, when its pages hold enough */
    private fun fromStorage(label: String, left: Int, matches: (ItemStack) -> Boolean): RequestedItem? {
        val pages = StorageBridge.pagesHolding(matches)
        if (pages.sumOf { it.count } < left) return null

        val hover = Component.literal("Click here to open your storage for $left ${label.lowercase()}!")
        pages.forEach { hover.append(Component.literal("\n - ${it.page}: ${it.count}").withStyle(ChatFormatting.GRAY)) }

        return RequestedItem(label, StorageBridge.openStorageCommand(label), hover)
    }

    /** Whether this greenhouse has already named this phase since the player walked into it. */
    private fun quiet(grid: GreenhouseGrid, phase: String): Boolean =
        messageMap.containsKey("${grid.layout.id}|$phase")

    private fun send(grid: GreenhouseGrid, phase: String, count: () -> List<RequestedItem>) {
        val needs = count()
        if (needs.isEmpty()) return

        val key = "${grid.layout.id}|$phase"
        messageMap[key] = Instant.now()
        val body = line(needs)
        lastMessage = LastPlannerMessage(key, count, body, ChatUtils.sendWithPrefix(body))
    }

    /**
     * The plot changed under a line that is up: the next recount, which only a click brings, counts
     * against what stands now rather than what stood when the line was sent. Nothing is redrawn
     * here, so the line does not move on every plant put down.
     */
    /** Takes the line out of chat once nothing it names is left to get. */
    private fun retractLine(grid: GreenhouseGrid, phase: String) {
        val sent = lastMessage ?: return
        if (sent.key != "${grid.layout.id}|$phase") return

        ChatUtils.retract(sent.line)
        lastMessage = null
    }

    private fun refreshCount(grid: GreenhouseGrid, phase: String, count: () -> List<RequestedItem>) {
        val sent = lastMessage ?: return
        if (sent.key != "${grid.layout.id}|$phase") return

        sent.count = count
    }

    /** Runs the command behind a clicked item and takes the line out of chat until the game has answered. */
    fun clicked(command: String) {
        ChatUtils.sendCommand(command.removePrefix("/"))

        lastMessage?.let { ChatUtils.retract(it.line) }

        awaiting = if (command.startsWith("/gfs")) Answer.SACKS else Answer.MENU
        awaitingSince = Instant.now()
        resendMessageAt = null
        menuOpenedFromClick = false
    }

    /** sends the last line again at the bottom, recounted when asked */
    private fun resend(recount: Boolean) {
        val sent = lastMessage ?: return
        ChatUtils.retract(sent.line)

        if (recount) {
            val needs = sent.count()
            if (needs.isEmpty()) {
                lastMessage = null
                return
            }
            sent.body = line(needs)
        }

        sent.line = ChatUtils.sendWithPrefix(sent.body)
    }

    /** The game has answered, so the line goes back up once the inventory has caught up. */
    private fun answered() {
        awaiting = null
        awaitingSince = null
        resendMessageAt = Instant.now().plus(INVENTORY_RECOUNT_DELAY)
    }

    @EventHandler
    fun onSystemChat(event: SystemChatEvent) {
        if (awaiting == null || event.overlay) return

        val text = event.text.trim()

        // the command never ran, so the line goes straight back up for another try
        if (ON_COOLDOWN_REGEX.matches(text)) {
            answered()
            return
        }

        if (awaiting != Answer.SACKS) return
        if (SACKS_ANSWER_REGEXES.none { it.matches(text) }) return

        answered()
    }

    /** recounts once the sacks have answered, or the Builder or storage menu closes */
    @EventHandler
    fun onWorldTick(event: WorldTickEvent) {
        val now = Instant.now()

        resendMessageAt?.let { at ->
            if (now.isAfter(at)) {
                resendMessageAt = null
                resend(recount = true)
            }
            return
        }

        val since = awaitingSince ?: return

        // nothing answered, so the line goes back up as it was rather than staying gone
        if (now.isAfter(since.plus(ANSWER_WINDOW))) {
            answered()
            return
        }
        if (awaiting != Answer.MENU) return

        val menuOpen = McCompat.currentScreen() is AbstractContainerScreen<*>
        if (menuOpen) {
            menuOpenedFromClick = true
        } else if (menuOpenedFromClick) {
            answered()
        }
    }

    private fun line(requestedItems: List<RequestedItem>): Component {
        val line = Component.literal("Click to get: ").withStyle(ChatFormatting.GRAY)
        requestedItems.forEachIndexed { index, need ->
            if (index > 0) line.append(Component.literal(" "))
            line.append(entry(need))
        }
        return line
    }

    /** One name in brackets, clickable unless there is nowhere to get the thing from. */
    private fun entry(requestedItem: RequestedItem): Component {
        val text = Component.literal("[${requestedItem.label}]")
        val command = requestedItem.command
            ?: return text.withStyle(
                Style.EMPTY
                    .withColor(ChatFormatting.GRAY)
                    .withHoverEvent(HoverEvent.ShowText(requestedItem.hover))
            )

        return text.withStyle(
            Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(ClickEvent.RunCommand("${MainInternal.COMMAND} ${GetPlannerItemCommand.NAME} $command"))
                .withHoverEvent(HoverEvent.ShowText(requestedItem.hover))
        )
    }

    private fun sackHover(amount: Int, name: String): Component =
        Component.literal("Click here to get $amount $name from sacks!")

    /** The hover on something with nowhere to get it: how many are wanted, and why there is no click. */
    private fun unbuyableHover(label: String, amount: Int): Component = Component.literal("$label x$amount")
        .append(Component.literal("\n"))
        .append(
            Component.literal("$label is not in sacks.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
        )

    private fun inventory(): List<ItemStack> {
        val player = Minecraft.getInstance().player ?: return emptyList()
        val inventory = player.inventory
        return (0 until inventory.containerSize).map { inventory.getItem(it) }
    }

    private fun heldBlocks(block: Block): Int {
        val item = block.asItem()
        return inventory().filter { it.item == item }.sumOf { it.count }
    }

    private fun heldItems(id: SkyBlockId): Int =
        inventory().filter { !it.isEmpty && it.getSkyBlockId() == id }.sumOf { it.count }
}
