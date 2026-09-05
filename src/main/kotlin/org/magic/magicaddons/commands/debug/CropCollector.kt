package org.magic.magicaddons.commands.debug

import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import net.minecraft.world.entity.EquipmentSlot
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
import org.magic.magicaddons.data.greenhouse.GREENHOUSE_SOIL_Y
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.util.getBuildableArea
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI
import org.magic.magicaddons.data.greenhouse.CropStagePattern
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GrowthStageInfo
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.data.greenhouse.PlantDex
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
 * Collects stage definitions from a whole greenhouse at once. Run standing one block south of the
 * grid's south-eastern corner, facing north; confirm a listed line by clicking it, then `collect finish`.
 */
object CropCollector : EntityUtils.HighlightSource {

    /** Above the mob highlighter, since a collection run is the thing being looked at. */
    override val highlightPriority: Int = 100

    override fun highlightColor(entity: Entity): Int = standColors[entity] ?: GRAY

    private const val GRID: Int = 10

    /** The devourer's roots are their own element, though the diagnosis names the devourer. */
    private const val DEVOURER: String = "Devourer"
    private const val DEVOURER_ROOTS: String = "DevourerRoots"

    /** How far above the soil a plant can reach, for the stand search and the block columns. */
    private const val PLANT_HEIGHT: Int = 15

    /** The skulls the plot marker stands carry on every greenhouse, never part of a plant. */
    private val PLOT_MARKER_SKINS: Set<String> = CropStageExporter.PLOT_MARKER_SKINS

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
        Legacy("needs normalization"),

        /** Matched, and described, but recorded without the way its stands are turned. */
        Unturned("needs rotation data"),

        /** Matched, but a stand stood full sized where its definition says small. */
        Oversized("needs isSmall = false"),

        /** Matched, but a stand stood small where its definition says full sized. */
        Undersized("needs isSmall = true"),

        /** Named for a crop we know, standing at a stage nobody has recorded. */
        Unrecorded("unrecorded"),

        /** A mutation the player put down, whose bought look nobody has recorded. */
        PlacedMissing("needs placed data"),

