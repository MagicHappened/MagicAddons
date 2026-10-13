package org.magic.magicaddons.commands.debug

import com.mojang.blaze3d.vertex.PoseStack
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import java.io.File
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.StandReader
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.PlantStage
import org.magic.magicaddons.data.greenhouse.plot.GREENHOUSE_SOIL_Y
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.handlers.ModFiles
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.render.WorldRenderer
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.PlayerUtils
import org.magic.magicaddons.util.SBLocation
import org.magic.magicaddons.util.getBuildableArea
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI

object CropCollector : EntityUtils.HighlightSource {

    override val highlightPriority: Int = 100

    override fun highlightColor(entity: Entity): Int = standColors[entity] ?: GRAY

    private const val GRID_SIZE: Int = 10

    private const val DEVOURER: String = "Devourer"
    private const val DEVOURER_ROOTS: String = "DevourerRoots"

    private const val MAX_PLANT_HEIGHT: Int = 15

    private const val FINISHED_HIGHLIGHT_SECONDS: Long = 5

    private const val GRAY: Int = 0xFF9E9E9E.toInt()
    private const val UNKNOWN_WHITE: Int = 0xFFFFFFFF.toInt()

    private const val BLOCK_ALPHA: Int = 0x38

    private const val HELD_ITEM_MARKER_RADIUS: Double = 0.0625
    private const val HELD_ITEM_MARKER_RANGE: Double = 32.0
    private const val HELD_ITEM_MARKER_COLOR: Int = 0xFFFF00FF.toInt()

    private val CROP_PALETTE_COLORS: IntArray = intArrayOf(
        0xFFFF5555.toInt(), 0xFF55FF55.toInt(), 0xFF5599FF.toInt(), 0xFFFFAA00.toInt(),
        0xFFFF55FF.toInt(), 0xFF55FFFF.toInt(), 0xFFFFFF55.toInt(), 0xFFAA77FF.toInt(),
        0xFFFF9999.toInt(), 0xFF99CC66.toInt(), 0xFF66CCCC.toInt(), 0xFFCC9966.toInt()
    )

    private enum class PlantStatus(val label: String) {
        MatchesData("matches current data"),

        StemAgeVaries("stem ages differ"),

        Unrecorded("unrecorded"),

        Unknown("unknown crop")
    }

    private class PlantEntry(
        val id: Int,
        val def: CropDefinition?,
        val origin: BlockPos,
        val stands: List<ArmorStand>,
        val status: PlantStatus,

        val stageText: String?,
        val stageNum: Int?,
        val names: Set<String>,
        val color: Int,
        var confirmed: Boolean = false,
        var boxes: List<AABB> = emptyList(),
        var toolNote: String? = null
    )

    private class CollectorSession(
        val level: ClientLevel,
        val gridOrigin: BlockPos,
        val entries: MutableList<PlantEntry> = mutableListOf()
    ) {
        var finishedAt: Instant? = null
    }

    private var session: CollectorSession? = null

    fun isCollectorActive(): Boolean =
        session?.let { it.finishedAt == null && Minecraft.getInstance().level === it.level } == true

    private val standColors: MutableMap<Entity, Int> = mutableMapOf()
    private val cropColors: MutableMap<String, Int> = mutableMapOf()

