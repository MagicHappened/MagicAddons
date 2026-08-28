package org.magic.magicaddons.commands.debug

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.data.greenhouse.WorldRotation
import org.magic.magicaddons.render.WorldRender
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.PlayerUtils
import java.io.File
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Collects stage definitions from a whole greenhouse at once instead of one diagnosis click at a
 * time.
 *
 * Run standing one block south of the grid's south-eastern corner, facing north, so the ten by ten
 * lies ahead and to the left. The plot api only answers for the player's own garden, which is why
 * the grid is taken from where the player stands rather than asked for.
 *
 * Everything found is listed in chat and lit up in the world, one colour per crop, so what the
 * collector believes can be checked by eye against what is planted. Plants whose stage already
 * matches current data show gray and are left out unless clicked back in; everything else is a
 * candidate, confirmed by clicking its line. A diagnosis click during a run is taken as ground
 * truth for the plant the player is standing on. `collect finish` writes the confirmed plants to a
 * file for inspection, and nothing in the mod consumes what it writes.
 */
object CropCollector : EntityUtils.HighlightSource {

    /** Above the mob highlighter, since a collection run is the thing being looked at. */
    override val highlightPriority: Int = 100

    override fun highlightColor(entity: Entity): Int = standColors[entity] ?: GRAY

    private const val GRID: Int = 10

    /** How far above the soil a plant can reach, for the stand search and the block columns. */
    private const val PLANT_HEIGHT: Int = 15

    private const val GRAY: Int = 0xFF9E9E9E.toInt()
    private const val UNKNOWN_WHITE: Int = 0xFFFFFFFF.toInt()

    private const val BLOCK_ALPHA: Int = 0x38

    /** One colour per crop, told apart at a glance; assigned by order of appearance. */
    private val PALETTE: IntArray = intArrayOf(
        0xFFFF5555.toInt(), 0xFF55FF55.toInt(), 0xFF5599FF.toInt(), 0xFFFFAA00.toInt(),
        0xFFFF55FF.toInt(), 0xFF55FFFF.toInt(), 0xFFFFFF55.toInt(), 0xFFAA77FF.toInt(),
        0xFFFF9999.toInt(), 0xFF99CC66.toInt(), 0xFF66CCCC.toInt(), 0xFFCC9966.toInt()
    )

    private enum class Status(val label: String) {
        /** Matches the definitions as they stand, nothing to collect; listed gray, ignored. */
        Current("matches current data"),

        /** Matched, but through the pre-normalization fallback: worth re-collecting. */
        Legacy("recorded before normalized exports"),

        /** Named for a crop we know, standing at a stage nobody has recorded. */
        Unrecorded("unrecorded"),

        /** Named for nothing in the registry, reported but not collectable. */
        Unknown("unknown crop")
    }

    private class Entry(
        val id: Int,
        val def: CropDefinition?,
        var origin: BlockPos,
        val stands: List<ArmorStand>,
        var status: Status,
        /** What the plant said through a diagnosis, or what matching decided; null is unread. */
        var stageText: String?,
        var stageNum: Int?,
        val names: Set<String>,
        val color: Int,
        var confirmed: Boolean = false,
        var boxes: List<AABB> = emptyList()
    )

    private class Session(
        val level: ClientLevel,
        val gridOrigin: BlockPos,
        val entries: MutableList<Entry> = mutableListOf()
    ) {
        var finishedAt: Instant? = null
    }

    private var session: Session? = null
    private val standColors: MutableMap<Entity, Int> = mutableMapOf()
    private val cropColors: MutableMap<String, Int> = mutableMapOf()

    // ------------------------------------------------------------------ scanning

