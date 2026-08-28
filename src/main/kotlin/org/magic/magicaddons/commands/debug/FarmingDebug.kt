package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import tech.thatgravyboat.skyblockapi.api.profile.hunting.AttributeAPI
import org.magic.magicaddons.render.WorldRender
import java.time.Duration
import java.time.Instant
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Interaction
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.data.greenhouse.Footprint
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.LayoutRenderState
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

    /** How long a dump leaves its stands lit up for. */
    private val HIGHLIGHT_TIME: Duration = Duration.ofSeconds(30)

    /**
     * A stand tells you how it is built by the colour it is lit in, since what is on screen is one
     * head and what is in the world may be several stands holding it up.
     */
    private const val SMALL_COLOR: Int = 0xFF33FF66.toInt()
    private const val FULL_COLOR: Int = 0xFFFF9922.toInt()
    private const val MARKER_COLOR: Int = 0xFFAA44EE.toInt()

    private const val HIGHLIGHT_ALPHA: Int = 0x50

    /** What the last dump found, lit up in the world so it can be counted by eye. */
    @Volatile
    private var highlighted: List<Pair<AABB, Int>> = emptyList()

    @Volatile
    private var highlightUntil: Instant? = null

    /** Draws the boxes of whatever the last dump listed, from the frame's own render pass. */
    fun submitHighlights(poseStack: PoseStack, collector: SubmitNodeCollector, cameraPos: Vec3) {
        val until = highlightUntil ?: return

        if (Instant.now().isAfter(until)) {
            highlighted = emptyList()
            highlightUntil = null
            return
        }

        highlighted.forEach { (box, color) ->
            WorldRender.markBox(poseStack, collector, cameraPos, box, color, HIGHLIGHT_ALPHA)
        }
    }

    override val argument: String = "farming"
    override val description: String = "returns data for greenhouse testing"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                dumpNearbyEntities(DEFAULT_RADIUS, false)
                return@executes 1
            }
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("entities")
                    .executes {
                        dumpNearbyEntities(DEFAULT_RADIUS, false)
                        return@executes 1
                    }
                    .then(
                        RequiredArgumentBuilder.argument<FabricClientCommandSource, Boolean>(
                            "holograms",
                            BoolArgumentType.bool()
                        ).executes {
                            dumpNearbyEntities(
                                DEFAULT_RADIUS,
                                BoolArgumentType.getBool(it, "holograms")
                            )
                            return@executes 1
                        }.then(
                            RequiredArgumentBuilder.argument<FabricClientCommandSource, Double>(
                                "radius",
                                DoubleArgumentType.doubleArg(0.5, 32.0)
                            ).executes {
                                dumpNearbyEntities(
                                    DoubleArgumentType.getDouble(it, "radius"),
                                    BoolArgumentType.getBool(it, "holograms")
                                )
                                return@executes 1
                            }
                        )
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("entitiesAll")
                    .executes {
                        dumpEverything(DEFAULT_RADIUS)
                        return@executes 1
                    }
                    .then(
                        RequiredArgumentBuilder.argument<FabricClientCommandSource, Double>(
                            "radius",
                            DoubleArgumentType.doubleArg(0.5, 32.0)
                        ).executes {
                            dumpEverything(DoubleArgumentType.getDouble(it, "radius"))
                            return@executes 1
                        }
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("collect")
                    .executes {
                        CropCollector.scan()
                        return@executes 1
                    }
                    .then(
                        LiteralArgumentBuilder.literal<FabricClientCommandSource>("finish")
                            .executes {
                                CropCollector.finish()
                                return@executes 1
                            }
                    )
                    .then(
                        LiteralArgumentBuilder.literal<FabricClientCommandSource>("quit")
                            .executes {
                                CropCollector.quit()
                                return@executes 1
                            }
                    )
                    .then(
                        LiteralArgumentBuilder.literal<FabricClientCommandSource>("adjustPos")
                            .then(
                                RequiredArgumentBuilder.argument<FabricClientCommandSource, String>(
                                    "offset",
                                    StringArgumentType.word()
                                ).executes {
                                    CropCollector.scan(StringArgumentType.getString(it, "offset"))
                                    return@executes 1
                                }
                            )
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("uniques")
                    .executes {
                        dumpGrowthState()
                        return@executes 1
                    }
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

    /**
     * Everything the growth tick is worked out from, and what it currently comes to.
     *
     * Worth watching live: planting a unique anywhere shortens the tick for every plot at once, so
     * this moves while you work rather than only between sessions.
     */
    private fun dumpGrowthState() {
        val misc = GreenhouseData.miscInfo
        val uniques = GreenhouseData.getCurrentUniques()
        val missing = GreenhouseData.getMissingUniques()

        ChatUtils.sendWithPrefix(Component.literal("Growth").withStyle(ChatFormatting.GOLD))

        ChatUtils.send(field("uniques", "${uniques.size} of ${uniques.size + missing.size}"))
        ChatUtils.send(field("crop growth", misc.cropGrowthValue?.toString() ?: "unknown"))
        ChatUtils.send(field("speed upgrade", misc.cropSpeedUpgradeValue?.toString() ?: "unknown"))
        ChatUtils.send(field("yield upgrade", misc.cropYieldUpgradeValue?.toString() ?: "unknown"))
        ChatUtils.send(
            field(
                "speed attribute",
                GreenhouseData.greenhouseSpeedAttribute()?.toString() ?: "unknown, counted as 0"
            )
        )

        val tick = GreenhouseData.currentGrowthTickMs()

        ChatUtils.send(
            field("growth tick", tick?.let { exactDuration(it) } ?: "cannot be worked out yet")
        )

        dumpAttributes()
    }

    /**
     * Every attribute shard the player holds, by id and level.
     *
     * The greenhouse speed attribute is in here somewhere, but nothing in the api names it as such
     * and its id cannot be guessed, so it has to be read off a player who has one. Whichever line
     * below is the greenhouse one is the id to wire in.
     */
    private fun dumpAttributes() {
        // anything the player has at all: a shard sitting in the box is owned but not yet syphoned,
        // so it has no level, and filtering on level alone hides everything but the levelled ones
        // only the greenhouse one: listing every shard held ran past what chat keeps
        val owned = AttributeAPI.attributeMap
            .filterKeys { it.id == GreenhouseData.GREENHOUSE_SPEED_ATTRIBUTE_ID }

        if (owned.isEmpty()) {
            ChatUtils.send(field("attributes", "none held, or the attribute menu has not been opened"))
            return
        }

        ChatUtils.sendWithPrefix(
            Component.literal("Attributes held (${owned.size})").withStyle(ChatFormatting.GOLD)
        )

        owned.entries
            .sortedByDescending { it.value.level }
            .forEach { (id, data) ->
                ChatUtils.send(
                    copyable(id.id, "level ${data.level}, ${data.owned} owned, ${data.syphoned} syphoned")
                )
            }
    }

    /** A duration to the second, for a figure being held against the game's own. */
    private fun exactDuration(ms: Long): String {
        val seconds = ms / 1000

        return "%dh %02dm %02ds".format(seconds / 3600, seconds % 3600 / 60, seconds % 60)
    }

    /**
     * Every stand and display within [radius] of the player, nearest first.
     *
     * The stands this mod draws for a plan are left out unless [holograms] asks for them, so what
     * is listed is what the server actually put there.
     */
    private fun dumpNearbyEntities(radius: Double, holograms: Boolean) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val level = client.level ?: return

        val ours = if (holograms) emptySet() else LayoutRenderState.ghostStands.toSet()

        val entities = level.getEntities(player, player.boundingBox.inflate(radius))
            // everything a crop could be built out of, not only stands. A plant whose parts are
            // displays is invisible to anything that asks for stands, which is exactly the case
            // this listing exists to catch, so the net is cast by what an entity carries rather
            // than by what class it happens to be
            .filter { it is ArmorStand || it is Display || it is Interaction || it.hasCustomName() }
            .filterNot { it in ours }
            .sortedBy { it.distanceToSqr(player) }

        if (entities.isEmpty()) {
            ChatUtils.sendWithPrefix("Nothing worth listing within $radius blocks")
            return
        }

        ChatUtils.sendWithPrefix(
            Component.literal("${entities.size} nearby, within $radius blocks")
                .withStyle(ChatFormatting.GOLD)
        )

        // lit up as well as listed, since one head on screen can be several stands underneath it
        highlighted = entities.map { entity ->
            val color = when {
                entity is ArmorStand && entity.isMarker -> MARKER_COLOR
                entity is ArmorStand && entity.isSmall -> SMALL_COLOR
                else -> FULL_COLOR
            }

            entity.boundingBox to color
        }
        highlightUntil = Instant.now().plus(HIGHLIGHT_TIME)

        ChatUtils.sendWithPrefix(
            Component.literal("green small, orange full sized, purple marker")
                .withStyle(ChatFormatting.DARK_GRAY)
        )

        val text = entities.joinToString("\n") { entity ->
            describeEntity(entity, entity.position().subtract(player.position()))
        }

        client.keyboardHandler.clipboard = text

        ChatUtils.send(clipboard(text, "${entities.size} entities"))
    }

    /**
     * Everything within [radius], holding nothing back.
     *
     * The filtered listing answers "which of the things I know about is here". This answers the
     * other question, the one that only comes up when something is plainly visible and nothing in
     * the mod can see it: what is here at all. So it takes every entity of every type, says
     * everything it can say about each, and makes no judgement about what is worth mentioning,
     * because a judgement about what is worth mentioning is exactly what would hide the answer.
     *
     * It goes to the clipboard rather than only to chat. It is long on purpose, and chat drops the
     * top of it.
     */
    private fun dumpEverything(radius: Double) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val level = client.level ?: return

        // every entity, this one included: a listing that quietly leaves someone out is the thing
        // that made this command necessary
        val entities = level.getEntities(null as Entity?, player.boundingBox.inflate(radius))
            .sortedBy { it.distanceToSqr(player) }

        val text = buildString {
            appendLine("${entities.size} entities within $radius of ${fmt(player.position())}")

            entities.forEach { entity ->
                val at = entity.position()

                appendLine()
                appendLine("${entity.type.toString().substringAfterLast('.')} ${fmt(at)}")
                appendLine("  id=${entity.id} uuid=${entity.uuid}")
                appendLine("  rot=%.2f/%.2f box=${entity.boundingBox}".format(entity.yRot, entity.xRot))
                appendLine("  invisible=${entity.isInvisible} passengers=${entity.passengers.size}")

                entity.customName?.let {
                    appendLine("  name=${it.string}")
                    appendLine("  runs=${describeRuns(it)}")
                }

                if (entity is ArmorStand) {
                    appendLine(
                        "  stand small=${entity.isSmall} marker=${entity.isMarker} " +
                                "arms=${entity.showArms()} basePlate=${entity.showBasePlate()}"
                    )
                    appendLine("  headPose=${entity.headPose}")

                    EquipmentSlot.entries.forEach { slot ->
                        val stack = entity.getItemBySlot(slot)
                        if (!stack.isEmpty) appendLine("  $slot=${describeStack(stack)}")
                    }
                }

                if (entity is Display) {
                    appendLine("  display=${entity.javaClass.simpleName}")
                }

                if (entity is Display.ItemDisplay) {
                    appendLine("  item=${describeStack(entity.itemStack)}")
                }

                if (entity is Display.BlockDisplay) {
                    appendLine("  block=${entity.blockState}")
                }
            }
        }

        client.keyboardHandler.clipboard = text

        ChatUtils.send(clipboard(text, "${entities.size} entities"))
    }

    /**
     * One line the user clicks instead of a listing they have to scroll past.
     *
     * The text is already on the clipboard when this is sent, so the click is a second chance at
     * it rather than the only one.
     */
    private fun clipboard(text: String, what: String): Component {
        val lines = text.count { it == '\n' } + 1

        return Component.literal("[MA] ").withStyle(ChatFormatting.GOLD)
            .append(
                Component.literal("Click to copy $lines lines ($what)").withStyle(
                    Style.EMPTY
                        .withColor(ChatFormatting.YELLOW)
                        .withClickEvent(ClickEvent.CopyToClipboard(text))
                        .withHoverEvent(HoverEvent.ShowText(Component.literal("Already copied")))
                )
            )
    }

    /** One entity written out, for a listing that goes to the clipboard rather than to chat. */
    private fun describeEntity(entity: Entity, offset: Vec3): String = buildString {
        appendLine("${entity.type.toString().substringAfterLast('.')} ${fmt(entity.position())}")
        appendLine("  you ${fmt(offset)}")

        if (entity is ArmorStand) {
            appendLine("  stand small=${entity.isSmall} marker=${entity.isMarker}")
        }

        entity.customName?.let {
            appendLine("  name=${it.string}")
            appendLine("  runs=${describeRuns(it)}")
        }

        // every slot, since a crop's skull turns out not to always be worn on the head
        if (entity is ArmorStand) {
            EquipmentSlot.entries.forEach { slot ->
                val stack = entity.getItemBySlot(slot)
                if (!stack.isEmpty) appendLine("  $slot=${describeStack(stack)}")
            }
        }

        if (entity is Display.ItemDisplay) appendLine("  item=${describeStack(entity.itemStack)}")
    }

    /** An item as everything that might tell us what it is, not only its skull. */
    private fun describeStack(stack: net.minecraft.world.item.ItemStack): String = buildString {
        append(stack.count).append("x ").append(stack.item)

        PlayerUtils.getSkinHash(stack)?.let { append(" skin=").append(it) }

        stack.hoverName.let { append(" as \"").append(it.string).append("\"") }

        stack.componentsPatch.entrySet().forEach { (type, value) ->
            append(" | ").append(type).append("=").append(value)
        }
    }

    /** A position short enough to read in chat. */
    private fun fmt(pos: Vec3): String = "%.4f %.4f %.4f".format(pos.x, pos.y, pos.z)

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
            is ArmorStand -> EquipmentSlot.entries.map { entity.getItemBySlot(it) }
                .firstOrNull { !it.isEmpty }
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
