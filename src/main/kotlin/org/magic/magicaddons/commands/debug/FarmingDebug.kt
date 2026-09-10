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
import org.magic.magicaddons.commands.CropWords
import org.magic.magicaddons.commands.fmt
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.PlantDex
import org.magic.magicaddons.ui.screens.CropPreviewScreen
import org.magic.magicaddons.features.farming.greenhousePresets.LayoutRenderState
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.util.EntityUtils.typePath
import org.magic.magicaddons.util.PlayerUtils
import org.magic.magicaddons.util.ScreenUtil

/**
 * Reads the entities standing around the player. Greenhouse stands have no hit box to aim at, so
 * they are dumped by proximity, with each name's formatting spelled out so bars can be read.
 */
object FarmingDebug : AbstractCommand() {

    private const val DEFAULT_RADIUS: Double = 4.0
    private const val MIN_RADIUS: Double = 0.5
    private const val MAX_RADIUS: Double = 32.0

    /** The plant dex word that lists every gap instead of one crop. */
    private const val MISSING_WORD: String = "missing"

    /** How long a dump leaves its stands lit up for. */
    private val HIGHLIGHT_TIME: Duration = Duration.ofSeconds(30)

    /** A stand's colour says how it is built: one head on screen may be several stands in the world. */
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

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        val entities = LiteralArgumentBuilder.literal<FabricClientCommandSource>("entities")
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
                        DoubleArgumentType.doubleArg(MIN_RADIUS, MAX_RADIUS)
                    ).executes {
                        dumpNearbyEntities(
                            DoubleArgumentType.getDouble(it, "radius"),
                            BoolArgumentType.getBool(it, "holograms")
                        )
                        return@executes 1
                    }
                )
            )

        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            // bare "farming" runs "farming entities"
            .executes(entities.command)
            .then(entities)
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("entitiesAll")
                    .executes {
                        dumpEverything(DEFAULT_RADIUS)
                        return@executes 1
                    }
                    .then(
                        RequiredArgumentBuilder.argument<FabricClientCommandSource, Double>(
                            "radius",
                            DoubleArgumentType.doubleArg(MIN_RADIUS, MAX_RADIUS)
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
                plantDexCommand()
            )
            .then(previewCommand())
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("scan")
                    .executes {
                        GreenhouseData.scanUpdatesState = !GreenhouseData.scanUpdatesState
                        ChatUtils.sendWithPrefix(
                            "Greenhouse scans ${allowed(GreenhouseData.scanUpdatesState)} update the mod's data."
                        )
                        return@executes 1
                    }
            )
            .then(
                LiteralArgumentBuilder.literal<FabricClientCommandSource>("tool")
                    .executes {
                        GreenhouseData.toolUpdatesState = !GreenhouseData.toolUpdatesState
                        ChatUtils.sendWithPrefix(
                            "Diagnosis tool readings ${allowed(GreenhouseData.toolUpdatesState)} update the mod's data."
                        )
                        return@executes 1
                    }
            )
    }

    /** How a toggle reads in the line that reports it. */
    private fun allowed(on: Boolean): String = if (on) "now" else "no longer"

    /** The crop preview, on a crop when one is named and empty when not. */
    private fun previewCommand(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>("preview")
            .executes {
                ScreenUtil.setScreen(CropPreviewScreen(null))
                return@executes 1
            }
            .then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, String>(
                    "crop",
                    StringArgumentType.word()
                ).suggests { _, builder ->
                    CropWords.suggest(builder)
                }.executes {
                    val word = StringArgumentType.getString(it, "crop")
                    val def = CropWords.find(word)

                    if (def == null) {
                        ChatUtils.sendWithPrefix("No crop called $word.")
                        return@executes 0
                    }

                    ScreenUtil.setScreen(CropPreviewScreen(null, def))
                    return@executes 1
                }
            )

    /** The dex, and under it "missing" then every crop as a command word. */
    private fun plantDexCommand(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>("plantDex")
            .executes {
                dumpPlantDex()
                return@executes 1
            }
            .then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, String>(
                    "crop",
                    StringArgumentType.word()
                ).suggests { _, builder ->
                    CropWords.suggest(builder, listOf(MISSING_WORD))
                }.executes {
                    val word = StringArgumentType.getString(it, "crop")
                    val def = CropWords.find(word)

                    when {
                        word.equals(MISSING_WORD, ignoreCase = true) -> dumpMissingPlants()
                        def != null -> dumpPlantDexFor(def)
                        else -> ChatUtils.sendWithPrefix("No crop called $word.")
                    }
                    return@executes 1
                }
            )

    /** Every crop still missing something, one line per tier with the crops in its hover. */
    private fun dumpMissingPlants() {
        val gaps = PlantDex.gapsByTier()

        if (gaps.isEmpty()) {
            ChatUtils.sendWithPrefix("Nothing missing. The dex is complete.")
            return
        }

        ChatUtils.sendWithPrefix(
            Component.literal("Currently missing plants (hover):").withStyle(ChatFormatting.GOLD)
        )

        gaps.forEach { (tier, crops) ->
            val hover = Component.empty()
            crops.forEachIndexed { index, gap ->
                if (index > 0) hover.append(Component.literal("\n"))
                hover.append(Component.literal(gap.def.name).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" -> ${gap.parts.joinToString("; ")}").withStyle(ChatFormatting.GRAY))
            }

            ChatUtils.send(
                Component.literal("  ${PlantDex.TIER_TITLES[tier]} (${crops.size})")
                    .withStyle(ChatFormatting.YELLOW)
                    .withStyle { it.withHoverEvent(HoverEvent.ShowText(hover)) }
            )
        }
    }

    /** What one crop is still missing, said in chat rather than copied. */
    private fun dumpPlantDexFor(def: CropDefinition) {
        val missing = PlantDex.reportFor(def)

        if (missing == null) {
            ChatUtils.sendWithPrefix(
                Component.literal("${def.name}: all ${def.maxStage} stages recorded")
                    .withStyle(ChatFormatting.GREEN)
            )
            return
        }

        ChatUtils.sendWithPrefix(
            Component.literal("${def.name}: ${PlantDex.percentFor(def)}% of ${def.maxStage} stages")
                .withStyle(ChatFormatting.GOLD)
        )
        ChatUtils.send(
            Component.literal("  $missing").withStyle(ChatFormatting.GRAY)
        )
    }

    /**
     * How much of every crop the definitions cover. The percentage goes to chat, the gap list to
     * the clipboard, sorted so a collection trip can be planned off it.
     */
    private fun dumpPlantDex() {
        val report = PlantDex.report()

        ChatUtils.sendWithPrefix(
            Component.literal(
                "Plant dex: ${report.percent}% recorded (${report.recorded} of ${report.total} stages)"
            ).withStyle(ChatFormatting.GOLD)
        )

        if (report.missingList.isEmpty()) {
            ChatUtils.sendWithPrefix("Nothing missing. The dex is complete.")
            return
        }

        Minecraft.getInstance().keyboardHandler.clipboard = report.missingList

        ChatUtils.send(clipboard(report.missingList, "${report.incompleteCrops} crops incomplete"))
    }

    /** Every stand and display near the player, nearest first. Our own plan stands only on request. */
    private fun dumpNearbyEntities(radius: Double, holograms: Boolean) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val level = client.level ?: return

        val ours = if (holograms) emptySet() else LayoutRenderState.ghostStands.toSet()

        val entities = level.getEntities(player, player.boundingBox.inflate(radius))
            // by what an entity carries rather than what class it is: a plant built out of displays is
            // invisible to anything asking for stands, which is the case this listing exists to catch
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
     * Everything in range, holding nothing back, for when something is plainly visible and nothing
     * in the mod can see it. Goes to the clipboard, since chat drops the top of it.
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
                appendLine("${entity.typePath()} ${fmt(at)}")
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
                    appendLine("  rightArmPose=${entity.rightArmPose}")

                    // where the collector believes a held item hangs, for calibrating that guess
                    // against what is plainly visible in game
                    CropCollector.heldItemBlock(entity)?.let {
                        appendLine("  heldItemBlock=(${it.x}, ${it.y}, ${it.z})")
                    }

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

    /** One clickable line instead of a listing to scroll past. The text is already on the clipboard. */
    private fun clipboard(text: String, what: String): Component {
        val lines = text.count { it == '\n' } + 1

        return ChatUtils.buildWithPrefix(
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
        appendLine("${entity.typePath()} ${fmt(entity.position())}")
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

    /** The custom name as its styled runs, `colour:text` each, which is what makes a bar readable. */
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
}