        /** Named for nothing in the registry, reported but not collectable. */
        Unknown("unknown crop")
    }

    private class Entry(
        val id: Int,
        var def: CropDefinition?,
        var origin: BlockPos,
        val stands: List<ArmorStand>,
        var status: Status,
        /** What the plant said through a diagnosis, or what matching decided; null is unread. */
        var stageText: String?,
        var stageNum: Int?,
        val names: Set<String>,
        var color: Int,
        var confirmed: Boolean = false,
        var boxes: List<AABB> = emptyList(),
        /** What the diagnosis tool and the matcher said about it, shown in place of the usual label. */
        var toolNote: String? = null,
        /** A mutation the player put down in their own garden: what is recorded is its bought look. */
        var placedLook: Boolean = false
    )

    private class Session(
        val level: ClientLevel,
        val gridOrigin: BlockPos,
        val entries: MutableList<Entry> = mutableListOf()
    ) {
        var finishedAt: Instant? = null
    }

    private var session: Session? = null

    /** Whether a run is live in the world the player is looking at. */
    fun isActive(): Boolean =
        session?.let { it.finishedAt == null && Minecraft.getInstance().level === it.level } == true
    private val standColors: MutableMap<Entity, Int> = mutableMapOf()
    private val cropColors: MutableMap<String, Int> = mutableMapOf()

    // ------------------------------------------------------------------ scanning

    fun scan(adjust: String? = null) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return
        val level = client.level ?: return

        // "west2" says the player stands two west of the spot the grid is measured from, so undoing
        // it recovers the spot
        val displacement = adjust?.let {
            parseAdjust(it) ?: run {
                ChatUtils.sendWithPrefix(
                    "Could not read \"$it\", say a direction then blocks, such as west2 or north1."
                )
                return
            }
        }

        // on any garden the plot under the player says where the grid is, so they may stand anywhere
        val origin = plotOrigin() ?: run {
            // north is the whole orientation contract, so standing any other way is an error now
            // rather than a grid collected sideways
            if (abs(Mth.wrapDegrees(player.yRot)) < 135f) {
                ChatUtils.sendWithPrefix(
                    Component.literal("Face north (the grid ahead and to the left), then run this again.")
                        .withStyle(ChatFormatting.RED)
                )
                return
            }

            // only x and z come from the player: greenhouse soil sits at one height, whatever the
            // player happens to be standing on
            val feet = player.blockPosition()
            val actual = BlockPos(feet.x, GREENHOUSE_SOIL_Y, feet.z)
            val standingOn = displacement?.let { (dx, dz) -> actual.offset(-dx, 0, -dz) } ?: actual

            // one south of the south-eastern corner: the corner is a step north, and the grid runs
            // nine further north and nine west from it
            BlockPos(standingOn.x - (GRID - 1), standingOn.y, standingOn.z - GRID)
        }

        clear()

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
            // the plot's own marker head hovers high over every greenhouse without being flagged
            // a marker, and once floated seven blocks up into a snoozling export
            .filterNot { PlayerUtils.getSkullHash(it) in PLOT_MARKER_SKINS }
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

                val (text, num) = when (val g = found.instance.growthStage) {
                    is GrowthStageInfo.Known -> g.stage.toString() to g.stage
                    is GrowthStageInfo.Estimated -> "${g.range.first}..${g.range.last}" to null
                    else -> null to null
                }

                // the same promotion the correction pass makes: matched fine, but recorded
                // without the way its stands are turned, so worth taking again
                val status = when {
                    found.rotationLegacy -> Status.Legacy
                    num != null && PlantDex.needsRotation(def, num) -> Status.Unturned
                    else -> Status.Current
                }.let { if (num != null) sizeMismatch(def, num, stands) ?: it else it }

                addEntry(def, slotPos, stands, status, text, num, stands.standNames())
            }
        }

        // second pass: whatever is left, grouped by the crop its stands are named for
        for ((def, standsOfCrop) in pool.groupBy { identify(it) }) {
            if (def == null) {
                // reported so nothing vanishes, but never collected: a crop with no definition has no
                // footprint to anchor by
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

        // third pass: plants made only of blocks, as the base crops are. A block state names its crop
        // only when exactly one definition uses it
        val covered = mutableSetOf<Long>()
        s.entries.forEach { entry ->
            val w = entry.def?.footprint?.width ?: 1
            val h = entry.def?.footprint?.height ?: 1
            for (cx in 0 until w) {
                for (cz in 0 until h) {
                    covered.add(BlockPos.asLong(entry.origin.x + cx, entry.origin.y, entry.origin.z + cz))
                }
            }
        }

        for (dx in 0 until GRID) {
            for (dz in 0 until GRID) {
                val slotPos = origin.offset(dx, 0, dz)
                if (slotPos.asLong() in covered) continue
                if (level.getBlockState(slotPos).isAir) continue

                val above = level.getBlockState(slotPos.above())
                if (above.isAir) continue

                val candidates = defsForBlockState(above, level.getBlockState(slotPos).block)
                val described = describeState(above)

                when {
                    candidates.size == 1 -> addEntry(
                        candidates.single(), slotPos, emptyList(),
                        Status.Unrecorded, null, null, setOf(described)
                    )

                    candidates.isEmpty() -> addEntry(
                        null, slotPos, emptyList(),
                        Status.Unknown, null, null, setOf(described)
                    )

                    else -> addEntry(
                        null, slotPos, emptyList(), Status.Unknown, null, null,
                        setOf(described + " \u2014 " + candidates.joinToString("/") { it.name })
                    )
                }
            }
        }

        s.entries.sortBy { it.status.ordinal.let { o -> if (it.status == Status.Current) 9 else o } }

        ChatUtils.sendWithPrefix("${s.entries.size} plants found, click the right ones to confirm:")
        s.entries.forEach { sendLine(it) }
        ChatUtils.send(
            Component.literal("  press G for the checklist, collect finish writes the file, collect quit dismisses")
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
            id = (s.entries.maxOfOrNull { it.id } ?: -1) + 1,
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

    /** "west2" as the two axes it moves, or null for anything that is not direction-then-count. */
    private fun parseAdjust(spec: String): Pair<Int, Int>? {
        val match = Regex("(north|south|east|west)(\\d+)", RegexOption.IGNORE_CASE)
            .matchEntire(spec.trim()) ?: return null

        val count = match.groupValues[2].toIntOrNull()?.takeIf { it in 1..64 } ?: return null

        return when (match.groupValues[1].lowercase()) {
            "north" -> 0 to -count
            "south" -> 0 to count
            "east" -> count to 0
            else -> -count to 0
        }
    }

    /** Every block state a definition's stages describe, for the plants that have no stands. */
    private val defsByState: Map<net.minecraft.world.level.block.state.BlockState, List<CropDefinition>> by lazy {
        buildMap<net.minecraft.world.level.block.state.BlockState, MutableList<CropDefinition>> {
            CropRegistry.all.forEach { def ->
                def.stageDefs
                    .flatMap { it.blocks.orEmpty() }
                    .map { it.blockState }
                    .distinct()
                    .forEach { getOrPut(it) { mutableListOf() }.add(def) }
            }
        }
    }

    private fun defsForBlockState(
        state: net.minecraft.world.level.block.state.BlockState,
        soil: net.minecraft.world.level.block.Block
    ): List<CropDefinition> = defsByState[state].orEmpty().filter { soil in it.requiredSoil }

    /** A block state short enough for a row, "melon_stem[age=3]" rather than the full toString. */
    private fun describeState(state: net.minecraft.world.level.block.state.BlockState): String =
        state.toString()
            .removePrefix("Block{minecraft:")
            .replace("}", "")

    private fun ArmorStand.standName(): String? = customName?.string

    private fun List<ArmorStand>.standNames(): Set<String> =
        mapNotNull { it.standName() }.toSet()

    /**
     * The definition a stand's name points at, by the longest name prefix that fits: the game names
     * stands after their crop with decorations on the end.
     */
    /**
     * The one definition a skull hash appears in, for stands with no name. Shared hashes are left out.
     */
    private val defsByHash: Map<String, CropDefinition> by lazy {
        val owners = mutableMapOf<String, MutableSet<CropDefinition>>()

        CropRegistry.all.forEach { def ->
            def.stageDefs.forEach { stage ->
                stage.armorStands?.forEach { stand ->
                    stand.hashString?.let { owners.getOrPut(it) { mutableSetOf() }.add(def) }
                }
            }
        }

        owners.filterValues { it.size == 1 }.mapValues { it.value.first() }
    }

    /**
     * The grid's corner of the plot the player stands in, or null off a garden or outside every
     * plot. Plots sit at the same coordinates on every garden, so this holds for a guest as well.
     */
    private fun plotOrigin(): BlockPos? {
        if (LocationAPI.island != SkyBlockIsland.GARDEN) return null
        val plot = PlotAPI.getCurrentPlot()?.takeUnless { it.isBarn } ?: return null
        val area = plot.getBuildableArea()

        return BlockPos(area.minX.toInt(), GREENHOUSE_SOIL_Y, area.minZ.toInt())
    }

    /**
     * The size status when a stand of [stands] is not the size the definition gives that skull at
     * [stage], or null when every size agrees. Remembered in the dex, so every listing says so.
     * A skull the stage lists at both sizes is left alone, since either stand could be the one.
     */
    private fun sizeMismatch(def: CropDefinition, stage: Int, stands: List<ArmorStand>): Status? {
        val sizeOf = def.stageDefs
            .flatMap { if (it is CropStagePattern) it.expand() else listOf(it) }
            .filter { stage in it.stageRange }
            .flatMap { it.armorStands.orEmpty() }
            .filter { it.hashString != null }
            .groupBy({ it.hashString!! }, { it.isSmall })
            .filterValues { sizes -> sizes.distinct().size == 1 }
            .mapValues { it.value.first() }

        val status = stands.firstNotNullOfOrNull { stand ->
            when (sizeOf[PlayerUtils.getSkullHash(stand)]) {
                true -> if (stand.isSmall) null else Status.Oversized
                false -> if (stand.isSmall) Status.Undersized else null
                null -> null
            }
        }

        if (status != null) PlantDex.noteSize(def.name, stage, needsSmall = status == Status.Undersized)
        return status
    }

    private fun identify(stand: ArmorStand): CropDefinition? =
        defForName(stand.standName())
            ?: PlayerUtils.getSkullHash(stand)?.let { defsByHash[it] }

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
     * Where the plant most likely starts, from the middle of its stands. A guess: stands need not be
     * centred, and a diagnosis replaces it with the truth.
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

    /** One row of the checklist: everything a screen needs of an entry and nothing more. */
    data class Row(
        val id: Int,
        val label: String,
        val color: Int,
        val confirmed: Boolean,
        val collectable: Boolean
    )

    fun rows(): List<Row> = session?.entries?.map { entry ->
        Row(
            id = entry.id,
            label = (if (entry.confirmed) "✔ " else "") + rowLabel(entry),
            color = entry.color,
            confirmed = entry.confirmed,
            collectable = entry.status != Status.Unknown
        )
    } ?: emptyList()

    /** The click on an entry's line, from chat or from the checklist screen. */
    fun toggle(id: Int, announce: Boolean = true) {
        val s = session ?: run {
            ChatUtils.sendWithPrefix("No collection running.")
            return
        }
        // by the id it was given, never by position: the list is sorted for display after the
        // ids are handed out, so the entry sitting at index n is not entry n
        val entry = s.entries.firstOrNull { it.id == id } ?: run {
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

        // a ticked plant stops being lit: the highlights are the pile still to sort, so what was
        // just confirmed disappearing from it is the feedback that the click landed
        if (entry.confirmed) {
            entry.stands.forEach {
                standColors.remove(it)
                EntityUtils.remove(it, this)
            }
        } else {
            entry.stands.forEach {
                standColors[it] = entry.color
                EntityUtils.add(it, this)
            }
        }

        if (announce) sendLine(entry)
    }

    /**
     * A diagnosis taken during a run, which outranks everything the scan decided: every entry over
     * that footprint is dropped and rebuilt from the stands actually standing there.
     */
    /**
     * The block the item in a stand's hand hangs inside, modelled from its shoulder, pose and yaw.
     * An approximation, but the only question is which whole block the item sits in.
     */
    fun heldItemBlock(stand: ArmorStand): BlockPos? {
        val holdsItem = !stand.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty ||
                !stand.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty
        if (!holdsItem) return null

        val scale = if (stand.isSmall) 0.5 else 1.0
        val pose = stand.rightArmPose

        // signs pinned by two known poses: an arm at x = -90 holds its item in front of the
        // stand, and a right arm at z = +90 holds it out away from the body
        val arm = Vec3(0.0, -10.0 / 16.0, 0.0)
            .xRot(Math.toRadians(pose.x.toDouble()).toFloat())
            .yRot(-Math.toRadians(pose.y.toDouble()).toFloat())
            .zRot(-Math.toRadians(pose.z.toDouble()).toFloat())

        val local = Vec3(-5.0 / 16.0, 22.0 / 16.0, 0.0).add(arm).scale(scale)
        val turned = local.yRot(-Math.toRadians(stand.yRot.toDouble()).toFloat())

        return BlockPos.containing(stand.position().add(turned))
    }

    /** [hit] is the block or stand the tool was pointed at; only its x and z are used. */
    fun correct(diagnosed: CropDefinition, diagnosedStage: Int, hit: BlockPos) {
        val s = session ?: return
        val client = Minecraft.getInstance()
        if (client.level !== s.level) return

        val standingOn = BlockPos(hit.x, GREENHOUSE_SOIL_Y, hit.z)

        val w = diagnosed.footprint.width
        val h = diagnosed.footprint.height

        // the same net the scan casts, but over this plant's whole footprint and blind to earlier
        // claims, and a block wider each way since a stand may reach in from next door
        val stands = s.level.getEntitiesOfClass(
            ArmorStand::class.java,
            AABB(
                standingOn.x - 1.0, standingOn.y - 2.0, standingOn.z - 1.0,
                standingOn.x + w + 1.0, standingOn.y + PLANT_HEIGHT.toDouble(), standingOn.z + h + 1.0
            )
        )
            .filterNot { it.isMarker }
            .filterNot { PlayerUtils.getSkullHash(it) == null && !it.hasCustomName() }
            // the plot's own marker head hovers high over every greenhouse without being flagged
            // a marker, and once floated seven blocks up into a snoozling export
            .filterNot { PlayerUtils.getSkullHash(it) in PLOT_MARKER_SKINS }
            // a stand holding an item belongs where the item hangs, not where its feet are: the
            // jellybean's smallest looks stand in the next block over with an arm reached out
            .filter { stand ->
                val claimed = heldItemBlock(stand) ?: stand.blockPosition()

                claimed.x in standingOn.x until standingOn.x + w &&
                        claimed.z in standingOn.z until standingOn.z + h
            }

        // a diagnosis on a root names the devourer, but what stands there is the roots
        val roots = CropRegistry.all.firstOrNull { it.name == DEVOURER_ROOTS }
        val rootSkulls = roots?.stageDefs.orEmpty()
            .flatMap { if (it is CropStagePattern) it.expand() else listOf(it) }
            .flatMap { it.armorStands.orEmpty() }
            .mapNotNull { it.hashString }
            .toSet()
        val onRoots = roots != null && diagnosed.name == DEVOURER &&
                stands.any { PlayerUtils.getSkullHash(it) in rootSkulls }

        val def = if (onRoots) roots!! else diagnosed
        val stage = if (onRoots) 1 else diagnosedStage

        // a mutation the player put down in their own garden wears its bought look, which is
        // recorded apart from the grown look of the same stage and matched apart from it
        val ownGarden = LocationAPI.island == SkyBlockIsland.GARDEN && !LocationAPI.isGuest
        val placedLook = ownGarden && def.isMutation && GreenhouseData.getCurrentGrid()?.let { grid ->
            grid.getSlotAt(standingOn, false)?.let { grid.elementCovering(it) }?.instance?.placed
        } == true

        val absorbed = s.entries.filter { entry ->
            val ew = entry.def?.footprint?.width ?: 1
            val eh = entry.def?.footprint?.height ?: 1

            val overlaps = entry.origin.x < standingOn.x + w && standingOn.x < entry.origin.x + ew &&
                    entry.origin.z < standingOn.z + h && standingOn.z < entry.origin.z + eh

            overlaps || entry.stands.any { it in stands }
        }

        absorbed.forEach { entry ->
            s.entries.remove(entry)
            entry.stands.forEach {
                standColors.remove(it)
                EntityUtils.remove(it, this)
            }
        }

        // the diagnosis names the plant, but the definitions may already describe this very
        // stage: a fresh entry is only unrecorded when nothing recorded matches what stands here
        val recorded = def.stageDefs
            .flatMap { if (it is CropStagePattern) it.expand() else listOf(it) }
            .filter { stage in it.stageRange && it.placed == placedLook }
            .map { it.matchesStage(standingOn, stands, def.footprint, def.rotatesWithPlot) }
            .firstOrNull { it.matched }

        val status = when {
            recorded == null && placedLook -> Status.PlacedMissing
            recorded == null -> Status.Unrecorded
            recorded.rotationLegacy -> Status.Legacy
            else -> sizeMismatch(def, stage, stands) ?: Status.Current
        }

        if (status == Status.Legacy) PlantDex.noteLegacy(def.name, stage..stage)

        // a stage matched from a recording that never said how its stands are turned can be
        // matched but not drawn, so a run is the moment to say it is worth taking again
        val turned = if (status == Status.Current && PlantDex.needsRotation(def, stage)) {
            Status.Unturned
        } else {
            status
        }

        addEntry(
            def = def,
            origin = standingOn,
            stands = stands,
            status = turned,
            stageText = stage.toString(),
            stageNum = stage,
            names = stands.standNames()
        )

        val note = if (absorbed.isEmpty()) "" else ", replacing ${absorbed.size} earlier guess(es)"
        val matcher = if (recorded != null) "matcher matched stage $stage" else "matcher found nothing at stage $stage"

        s.entries.lastOrNull()?.let { entry ->
            entry.placedLook = placedLook
            val placedNote = if (placedLook) " (placed)" else ""
            entry.toolNote = "Tool: stage $diagnosedStage/${diagnosed.maxStage}$placedNote, $matcher - ${turned.label}, " +
                    "started at (${standingOn.x}, ${standingOn.z})$note"
            sendLine(entry)
        }
    }

    /** The exported stage with the bought-look flag on it, so it never stands in for the grown look. */
    private fun markPlaced(code: String): String =
        Regex("""(\d+\.\.\d+)(\s*\)\s*)$""").replace(code) { "${it.groupValues[1]},\n    placed = true${it.groupValues[2]}" }

    // ------------------------------------------------------------------ output

    /** Ends the run without writing anything, for the scan that found nothing worth keeping. */
    fun quit() {
        if (session == null) {
            ChatUtils.sendWithPrefix("No collection running.")
            return
        }

        clear()
        ChatUtils.sendWithPrefix("Collection dismissed, nothing written.")
    }

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
                    quiet = true,
                    knownStands = entry.stands
                )?.let { if (entry.placedLook) markPlaced(it) else it }
                appendLine(code ?: "// world went away while writing this one")
                appendLine()
            }
        }

        val dir = File("config/magicaddons/collected")
        dir.mkdirs()
        // a file of one crop is named after it, so a run per crop stays easy to tell apart
        val crops = confirmed.mapNotNull { it.def?.name }.toSet()
        val stamp = System.currentTimeMillis()
        val file = if (crops.size == 1) {
            File(dir, "collected_${crops.first().replace(' ', '_')}_$stamp.txt")
        } else {
            File(dir, "collect-$stamp.txt")
        }
        file.writeText(text)

        s.finishedAt = Instant.now()

        ChatUtils.sendWithPrefix(
            Component.literal("Wrote ${confirmed.size} plants to ${file.path}")
                .withStyle(ChatFormatting.GREEN)
        )
    }

    private fun rowLabel(entry: Entry): String {
        // a nameless plant is still told apart from the next one by the skull it carries
        val name = entry.def?.name
            ?: entry.names.firstOrNull()
            ?: entry.stands.firstNotNullOfOrNull { PlayerUtils.getSkullHash(it) }?.take(12)?.plus("…")
            ?: "unknown"
        val stage = entry.stageText?.let { "stage $it" } ?: "stage ?"

        return entry.toolNote ?: "$name (${entry.origin.x}, ${entry.origin.z}) $stage — ${entry.status.label}"
    }

    private fun sendLine(entry: Entry) {
        val mark = if (entry.confirmed) "[✔] " else ""
        val body = "$mark[${entry.id}] ${rowLabel(entry)}"

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
            if (entry.confirmed) return@forEach

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