    fun scan() {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val level = client.level ?: return

        // north is the whole orientation contract, so standing any other way is an error now
        // rather than a grid collected sideways
        if (abs(Mth.wrapDegrees(player.yRot)) < 135f) {
            ChatUtils.sendWithPrefix(
                Component.literal("Face north (the grid ahead and to the left), then run this again.")
                    .withStyle(ChatFormatting.RED)
            )
            return
        }

        clear()

        // the block being stood on: standing on farmland puts the feet inside it, since farmland
        // is a sliver short of a full block, while a full soil leaves the feet in air above it
        val feet = player.blockPosition()
        val standingOn = if (!level.getBlockState(feet).isAir) feet else feet.below()

        // one south of the south-eastern corner: the corner is a step north, and the grid runs
        // nine further north and nine west from it
        val origin = BlockPos(standingOn.x - (GRID - 1), standingOn.y, standingOn.z - GRID)

        val s = Session(level, origin)
        session = s

        ChatUtils.sendWithPrefix(
            "Collecting from (${origin.x}, ${origin.y}, ${origin.z}) to " +
                    "(${origin.x + GRID - 1}, ${origin.y}, ${origin.z + GRID - 1})"
        )

        val pool = level.getEntitiesOfClass(
            ArmorStand::class.java,
            AABB(
                origin.x.toDouble(), origin.y - 2.0, origin.z.toDouble(),
                origin.x + GRID.toDouble(), origin.y + PLANT_HEIGHT.toDouble(), origin.z + GRID.toDouble()
            )
        )
            .filterNot { it.isMarker }
            .filterNot { PlayerUtils.getSkullHash(it) == null && !it.hasCustomName() }
            .toMutableList()

        // first pass: everything the definitions already recognise, wherever its origin lies
        for (dx in 0 until GRID) {
            for (dz in 0 until GRID) {
                val slotPos = origin.offset(dx, 0, dz)
                val soilState = level.getBlockState(slotPos)
                if (soilState.isAir) continue

                val found = GreenhouseGrid.findElementAt(
                    slotPos,
                    soilState.block,
                    pool,
                    LayoutSlot(slotPos.x, slotPos.z, soilState)
                ) ?: continue

                val stands = found.standEntities.orEmpty().filterIsInstance<ArmorStand>()
                pool.removeAll(stands.toSet())

                val def = found.instance.cropDef
                val status = if (found.rotationLegacy) Status.Legacy else Status.Current

                val (text, num) = when (val g = found.instance.growthStage) {
                    is GrowthStageInfo.Known -> g.stage.toString() to g.stage
                    is GrowthStageInfo.Estimated -> "${g.range.first}..${g.range.last}" to null
                    else -> null to null
                }

                addEntry(def, slotPos, stands, status, text, num, stands.standNames())
            }
        }

        // second pass: whatever is left, grouped by the crop its stands are named for
        for ((def, standsOfCrop) in pool.groupBy { defForName(it.standName()) }) {
            if (def == null) {
                // reported so nothing vanishes, but per decision never collected: a crop with no
                // definition at all has no footprint to anchor by, and one grown plant is cheaper
                // than untangling a bad guess later
                standsOfCrop.groupBy { it.blockPosition().atY(origin.y) }.forEach { (pos, stands) ->
                    addEntry(null, pos, stands, Status.Unknown, null, null, stands.standNames())
                }
                continue
            }

            for (cluster in clusterByFootprint(standsOfCrop, def)) {
                addEntry(
                    def,
                    guessOrigin(cluster, def, origin.y),
                    cluster,
                    Status.Unrecorded,
                    null, null,
                    cluster.standNames()
                )
            }
        }

        s.entries.sortBy { it.status.ordinal.let { o -> if (it.status == Status.Current) 9 else o } }

        ChatUtils.sendWithPrefix("${s.entries.size} plants found, click the right ones to confirm:")
        s.entries.forEach { sendLine(it) }
        ChatUtils.send(
            Component.literal("  then run /MagicAddons farming collect finish to write the file")
                .withStyle(ChatFormatting.DARK_GRAY)
        )
    }

    private fun addEntry(
        def: CropDefinition?,
        origin: BlockPos,
        stands: List<ArmorStand>,
        status: Status,
        stageText: String?,
        stageNum: Int?,
        names: Set<String>
    ) {
        val s = session ?: return

        val color = when (status) {
            Status.Current -> GRAY
            Status.Unknown -> UNKNOWN_WHITE
            else -> def?.let { colorFor(it.name) } ?: UNKNOWN_WHITE
        }

        val entry = Entry(
            id = s.entries.size,
            def = def,
            origin = origin,
            stands = stands,
            status = status,
            stageText = stageText,
            stageNum = stageNum,
            names = names,
            color = color
        )
        entry.boxes = boxesFor(entry)

        stands.forEach {
            standColors[it] = color
            EntityUtils.add(it, this)
        }

        s.entries.add(entry)
    }

    // ------------------------------------------------------------------ identity and geometry

    private fun norm(text: String): String = text.lowercase().filter { it.isLetter() }

    private fun ArmorStand.standName(): String? = customName?.string

    private fun List<ArmorStand>.standNames(): Set<String> =
        mapNotNull { it.standName() }.toSet()