    fun scanGreenhouse() {
        val client = Minecraft.getInstance()
        val level = client.level ?: return

        val origin = getPlotOrigin() ?: run {
            ChatUtils.sendWithPrefix(
                Component.literal("Nothing to collect: stand in a greenhouse plot first.")
                    .withStyle(ChatFormatting.RED)
            )
            return
        }

        clearCollectorSession()

        val collectorSession = CollectorSession(level, origin)
        session = collectorSession

        ChatUtils.sendWithPrefix(
            "Collecting from (${origin.x}, ${origin.y}, ${origin.z}) to " +
                    "(${origin.x + GRID_SIZE - 1}, ${origin.y}, ${origin.z + GRID_SIZE - 1})"
        )

        val entities = level.getEntitiesOfClass(
            ArmorStand::class.java,
            AABB(
                origin.x.toDouble(), origin.y - 2.0, origin.z.toDouble(),
                origin.x + GRID_SIZE.toDouble(), origin.y + MAX_PLANT_HEIGHT.toDouble(), origin.z + GRID_SIZE.toDouble()
            )
        )
            .filterNot { it.isMarker }
            .filterNot {
                PlayerUtils.getSkullHash(it) == null && !it.hasCustomName() &&
                        EntityUtils.heldItem(it) == null
            }.toMutableList()

        val matchedPlants = mutableListOf<MatchedPlant>()

        for (dx in 0 until GRID_SIZE) {
            for (dz in 0 until GRID_SIZE) {

                val slotPos = origin.offset(dx, 0, dz)
                val soilState = level.getBlockState(slotPos)

                if (soilState.isAir) continue

                val foundPlant = GreenhouseGrid.matchPlantAt(
                    slotPos,
                    soilState.block,
                    entities,
                    LayoutSlot(slotPos.x, slotPos.z, soilState.block)
                ) ?: continue

                val usedStands = foundPlant.stands.orEmpty().filterIsInstance<ArmorStand>()
                entities.removeAll(usedStands.toSet())

                val def = foundPlant.plant.cropDef

                val (text, num) = when (val g = foundPlant.plant.growthStage) {
                    is PlantStage.Known -> g.stage.toString() to g.stage
                    is PlantStage.Estimated -> "${g.range.first}..${g.range.last}" to null
                    else -> null to null
                }

                val status = when {
                    num != null && def.stemAgeVaries && !stemAgesMatch(def, num, slotPos, usedStands) -> PlantStatus.StemAgeVaries
                    else -> PlantStatus.MatchesData
                }

                matchedPlants += MatchedPlant(def, slotPos, usedStands, status, text, num)
            }
        }

        matchedPlants.forEach { matched ->
            val unrecordedStands = entities.filter { stand ->
                val cell = EntityUtils.cellOf(stand)
                identifyDefinitionForStand(stand) == matched.def &&
                        cell.x - matched.origin.x in 0 until matched.def.footprint.width &&
                        cell.z - matched.origin.z in 0 until matched.def.footprint.height
            }
            entities.removeAll(unrecordedStands.toSet())

            val stands = matched.stands + unrecordedStands
            val status = if (unrecordedStands.isEmpty()) matched.status else PlantStatus.Unrecorded
            addEntry(matched.def, matched.origin, stands, status, matched.stageText, matched.stageNum, stands.standNames())
        }

        for ((def, standsOfCrop) in entities.groupBy { identifyDefinitionForStand(it) }) {
            if (def == null) {
                standsOfCrop.groupBy { EntityUtils.cellOf(it).atY(origin.y) }.forEach { (pos, stands) ->
                    addEntry(null, pos, stands, PlantStatus.Unknown, null, null, stands.standNames())
                }
                continue
            }

            for (cluster in clusterDefinitionStandsByFootprint(standsOfCrop, def)) {
                addEntry(
                    def,
                    guessOrigin(cluster, def, origin.y),
                    cluster,
                    PlantStatus.Unrecorded,
                    null, null,
                    cluster.standNames()
                )
            }
        }

        val coveredBlocks = mutableSetOf<Long>()
        collectorSession.entries.forEach { entry ->
            val width = entry.def?.footprint?.width ?: 1
            val height = entry.def?.footprint?.height ?: 1
            for (cx in 0 until width) {
                for (cz in 0 until height) {
                    coveredBlocks.add(BlockPos.asLong(entry.origin.x + cx, entry.origin.y, entry.origin.z + cz))
                }
            }
        }

        for (dx in 0 until GRID_SIZE) {
            for (dz in 0 until GRID_SIZE) {
                val slotPos = origin.offset(dx, 0, dz)

                if (slotPos.asLong() in coveredBlocks) continue
                if (level.getBlockState(slotPos).isAir) continue

                val above = level.getBlockState(slotPos.above())
                if (above.isAir) continue

                val candidates = defsForBlockStateMatchingSoil(above, level.getBlockState(slotPos).block)
                val described = describeState(above)

                when {
                    candidates.size == 1 -> addEntry(
                        candidates.single(), slotPos, emptyList(),
                        PlantStatus.Unrecorded, null, null, setOf(described)
                    )

                    candidates.isEmpty() -> addEntry(
                        null, slotPos, emptyList(),
                        PlantStatus.Unknown, null, null, setOf(described)
                    )

                    else -> addEntry(
                        null, slotPos, emptyList(), PlantStatus.Unknown, null, null,
                        setOf(described + " \u2014 " + candidates.joinToString("/") { it.name })
                    )
                }
            }
        }

        collectorSession.entries.sortBy { it.status.ordinal.let { statusNum -> if (it.status == PlantStatus.MatchesData) 9 else statusNum } }

        sendInstructions(collectorSession.entries.size)
    }

