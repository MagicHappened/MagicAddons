package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.data.greenhouse.Footprint
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils

/**
 * Reads the entities standing around the player.
 *
 * A greenhouse keeps most of what it knows on armor stands that carry nothing but a custom name.
 * They have no hit box worth aiming at, so the mob hit debug cannot reach them, and the name is
 * rendered with formatting that the screen does not show. This dumps them by proximity instead of
 * by aim, with the formatting spelled out, since a bar such as the water level is only readable as
 * the colours its characters are split into.
 */
object FarmingDebug : AbstractCommand() {
    var footprint: Footprint = Footprint(1, 1)

    private const val DEFAULT_RADIUS: Double = 4.0

    override val argument: String = "farming"
    override val description: String = "returns data for greenhouse testing"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                dumpNearbyEntities(DEFAULT_RADIUS)
                return@executes 1
            }
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("stands")
                    .executes {
                        dumpNearbyEntities(DEFAULT_RADIUS)
                        return@executes 1
                    }
                    .then(
                        RequiredArgumentBuilder.argument<FabricClientCommandSource, Double>(
                            "radius",
                            DoubleArgumentType.doubleArg(0.5, 32.0)
                        ).executes {
                            dumpNearbyEntities(DoubleArgumentType.getDouble(it, "radius"))
                            return@executes 1
                        }
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("footprint")
                    .then(
                        RequiredArgumentBuilder.argument<FabricClientCommandSource, String>(
                            "footprint",
                            StringArgumentType.word()
                        ).executes {
                            val stringArg = StringArgumentType.getString(it, "footprint")

                            try {
                                footprint = Footprint(stringArg[0].digitToInt(), stringArg[1].digitToInt())
                            } catch (e: Exception) {
                                ChatUtils.sendWithPrefix("Footprint has to be two digits, such as 22 for a 2x2")
                                return@executes 0
                            }

                            ChatUtils.sendWithPrefix("Footprint is now ${footprint.width}x${footprint.height}")
                            return@executes 1
                        }
                    )
            )
    }

    /** Every named stand and display within [radius] of the player, nearest first. */
    private fun dumpNearbyEntities(radius: Double) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val level = client.level ?: return

        val entities = level.getEntities(player, player.boundingBox.inflate(radius))
            .filter { (it is ArmorStand && it.hasCustomName()) || it is Display }
            .sortedBy { it.distanceToSqr(player) }

        if (entities.isEmpty()) {
            ChatUtils.sendWithPrefix("Nothing named within $radius blocks")
            return
        }

        ChatUtils.sendWithPrefix(
            Component.literal("${entities.size} nearby, within $radius blocks")
                .withStyle(ChatFormatting.GOLD)
        )

        entities.forEach { entity ->
            val offset = entity.position().subtract(player.position())

            ChatUtils.send(
                Component.literal("- ${entity.type.toString().substringAfterLast('.')} ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(
                        Component.literal(
                            "%.2f %.2f %.2f".format(offset.x, offset.y, offset.z)
                        ).withStyle(ChatFormatting.DARK_GRAY)
                    )
            )

            // how a stand is built decides where its head ends up, and a stand rebuilt at the right
            // position but the wrong size puts its skull at the wrong height
            if (entity is ArmorStand) {
                ChatUtils.send(
                    field(
                        "stand",
                        "small=${entity.isSmall} marker=${entity.isMarker} arms=${entity.showArms()}"
                    )
                )
            }

            entity.customName?.let { name ->
                // the name as it draws, so a bar can be counted, and again as the runs of styling
                // it is built from, which is the only way to tell a filled notch from an empty one
                ChatUtils.send(field("name", name.string))
                ChatUtils.send(copyable("runs", describeRuns(name)))
            }

            describeHeldItem(entity)?.let { ChatUtils.send(it) }
        }
    }

    /**
     * The custom name broken into its styled runs, written as `colour:text` for each. A bar that
     * looks like one string on screen is several runs here, which is what makes it readable.
     */
    private fun describeRuns(name: Component): String {
        val runs = mutableListOf<String>()

        name.visit({ style, text ->
            if (text.isNotEmpty()) {
                val color = style.color?.serialize() ?: "none"
                runs.add("$color:$text")
            }
            java.util.Optional.empty<Unit>()
        }, Style.EMPTY)

        return runs.joinToString(" | ")
    }

    /** The skull or item a stand or display carries, which is how most crops are identified. */
    private fun describeHeldItem(entity: Entity): Component? {
        val stack = when (entity) {
            is Display.ItemDisplay -> entity.itemStack
            is ArmorStand -> entity.getItemBySlot(EquipmentSlot.HEAD)
            else -> null
        } ?: return null

        if (stack.isEmpty) return null

        val hash = PlayerUtils.getSkinHash(stack) ?: return field("item", stack.item.toString())

        return copyable("hash", hash)
    }

    private fun field(label: String, value: String): Component =
        Component.literal("  $label: ").withStyle(ChatFormatting.DARK_GRAY)
            .append(Component.literal(value).withStyle(ChatFormatting.WHITE))

    /** A value worth taking out of the game whole, rather than reading off the screen. */
    private fun copyable(label: String, value: String): Component =
        Component.literal("  $label: ").withStyle(ChatFormatting.DARK_GRAY)
            .append(
                Component.literal(value).withStyle(
                    Style.EMPTY
                        .withColor(ChatFormatting.YELLOW)
                        .withClickEvent(ClickEvent.CopyToClipboard(value))
                        .withHoverEvent(HoverEvent.ShowText(Component.literal("Click to copy")))
                )
            )
}