    /**
     * The definition a stand's name points at, by the longest name prefix that fits.
     *
     * The game names stands after their crop with decorations of its own on the end, snoozlingLeaf0
     * and magicjellybean1 alike, so the crop is the front of the name rather than the whole of it.
     * Longest match keeps a name from settling for a shorter crop it happens to start like.
     */
    private fun defForName(name: String?): CropDefinition? {
        val n = name?.let(::norm) ?: return null
        if (n.isEmpty()) return null

        return CropRegistry.all
            .filter { n.startsWith(norm(it.name)) }
            .maxByOrNull { norm(it.name).length }
    }

    /** Stands close enough to be one plant of [def], greedily flooded from the first. */
    private fun clusterByFootprint(stands: List<ArmorStand>, def: CropDefinition): List<List<ArmorStand>> {
        val reach = max(def.footprint.width, def.footprint.height) - 1
        val remaining = stands.toMutableList()
        val clusters = mutableListOf<List<ArmorStand>>()

        while (remaining.isNotEmpty()) {
            val cluster = mutableListOf(remaining.removeFirst())
            var grew = true

            while (grew) {
                grew = false
                val near = remaining.filter { candidate ->
                    cluster.any { member ->
                        val a = candidate.blockPosition()
                        val b = member.blockPosition()
                        max(abs(a.x - b.x), abs(a.z - b.z)) <= reach
                    }
                }
                if (near.isNotEmpty()) {
                    cluster.addAll(near)
                    remaining.removeAll(near.toSet())
                    grew = true
                }
            }

            clusters.add(cluster)
        }

        return clusters
    }

    /**
     * Where the plant most likely starts, from the middle of its stands.
     *
     * A guess and said to be one: stands need not be centred on their plant, so the click that
     * confirms an entry is also the check on this, and a diagnosis taken standing on the plant's
     * north-western block replaces it with the truth.
     */
    private fun guessOrigin(cluster: List<ArmorStand>, def: CropDefinition, soilY: Int): BlockPos {
        val xs = cluster.map { it.x }
        val zs = cluster.map { it.z }
        val midX = (xs.min() + xs.max()) / 2.0
        val midZ = (zs.min() + zs.max()) / 2.0

        return BlockPos(
            (midX - def.footprint.width / 2.0).roundToInt(),
            soilY,
            (midZ - def.footprint.height / 2.0).roundToInt()
        )
    }

    /** The soil the entry stands on and every block of plant above it, framed for the eye. */
    private fun boxesFor(entry: Entry): List<AABB> {
        val level = session?.level ?: return emptyList()
        val w = entry.def?.footprint?.width ?: 1
        val h = entry.def?.footprint?.height ?: 1

        return buildList {
            for (dx in 0 until w) {
                for (dz in 0 until h) {
                    val soil = entry.origin.offset(dx, 0, dz)
                    add(AABB(soil))

                    var pos = soil.above()
                    while (!level.getBlockState(pos).isAir && pos.y <= soil.y + PLANT_HEIGHT) {
                        add(AABB(pos))
                        pos = pos.above()
                    }
                }
            }
        }
    }

    private fun colorFor(cropName: String): Int =
        cropColors.getOrPut(cropName) { PALETTE[cropColors.size % PALETTE.size] }

    // ------------------------------------------------------------------ the player's verdicts

    /** The click on an entry's chat line, flipping it in or out of the file. */
    fun toggle(id: Int) {
        val s = session ?: run {
            ChatUtils.sendWithPrefix("No collection running.")
            return
        }
        val entry = s.entries.getOrNull(id) ?: run {
            ChatUtils.sendWithPrefix("No entry $id in this run.")
            return
        }

        if (entry.status == Status.Unknown) {
            ChatUtils.sendWithPrefix(
                "${entry.names.firstOrNull() ?: "That"} has no definition to anchor by, grow one at home first."
            )
            return
        }

        entry.confirmed = !entry.confirmed
        sendLine(entry)
    }