    private fun sendInstructions(found: Int) {
        ChatUtils.sendWithCommand("Found $found plants. Click here to send a guide", GUIDE_COMMAND)
    }

    fun sendGuide() {
        ChatUtils.send(hint("Open the collection screen with G keybind (only while this is active)"))

        ChatUtils.send(
            hint("Correct plants that seem incorrect or the mod says they've matched, but actually don't exist in the ")
                .append(
                    ChatUtils.buildStyled(
                        "plantDex",
                        ChatFormatting.AQUA,
                        Component.literal("click to run $PLANT_DEX_MISSING_COMMAND"),
                        ClickEvent.RunCommand(PLANT_DEX_MISSING_COMMAND),
                        underlined = true,
                    )
                )
                .append(hint(" with the diagnostic tool"))
        )

        ChatUtils.send(
            hint("After done press \"Write the file\" in the screen and send the file to developer to go through it")
        )
    }

    private fun sendLine(entry: PlantEntry) {
        val mark = if (entry.confirmed) "[✔] " else ""
        val body = "$mark[${entry.id}] ${rowLabel(entry)}"

        ChatUtils.send(Component.literal("  ").append(Component.literal(body)))
    }

    private const val GUIDE_COMMAND: String = "/ma debug farming collect guide"

    private fun hint(text: String): MutableComponent =
        Component.literal(text).withStyle(ChatFormatting.GRAY)

    private const val PLANT_DEX_MISSING_COMMAND: String = "/ma debug farming plantDex missing"

    private fun stemAgesMatch(def: CropDefinition, stage: Int, origin: BlockPos, stands: List<ArmorStand>): Boolean =
        looksAtStage(def, stage).any { it.matchesStage(origin, stands, def) != null }

    private fun looksAtStage(def: CropDefinition, stage: Int): List<CropStage> =
        def.growingStages.filter { stage in it.stageRange } + if (stage == def.stagePlacedAt) def.placedStages else emptyList()

    private fun recordedPlantAt(origin: BlockPos): Plant? {
        val grid = GreenhouseData.getCurrentGrid() ?: return null
        return grid.getSlotAt(origin, matchY = false)?.let { grid.layout.plantCovering(it) }
    }

