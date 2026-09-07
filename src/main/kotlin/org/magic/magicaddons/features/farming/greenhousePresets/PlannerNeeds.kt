package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId
import java.time.Duration
import java.time.Instant

/** Names what a planner still needs, as items the player clicks to get from their sacks or the Builder. */
object PlannerNeeds {

    /** How soon after naming a phase a greenhouse may name it again, once it is walked back into. */
    private val COOLDOWN: Duration = Duration.ofSeconds(30)

    /** Where the non-sack blocks are bought instead. */
    private const val BUILDER: String = "/call builder"

    /** Soil no sack holds, so it is bought instead. Farmland is asked for as the dirt to till. */
    private val NOT_IN_SACKS: Set<Block> = setOf(Blocks.DIRT, Blocks.NETHERRACK, Blocks.SOUL_SAND)

    /** Wheat is planted from seeds, which is what the sack holds. */
    private val SEEDS: SkyBlockId = SkyBlockItemId.item("SEEDS")

    /** When each greenhouse last named what a phase needs, by greenhouse and phase. */
    private val told = mutableMapOf<String, Instant>()

    /** One thing to get: how it is written, the command that gets it, and what the hover says. */
    private class Need(val label: String, val command: String?, val hover: Component)

    /** Which greenhouse the player is in, so returning to one is told apart from staying in it. */
    private var standingIn: String? = null

    /** Lets a greenhouse speak again at once, for when it is given a new plan to run. */
    fun forget(grid: GreenhouseGrid) {
        told.keys.removeAll { it.startsWith("${grid.layout.id}|") }
    }

    /** Walking back into a greenhouse lets it name what it needs again, 30 seconds after it last did. */
    fun arriveAt(grid: GreenhouseGrid?) {
        val id = grid?.layout?.id
        if (standingIn == id) return

        standingIn = id
        if (id == null) return

        val now = Instant.now()
        told.keys.removeAll { it.startsWith("$id|") && now.isAfter(told.getValue(it).plus(COOLDOWN)) }
    }

    /** Names the soil still to place, once per visit to the greenhouse. */
    fun tellSoil(grid: GreenhouseGrid, blocks: Map<Block, Int>) {
        if (blocks.isEmpty() || quiet(grid, "soil")) return

        val needs = blocks.mapNotNull { (block, count) ->
            val asked = if (block == Blocks.FARMLAND) Blocks.DIRT else block
            val label = asked.name.string
            val left = count - heldBlocks(asked)
            val name = label.lowercase()

            when {
                left <= 0 -> null
                asked in NOT_IN_SACKS -> Need(
                    label,
                    BUILDER,
                    Component.literal("Click here to buy $left $name from the Builder!")
                )
                else -> Need(label, "/gfs $name $left", sackHover(left, name))
            }
        }

        send(grid, "soil", needs)
    }

    /** Names the plants still to put down, once per visit to the greenhouse. */
    fun tellPlants(grid: GreenhouseGrid, crops: Map<CropDefinition, Int>) {
        if (crops.isEmpty() || quiet(grid, "plants")) return

        val needs = crops.mapNotNull { (def, count) ->
            val wheat = def.skyblockId == SkyBlockItemId.item("WHEAT")
            val label = if (wheat) "Seeds" else def.name
            val id = if (wheat) SEEDS else def.skyblockId
            val left = count - (id?.let { heldItems(it) } ?: 0)
            val name = label.lowercase()

            when {
                left <= 0 -> null
                id == null -> Need(label, null, unbuyableHover(label, left))
                else -> Need(label, "/gfs $name $left", sackHover(left, name))
            }
        }

        send(grid, "plants", needs)
    }

    /** Whether this greenhouse has already named this phase since the player walked into it. */
    private fun quiet(grid: GreenhouseGrid, phase: String): Boolean =
        told.containsKey("${grid.layout.id}|$phase")

    private fun send(grid: GreenhouseGrid, phase: String, needs: List<Need>) {
        if (needs.isEmpty()) return

        told["${grid.layout.id}|$phase"] = Instant.now()

        val line = Component.literal("Click to get: ").withStyle(ChatFormatting.GRAY)
        needs.forEachIndexed { index, need ->
            if (index > 0) line.append(Component.literal(" "))
            line.append(entry(need))
        }

        ChatUtils.sendWithPrefix(line)
    }

    /** One name in brackets, clickable unless there is nowhere to get the thing from. */
    private fun entry(need: Need): Component {
        val text = Component.literal("[${need.label}]")
        val command = need.command
            ?: return text.withStyle(
                Style.EMPTY
                    .withColor(ChatFormatting.GRAY)
                    .withHoverEvent(HoverEvent.ShowText(need.hover))
            )

        return text.withStyle(
            Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(ClickEvent.RunCommand(command))
                .withHoverEvent(HoverEvent.ShowText(need.hover))
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
