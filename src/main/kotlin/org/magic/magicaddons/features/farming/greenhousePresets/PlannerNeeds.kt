package org.magic.magicaddons.features.farming.greenhousePresets

import java.time.Duration
import java.time.Instant
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.commands.internal.MainInternal
import org.magic.magicaddons.commands.internal.farming.GetPlannerItemCommand
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.SystemChatEvent
import org.magic.magicaddons.events.greenhouse.PlotChangedEvent
import org.magic.magicaddons.events.world.WorldTickEvent
import org.magic.magicaddons.features.farming.greenhousePresets.lookups.EnhancedStorageBridge
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.compat.McCompat
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object PlannerNeeds {

    private val MESSAGE_COOLDOWN: Duration = Duration.ofSeconds(30)

    private val COMMAND_ANSWER_TIMEOUT: Duration = Duration.ofSeconds(30)

    private val INVENTORY_RECOUNT_DELAY: Duration = Duration.ofMillis(500)

    private const val BUILDER_COMMAND: String = "/call builder"

    private val COMMAND_ON_COOLDOWN_REGEX: Regex = Regex(".*This command is on cooldown.*")

    private val SACKS_ANSWER_REGEXES: List<Regex> = listOf(
        Regex("Moved [\\d,]+ .+ from your Sacks to your inventory\\."),
        Regex("You have no .+ in your Sacks!")
    )

    private val BLOCKS_NOT_IN_SACKS: Set<Block> = setOf(Blocks.DIRT, Blocks.NETHERRACK, Blocks.SOUL_SAND)

    private val SEEDS_ID: SkyBlockId = SkyBlockItemId.item("SEEDS")

    private class SeedRecipe(val seedId: SkyBlockId, val sackItemId: SkyBlockId, val seedsPerSackItem: Int, val sackName: String)

    private val SEED_RECIPE_BY_CROP_ID: Map<SkyBlockId, SeedRecipe> = mapOf(
        SkyBlockItemId.item("WHEAT") to SeedRecipe(SEEDS_ID, SEEDS_ID, 1, "seeds"),
        SkyBlockItemId.item("PUMPKIN") to SeedRecipe(SkyBlockItemId.item("PUMPKIN_SEEDS"), SkyBlockItemId.item("PUMPKIN"), 4, "pumpkin"),
        SkyBlockItemId.item("MELON") to SeedRecipe(SkyBlockItemId.item("MELON_SEEDS"), SkyBlockItemId.item("MELON"), 1, "melon")
    )

    private enum class NeedsPhase { Soil, Plants }

    private val sentAtByLineKey = mutableMapOf<String, Instant>()

    private class RequestedItem(val label: String, val command: String?, val hoverText: Component)

    private class SentNeedsLine(val key: String, var recountNeeds: () -> List<RequestedItem>, var body: Component, var line: Component)

    private enum class RecountTrigger { SacksMessage, MenuClosed }

    private var sentNeedsLine: SentNeedsLine? = null
    private var pendingRecount: RecountTrigger? = null
    private var pendingRecountSince: Instant? = null

    private var resendLineAt: Instant? = null

    private var isMenuOpenedFromClick: Boolean = false

    private fun lineKeyOf(grid: GreenhouseGrid, phase: NeedsPhase): String = "${grid.layout.id}|$phase"

    fun forgetSentLines(grid: GreenhouseGrid) {
        sentAtByLineKey.keys.removeAll { it.startsWith("${grid.layout.id}|") }
    }

    @EventHandler
    fun onPlotChanged(event: PlotChangedEvent) {
        val layoutId = event.new?.let { PlotLayout.plotId(it.id) } ?: return

        val now = Instant.now()
        sentAtByLineKey.keys.removeAll { it.startsWith("$layoutId|") && now.isAfter(sentAtByLineKey.getValue(it).plus(MESSAGE_COOLDOWN)) }
    }

    fun sendSoilNeeds(grid: GreenhouseGrid, blocks: Map<Block, Int>) {
        val recountNeeds = { soilRequests(blocks) }

        if (isLineAlreadySent(grid, NeedsPhase.Soil)) {
            if (blocks.isEmpty()) retractLine(grid, NeedsPhase.Soil) else refreshCount(grid, NeedsPhase.Soil, recountNeeds)
            return
        }
        if (blocks.isEmpty()) return

        sendMissingItems(grid, NeedsPhase.Soil, recountNeeds)
    }

    private fun soilRequests(blocks: Map<Block, Int>): List<RequestedItem> = blocks.mapNotNull { (block, neededCount) ->
        val blockToGet = if (block == Blocks.FARMLAND) Blocks.DIRT else block
        val label = blockToGet.name.string
        val missingCount = neededCount - heldBlockCount(blockToGet)
        val lowercaseName = label.lowercase()

        when {
            missingCount <= 0 -> null
            else -> storageRequest(label, missingCount) { it.item == blockToGet.asItem() }
                ?: if (blockToGet in BLOCKS_NOT_IN_SACKS) {
                    RequestedItem(label, BUILDER_COMMAND, Component.literal("Click here to buy $missingCount $lowercaseName from the Builder!"))
                } else {
                    RequestedItem(label, "/gfs $lowercaseName $missingCount", sackHover(missingCount, lowercaseName))
                }
        }
    }

    fun sendPlantNeeds(grid: GreenhouseGrid, crops: Map<CropDefinition, Int>) {
        val recountNeeds = { plantRequests(crops) }

        retractLine(grid, NeedsPhase.Soil)

        if (isLineAlreadySent(grid, NeedsPhase.Plants)) {
            if (crops.isEmpty()) retractLine(grid, NeedsPhase.Plants) else refreshCount(grid, NeedsPhase.Plants, recountNeeds)
            return
        }
        if (crops.isEmpty()) return

        sendMissingItems(grid, NeedsPhase.Plants, recountNeeds)
    }

    private fun plantRequests(crops: Map<CropDefinition, Int>): List<RequestedItem> = crops.mapNotNull { (crop, neededCount) ->
        val seedRecipe = crop.skyblockId?.let { SEED_RECIPE_BY_CROP_ID[it] }

        if (seedRecipe != null) return@mapNotNull seedRequest(crop, neededCount, seedRecipe)

        val label = crop.name
        val missingCount = neededCount - heldCropCount(crop)
        val lowercaseName = label.lowercase()

        when {
            missingCount <= 0 -> null
            else -> storageRequest(label, missingCount) { cropOfStack(it) == crop }
                ?: if (crop.skyblockId == null) {
                    RequestedItem(label, null, unbuyableHover(label, missingCount))
                } else {
                    RequestedItem(label, "/gfs $lowercaseName $missingCount", sackHover(missingCount, lowercaseName))
                }
        }
    }

    private fun seedRequest(crop: CropDefinition, neededCount: Int, seedRecipe: SeedRecipe): RequestedItem? {
        val heldSeedCount = heldItemCount(seedRecipe.seedId) + heldItemCount(seedRecipe.sackItemId) * seedRecipe.seedsPerSackItem
        val missingCount = neededCount - heldSeedCount
        if (missingCount <= 0) return null

        val label = if (seedRecipe.seedId == seedRecipe.sackItemId) "Seeds" else "${crop.name} Seeds"
        val sackItemsNeeded = (missingCount + seedRecipe.seedsPerSackItem - 1) / seedRecipe.seedsPerSackItem

        return storageRequest(label, missingCount) { it.getSkyBlockId() == seedRecipe.seedId || it.getSkyBlockId() == seedRecipe.sackItemId }
            ?: RequestedItem(label, "/gfs ${seedRecipe.sackName} $sackItemsNeeded", seedSackHover(missingCount, sackItemsNeeded, seedRecipe))
    }

    private fun seedSackHover(seeds: Int, sackItemsNeeded: Int, seedRecipe: SeedRecipe): Component =
        if (seedRecipe.seedsPerSackItem == 1 && seedRecipe.seedId == seedRecipe.sackItemId) sackHover(seeds, seedRecipe.sackName)
        else Component.literal("Click here to get $sackItemsNeeded ${seedRecipe.sackName} from sacks, for $seeds seeds!")

    private fun storageRequest(label: String, missingCount: Int, isWantedItem: (ItemStack) -> Boolean): RequestedItem? {
        val pages = EnhancedStorageBridge.pagesHolding(isWantedItem)
        if (pages.sumOf { it.count } < missingCount) return null

        val hoverText = Component.literal("Click here to open your storage for $missingCount ${label.lowercase()}!")
        pages.forEach { hoverText.append(Component.literal("\n - ${it.pageName}: ${it.count}").withStyle(ChatFormatting.GRAY)) }

        return RequestedItem(label, EnhancedStorageBridge.storageSearchCommand(label), hoverText)
    }

    private fun isLineAlreadySent(grid: GreenhouseGrid, phase: NeedsPhase): Boolean =
        sentAtByLineKey.containsKey(lineKeyOf(grid, phase))

    private fun sendMissingItems(grid: GreenhouseGrid, phase: NeedsPhase, recountNeeds: () -> List<RequestedItem>) {
        val needs = recountNeeds()
        if (needs.isEmpty()) return

        val key = lineKeyOf(grid, phase)
        sentAtByLineKey[key] = Instant.now()
        val body = requestedItemsLine(needs)
        sentNeedsLine = SentNeedsLine(key, recountNeeds, body, ChatUtils.sendWithPrefix(body))
    }

    private fun retractLine(grid: GreenhouseGrid, phase: NeedsPhase) {
        val sent = sentNeedsLine ?: return
        if (sent.key != lineKeyOf(grid, phase)) return

        ChatUtils.retract(sent.line)
        sentNeedsLine = null
    }

    private fun refreshCount(grid: GreenhouseGrid, phase: NeedsPhase, recountNeeds: () -> List<RequestedItem>) {
        val sent = sentNeedsLine ?: return
        if (sent.key != lineKeyOf(grid, phase)) return

        sent.recountNeeds = recountNeeds
    }

    fun onRequestClicked(command: String) {
        ChatUtils.sendCommand(command.removePrefix("/"))

        sentNeedsLine?.let { ChatUtils.retract(it.line) }

        pendingRecount = if (command.startsWith("/gfs")) RecountTrigger.SacksMessage else RecountTrigger.MenuClosed
        pendingRecountSince = Instant.now()
        resendLineAt = null
        isMenuOpenedFromClick = false
    }

    private fun resendLine(recount: Boolean) {
        val sent = sentNeedsLine ?: return
        ChatUtils.retract(sent.line)

        if (recount) {
            val needs = sent.recountNeeds()
            if (needs.isEmpty()) {
                sentNeedsLine = null
                return
            }
            sent.body = requestedItemsLine(needs)
        }

        sent.line = ChatUtils.sendWithPrefix(sent.body)
    }

    private fun onCommandAnswered() {
        pendingRecount = null
        pendingRecountSince = null
        resendLineAt = Instant.now().plus(INVENTORY_RECOUNT_DELAY)
    }

    @EventHandler
    fun onSystemChat(event: SystemChatEvent) {
        if (pendingRecount == null || event.overlay) return

        val text = event.text.trim()

        if (COMMAND_ON_COOLDOWN_REGEX.matches(text)) {
            onCommandAnswered()
            return
        }

        if (pendingRecount != RecountTrigger.SacksMessage) return
        if (SACKS_ANSWER_REGEXES.none { it.matches(text) }) return

        onCommandAnswered()
    }

    @EventHandler
    fun onWorldTick(event: WorldTickEvent) {
        val now = Instant.now()

        resendLineAt?.let { at ->
            if (now.isAfter(at)) {
                resendLineAt = null
                resendLine(recount = true)
            }
            return
        }

        val since = pendingRecountSince ?: return

        if (now.isAfter(since.plus(COMMAND_ANSWER_TIMEOUT))) {
            onCommandAnswered()
            return
        }
        if (pendingRecount != RecountTrigger.MenuClosed) return

        val isMenuOpen = McCompat.currentScreen() is AbstractContainerScreen<*>
        if (isMenuOpen) {
            isMenuOpenedFromClick = true
        } else if (isMenuOpenedFromClick) {
            onCommandAnswered()
        }
    }

    private fun requestedItemsLine(requestedItems: List<RequestedItem>): Component {
        val line = Component.literal("Click to get: ").withStyle(ChatFormatting.GRAY)
        requestedItems.forEachIndexed { index, requestedItem ->
            if (index > 0) line.append(Component.literal(" "))
            line.append(requestedItemButton(requestedItem))
        }
        return line
    }

    private fun requestedItemButton(requestedItem: RequestedItem): Component {
        val label = "[${requestedItem.label}]"
        val command = requestedItem.command
            ?: return ChatUtils.buildStyled(label, ChatFormatting.GRAY, requestedItem.hoverText)

        return ChatUtils.buildStyled(
            label,
            ChatFormatting.AQUA,
            requestedItem.hoverText,
            ClickEvent.RunCommand("${MainInternal.COMMAND} ${GetPlannerItemCommand.NAME} $command"),
        )
    }

    private fun sackHover(amount: Int, name: String): Component =
        Component.literal("Click here to get $amount $name from sacks!")

    private fun unbuyableHover(label: String, amount: Int): Component = Component.literal("$label x$amount")
        .append(Component.literal("\n"))
        .append(
            Component.literal("$label is not in sacks.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
        )

    private fun inventoryStacks(): List<ItemStack> {
        val player = Minecraft.getInstance().player ?: return emptyList()
        val inventory = player.inventory
        return (0 until inventory.containerSize).map { inventory.getItem(it) }
    }

    private fun heldBlockCount(block: Block): Int {
        val item = block.asItem()
        return inventoryStacks().filter { it.item == item }.sumOf { it.count }
    }

    private fun heldItemCount(id: SkyBlockId): Int =
        inventoryStacks().filter { !it.isEmpty && it.getSkyBlockId() == id }.sumOf { it.count }

    private fun cropOfStack(stack: ItemStack): CropDefinition? =
        if (stack.isEmpty) null else stack.getSkyBlockId()?.id?.let { CropRegistry.findByIdOrName(it) }

    private fun heldCropCount(crop: CropDefinition): Int =
        inventoryStacks().filter { cropOfStack(it) == crop }.sumOf { it.count }
}
