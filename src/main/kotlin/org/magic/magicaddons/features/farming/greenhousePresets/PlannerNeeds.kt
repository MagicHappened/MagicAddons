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

    /** map of each message a greenhouse sent to not repeat them */
    private val messageMap = mutableMapOf<String, Instant>()

    /** the requested item and its properties for the planner */
    private class RequestedItem(val label: String, val command: String?, val hover: Component)

    /** the line last sent, and how to count it again */
    private class LastPlannerMessage(val count: () -> List<RequestedItem>, var body: Component, var line: Component)

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

    /** Walking back into a greenhouse lets it name what it needs again, 30 seconds after it last did. */
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
        if (blocks.isEmpty() || quiet(grid, "soil")) return

        send(grid, "soil") { soilNeeds(blocks) }
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
        if (crops.isEmpty() || quiet(grid, "plants")) return

        send(grid, "plants") { plantNeeds(crops) }
    }

    private fun plantNeeds(crops: Map<CropDefinition, Int>): List<RequestedItem> = crops.mapNotNull { (def, count) ->
        val wheat = def.skyblockId == SkyBlockItemId.item("WHEAT")
        val label = if (wheat) "Seeds" else def.name
        val id = if (wheat) SEEDS else def.skyblockId
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

        messageMap["${grid.layout.id}|$phase"] = Instant.now()
        val body = line(needs)
        lastMessage = LastPlannerMessage(count, body, ChatUtils.sendWithPrefix(body))
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