    /**
     * A diagnosis taken during a run, which outranks every guess: the player is standing on the
     * plant's north-western block and the page has just said which stage it is.
     */
    fun correct(def: CropDefinition, stage: Int) {
        val s = session ?: return
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        if (client.level !== s.level) return

        val feet = player.blockPosition()
        val standingOn = if (!s.level.getBlockState(feet).isAir) feet else feet.below()
        val reach = max(def.footprint.width, def.footprint.height)

        val entry = s.entries
            .filter { it.def === def }
            .minByOrNull { max(abs(it.origin.x - standingOn.x), abs(it.origin.z - standingOn.z)) }
            ?.takeIf { max(abs(it.origin.x - standingOn.x), abs(it.origin.z - standingOn.z)) <= reach }

        if (entry == null) {
            ChatUtils.sendWithPrefix(
                "No collected ${def.name} near you to correct, stand on its north-western block."
            )
            return
        }

        entry.origin = standingOn
        entry.stageText = stage.toString()
        entry.stageNum = stage
        entry.boxes = boxesFor(entry)

        ChatUtils.sendWithPrefix("${def.name} pinned to (${standingOn.x}, ${standingOn.z}) at stage $stage")
        sendLine(entry)
    }

    // ------------------------------------------------------------------ output

    fun finish() {
        val s = session ?: run {
            ChatUtils.sendWithPrefix("No collection running, run collect first.")
            return
        }

        val confirmed = s.entries.filter { it.confirmed }
        if (confirmed.isEmpty()) {
            ChatUtils.sendWithPrefix("Nothing confirmed yet, click the lines that are right first.")
            return
        }

        val text = buildString {
            appendLine("// collected from grid at (${s.gridOrigin.x}, ${s.gridOrigin.y}, ${s.gridOrigin.z})")
            appendLine()

            confirmed.forEach { entry ->
                val def = entry.def ?: return@forEach

                appendLine("// ===== ${def.name} at (${entry.origin.x}, ${entry.origin.y}, ${entry.origin.z}) =====")
                appendLine(
                    "// status=${entry.status.label} stage=${entry.stageText ?: "unread"}" +
                            " worldStep=${WorldRotation.step(entry.origin.x, entry.origin.z)}" +
                            " stands=${entry.stands.size} names=${entry.names}"
                )

                val code = CropStageExporter.buildCropStageData(
                    basePos = entry.origin,
                    stageNum = entry.stageNum,
                    foundDefinition = def,
                    quiet = true
                )
                appendLine(code ?: "// world went away while writing this one")
                appendLine()
            }
        }

        val dir = File("config/magicaddons/collected")
        dir.mkdirs()
        val file = File(dir, "collect-${System.currentTimeMillis()}.txt")
        file.writeText(text)

        s.finishedAt = Instant.now()

        ChatUtils.sendWithPrefix(
            Component.literal("Wrote ${confirmed.size} plants to ${file.path}")
                .withStyle(ChatFormatting.GREEN)
        )
    }

    private fun sendLine(entry: Entry) {
        val name = entry.def?.name ?: entry.names.firstOrNull() ?: "unknown"
        val stage = entry.stageText?.let { "stage $it" } ?: "stage ?"
        val mark = if (entry.confirmed) "[✔] " else ""

        val body = "$mark[${entry.id}] $name (${entry.origin.x}, ${entry.origin.z}) $stage — ${entry.status.label}"

        val style = when (entry.status) {
            Status.Unknown -> Style.EMPTY.withColor(ChatFormatting.WHITE)
            else -> Style.EMPTY
                .withColor(TextColor.fromRgb(entry.color and 0xFFFFFF))
                .withClickEvent(ClickEvent.RunCommand("/MagicAddons internal collectToggle ${entry.id}"))
                .withHoverEvent(
                    HoverEvent.ShowText(
                        Component.literal(if (entry.confirmed) "Click to drop from the file" else "Click to confirm")
                    )
                )
        }

        ChatUtils.send(Component.literal("  ").append(Component.literal(body).withStyle(style)))
    }

    // ------------------------------------------------------------------ lifetime

    /** Draws the run's boxes from the frame's own pass, and retires the run when its time comes. */
    fun submitHighlights(poseStack: PoseStack, collector: SubmitNodeCollector, cameraPos: Vec3) {
        val s = session ?: return

        // the highlights live until the world does, or ten seconds past the file being written
        val done = s.finishedAt?.let { Instant.now().isAfter(it.plusSeconds(10)) } ?: false
        if (done || Minecraft.getInstance().level !== s.level) {
            clear()
            return
        }

        s.entries.forEach { entry ->
            entry.boxes.forEach { box ->
                WorldRender.markBox(poseStack, collector, cameraPos, box, entry.color, BLOCK_ALPHA)
            }
        }
    }

    private fun clear() {
        EntityUtils.removeAllForSource(this)
        standColors.clear()
        cropColors.clear()
        session = null
    }
}