    private fun stageRangeOf(entry: PlantEntry): List<Int>? {
        if (entry.stageNum != null) return null
        return entry.stageText?.split("..")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }
    }

    private fun addEntry(
        def: CropDefinition?,
        origin: BlockPos,
        stands: List<ArmorStand>,
        status: PlantStatus,
        stageText: String?,
        stageNum: Int?,
        names: Set<String>
    ) {
        val s = session ?: return

        val color = when (status) {
            PlantStatus.MatchesData -> GRAY
            PlantStatus.Unknown -> UNKNOWN_WHITE
            else -> def?.let { colorFor(it.name) } ?: UNKNOWN_WHITE
        }

        val entry = PlantEntry(
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

    private fun normalize(text: String): String = text.lowercase().filter { it.isLetter() }

    private val defsByState: Map<BlockState, List<CropDefinition>> by lazy {
        buildMap<BlockState, MutableList<CropDefinition>> {
            CropRegistry.allCrops.forEach { def ->
                (def.growingStages + def.placedStages)
                    .flatMap { it.blocks.orEmpty() }
                    .map { it.blockState }
                    .distinct()
                    .forEach { getOrPut(it) { mutableListOf() }.add(def) }
            }
        }
    }

    private fun defsForBlockStateMatchingSoil(
        state: BlockState,
        soil: Block
    ): List<CropDefinition> = defsByState[state].orEmpty().filter { soil in it.requiredSoil }

    private fun describeState(state: BlockState): String =
        state.toString()
            .removePrefix("Block{minecraft:")
            .replace("}", "")

    private fun ArmorStand.standName(): String? = customName?.string

    private fun List<ArmorStand>.standNames(): Set<String> =
        mapNotNull { it.standName() }.toSet()


    private val defsByHash: Map<String, CropDefinition> by lazy {
        val skullHashToCropDefs = mutableMapOf<String, MutableSet<CropDefinition>>()

        CropRegistry.allCrops.forEach { def ->
            (def.growingStages + def.placedStages).forEach { stage ->
                stage.armorStands?.forEach { stand ->
                    stand.hashString?.let { skullHashToCropDefs.getOrPut(it) { mutableSetOf() }.add(def) }
                }
            }
        }

        skullHashToCropDefs.filterValues { it.size == 1 }.mapValues { it.value.first() }
    }

    private fun getPlotOrigin(): BlockPos? {
        if (!SBLocation.Garden.inside()) return null

        val plot = PlotAPI.getCurrentPlot() ?: return null
        val collectable = if (LocationAPI.isGuest) !plot.isBarn else plot.data?.isGreenhouse == true
        if (!collectable) return null

        val area = plot.getBuildableArea()

        return BlockPos(area.minX.toInt(), GREENHOUSE_SOIL_Y, area.minZ.toInt())
    }

    private class MatchedPlant(
        val def: CropDefinition,
        val origin: BlockPos,
        val stands: List<ArmorStand>,
        val status: PlantStatus,
        val stageText: String?,
        val stageNum: Int?
    )

    private fun identifyDefinitionForStand(stand: ArmorStand): CropDefinition? =
        defForStandName(stand.standName())
            ?: PlayerUtils.getSkullHash(stand)?.let { defsByHash[it] }


    private fun defForStandName(name: String?): CropDefinition? {
        val n = name?.let(::normalize) ?: return null
        if (n.isEmpty()) return null

        return CropRegistry.allCrops
            .filter { n.startsWith(normalize(it.name)) }
            .maxByOrNull { normalize(it.name).length }
    }

    private fun clusterDefinitionStandsByFootprint(stands: List<ArmorStand>, def: CropDefinition): List<List<ArmorStand>> {
        val radius = max(def.footprint.width, def.footprint.height) - 1
        val standsRemaining = stands.toMutableList()
        val clusters = mutableListOf<List<ArmorStand>>()

        while (standsRemaining.isNotEmpty()) {
            val cluster = mutableListOf(standsRemaining.removeFirst())
            var clusterGrew = true

            while (clusterGrew) {
                clusterGrew = false
                val standNearCluster = standsRemaining.filter { candidate ->
                    cluster.any { member ->
                        val a = EntityUtils.cellOf(candidate)
                        val b = EntityUtils.cellOf(member)
                        max(abs(a.x - b.x), abs(a.z - b.z)) <= radius
                    }
                }
                if (standNearCluster.isNotEmpty()) {
                    cluster.addAll(standNearCluster)
                    standsRemaining.removeAll(standNearCluster.toSet())
                    clusterGrew = true
                }
            }

            clusters.add(cluster)
        }

        return clusters
    }

    // guess plant origin by the mid-point of its furthest stands
    private fun guessOrigin(cluster: List<ArmorStand>, def: CropDefinition, soilY: Int): BlockPos {
        val xs = cluster.map { EntityUtils.shownAt(it).x }
        val zs = cluster.map { EntityUtils.shownAt(it).z }
        val midX = (xs.min() + xs.max()) / 2.0
        val midZ = (zs.min() + zs.max()) / 2.0

        return BlockPos(
            (midX - def.footprint.width / 2.0).roundToInt(),
            soilY,
            (midZ - def.footprint.height / 2.0).roundToInt()
        )
    }

    private fun boxesFor(entry: PlantEntry): List<AABB> {
        val level = session?.level ?: return emptyList()
        val width = entry.def?.footprint?.width ?: 1
        val height = entry.def?.footprint?.height ?: 1

        return buildList {
            for (dx in 0 until width) {
                for (dz in 0 until height) {
                    val soil = entry.origin.offset(dx, 0, dz)
                    add(AABB(soil))

                    var pos = soil.above()
                    while (!level.getBlockState(pos).isAir && pos.y <= soil.y + MAX_PLANT_HEIGHT) {
                        add(AABB(pos))
                        pos = pos.above()
                    }
                }
            }
        }
    }

    private fun colorFor(cropName: String): Int =
        cropColors.getOrPut(cropName) { CROP_PALETTE_COLORS[cropColors.size % CROP_PALETTE_COLORS.size] }

    data class ChecklistRow(
        val id: Int,
        val label: String,
        val color: Int,
        val confirmed: Boolean,
        val isCollectable: Boolean
    )

    fun rows(): List<ChecklistRow> = session?.entries?.map { entry ->
        ChecklistRow(
            id = entry.id,
            label = (if (entry.confirmed) "✔ " else "") + rowLabel(entry),
            color = entry.color,
            confirmed = entry.confirmed,
            isCollectable = entry.status != PlantStatus.Unknown
        )
    } ?: emptyList()

    fun toggleEntry(entryId: Int) {
        val s = session ?: run {
            ChatUtils.sendWithPrefix("No collection running.")
            return
        }
        val entry = s.entries.firstOrNull { it.id == entryId } ?: run {
            ChatUtils.sendWithPrefix("No entry $entryId in this run.")
            return
        }

        if (entry.status == PlantStatus.Unknown) {
            ChatUtils.sendWithPrefix(
                "${entry.names.firstOrNull() ?: "That"} has no definition to anchor by"
            )
            return
        }

        entry.confirmed = !entry.confirmed

        // toggle highlighting for selected entries to better see which one you pick
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

    }

    fun correctEntries(diagnosed: CropDefinition, diagnosedStage: Int, hit: BlockPos) {
        val s = session ?: return
        val client = Minecraft.getInstance()
        if (client.level !== s.level) return

        val standingOn = BlockPos(hit.x, GREENHOUSE_SOIL_Y, hit.z)

        val w = diagnosed.footprint.width
        val h = diagnosed.footprint.height

        val stands = s.level.getEntitiesOfClass(
            ArmorStand::class.java,
            AABB(
                standingOn.x - 1.0, standingOn.y - 2.0, standingOn.z - 1.0,
                standingOn.x + w + 1.0, standingOn.y + MAX_PLANT_HEIGHT.toDouble(), standingOn.z + h + 1.0
            )
        )
            .filterNot { it.isMarker }
            .filterNot {
                PlayerUtils.getSkullHash(it) == null && !it.hasCustomName() &&
                        EntityUtils.heldItem(it) == null
            }
            .filter { stand ->
                val claimed = EntityUtils.cellOf(stand)

                claimed.x in standingOn.x until standingOn.x + w &&
                        claimed.z in standingOn.z until standingOn.z + h
            }
        val roots = CropRegistry.allCrops.firstOrNull { it.name == DEVOURER_ROOTS }
        val rootSkulls = roots?.growingStages.orEmpty()
            .flatMap { it.armorStands.orEmpty() }
            .mapNotNull { it.hashString }
            .toSet()
        val onRoots = roots != null && diagnosed.name == DEVOURER &&
                stands.any { PlayerUtils.getSkullHash(it) in rootSkulls }

        val def = if (onRoots) roots else diagnosed
        val stage = if (onRoots) 1 else diagnosedStage

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

        val recorded = looksAtStage(def, stage)
            .firstNotNullOfOrNull { it.matchesStage(standingOn, stands, def) }
        val recordedIgnoringStemAge = recorded ?: looksAtStage(def, stage)
            .firstNotNullOfOrNull { it.matchesStage(standingOn, stands, def, ignoreStemAge = true) }

        val status = when {
            recorded == null && recordedIgnoringStemAge != null -> PlantStatus.StemAgeVaries
            recorded == null -> PlantStatus.Unrecorded
            else -> PlantStatus.MatchesData
        }

        addEntry(
            def = def,
            origin = standingOn,
            stands = stands,
            status = status,
            stageText = stage.toString(),
            stageNum = stage,
            names = stands.standNames()
        )

        val note = if (absorbed.isEmpty()) "" else ", replacing ${absorbed.size} earlier guess(es)"
        val matcher = if (recorded != null) "matcher matched stage $stage" else "matcher found nothing at stage $stage"

        s.entries.lastOrNull()?.let { entry ->
            entry.toolNote = "${def.name}: tool says stage $diagnosedStage/${diagnosed.maxStage}, $matcher - ${status.label}, " +
                    "started at (${standingOn.x}, ${standingOn.z})$note"
            sendLine(entry)
        }
    }

    fun quit() {
        if (session == null) {
            ChatUtils.sendWithPrefix("No collection running.")
            return
        }

        clearCollectorSession()
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

        val stamp = System.currentTimeMillis()
        val plants = JsonArray()

        confirmed.forEach { entry ->
            val def = entry.def
            val parts = CropStageExporter.partsJson(entry.origin, def, entry.stands) ?: return@forEach
            val recorded = recordedPlantAt(entry.origin)

            plants.add(JsonObject().apply {
                addProperty("crop", def?.name)
                addProperty("elementId", def?.elementId)
                add("origin", CropStageExporter.jsonArrayOf(entry.origin.x, entry.origin.y, entry.origin.z))
                add("worldStep", parts.get("worldStep"))
                addProperty("status", entry.status.label)
                addProperty("stage", entry.stageNum)
                add("stageRange", stageRangeOf(entry)?.let { CropStageExporter.jsonArrayOf(*it.toTypedArray()) } ?: JsonNull.INSTANCE)
                addProperty("water", recorded?.waterLevel)
                addProperty("waterExact", recorded?.waterLevel?.let { recorded.waterExact })
                add("names", JsonArray().apply { entry.names.forEach { add(it) } })
                add("blocks", parts.get("blocks"))
                add("stands", parts.get("stands"))
            })
        }

        val collection = JsonObject().apply {
            addProperty("collectedAt", stamp)
            add("gridOrigin", CropStageExporter.jsonArrayOf(s.gridOrigin.x, s.gridOrigin.y, s.gridOrigin.z))
            addProperty("time", if (GreenhouseGrid.dayOrNightNow() == StandReader.NEEDS_NIGHT) "night" else "day")
            add("plants", plants)
        }

        val dir = ModFiles.modDir.resolve("collected").toFile()
        val crops = confirmed.mapNotNull { it.def?.name }.toSet()
        val file = if (crops.size == 1) {
            File(dir, "collected_${crops.first().replace(' ', '_')}_$stamp.json")
        } else {
            File(dir, "collect-$stamp.json")
        }
        ModFiles.writeTextAtomically(file.toPath(), CropStageExporter.prettyJson(collection) + "\n")

        s.finishedAt = Instant.now()

        ChatUtils.sendWithPrefix(
            Component.literal("Wrote ${confirmed.size} plants to ${file.path}")
                .withStyle(ChatFormatting.GREEN)
        )
    }

    private fun rowLabel(entry: PlantEntry): String {
        val name = entry.def?.name
            ?: entry.names.firstOrNull()
            ?: entry.stands.firstNotNullOfOrNull { PlayerUtils.getSkullHash(it) }?.take(12)?.plus("…")
            ?: "unknown"
        val stage = entry.stageText?.let { "stage $it" } ?: "stage ?"

        return entry.toolNote ?: "$name (${entry.origin.x}, ${entry.origin.z}) $stage — ${entry.status.label}"
    }

    fun submitHighlights(poseStack: PoseStack, collector: SubmitNodeCollector, cameraPos: Vec3) {
        val s = session ?: return

        val done = s.finishedAt?.let { Instant.now().isAfter(it.plusSeconds(FINISHED_HIGHLIGHT_SECONDS)) } ?: false
        if (done || Minecraft.getInstance().level !== s.level) {
            clearCollectorSession()
            return
        }

        s.entries.forEach { entry ->
            if (entry.confirmed) return@forEach

            entry.boxes.forEach { box ->
                WorldRenderer.markBox(poseStack, collector, cameraPos, box, entry.color, BLOCK_ALPHA)
            }
        }

        s.level.getEntitiesOfClass(ArmorStand::class.java, AABB.ofSize(cameraPos, HELD_ITEM_MARKER_RANGE, HELD_ITEM_MARKER_RANGE, HELD_ITEM_MARKER_RANGE))
            .forEach { stand ->
                val held = EntityUtils.heldItemPosition(stand) ?: return@forEach
                val marker = AABB.ofSize(held, HELD_ITEM_MARKER_RADIUS * 2, HELD_ITEM_MARKER_RADIUS * 2, HELD_ITEM_MARKER_RADIUS * 2)
                WorldRenderer.markBox(poseStack, collector, cameraPos, marker, HELD_ITEM_MARKER_COLOR, BLOCK_ALPHA)
            }
    }

    private fun clearCollectorSession() {
        EntityUtils.removeAllForSource(this)
        standColors.clear()
        cropColors.clear()
        session = null
    }
}
