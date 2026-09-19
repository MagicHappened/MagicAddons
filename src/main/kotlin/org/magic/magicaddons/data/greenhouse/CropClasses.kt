package org.magic.magicaddons.data.greenhouse

import net.minecraft.util.Mth
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.Level
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.StemBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.PlayerUtils
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import kotlin.math.abs
import kotlin.math.floor

sealed interface GrowthStageInfo {

    data class Known(val stage: Int) : GrowthStageInfo

    data class Estimated(val range: IntRange) : GrowthStageInfo

}



data class Footprint(val width: Int, val height: Int) {
    fun spaceAbove(soil: BlockPos, height: Int): AABB = AABB(
        soil.x.toDouble(), soil.y.toDouble(), soil.z.toDouble(),
        (soil.x + width).toDouble(),
        (soil.y + height).toDouble(),
        (soil.z + this.height).toDouble()
    )
}

data class CropArmorStand(
    /** feet position from the footprint centre at soil height */
    val offset: Vec3,
    val isSmall: Boolean,
    val headRotation: Rotations? = null,
    val xRotation: Float? = null,
    val yRotation: Float? = null,
    val hashString: String? = null,
    val nameContains: String? = null,
    /** a held item other than a skull, as "minecraft:gold_block" */
    val heldItemId: String? = null,
    val itemSlot: EquipmentSlot = EquipmentSlot.HEAD,
) {
    companion object {
        fun atOffsets(
            offsets: List<Vec3>,
            isSmall: Boolean,
            rotations: List<Rotations>? = null,
            xRotations: List<Float>? = null,
            yRotations: List<Float>? = null,
            hashString: String? = null,
            nameContains: String? = null,
            heldItemId: String? = null,
            itemSlot: EquipmentSlot = EquipmentSlot.HEAD
        ): List<CropArmorStand> = offsets.mapIndexed { i, offset ->
            CropArmorStand(
                offset = offset,
                isSmall = isSmall,
                headRotation = rotations?.getOrNull(i),
                xRotation = xRotations?.getOrNull(i),
                yRotation = yRotations?.getOrNull(i),
                hashString = hashString,
                nameContains = nameContains,
                heldItemId = heldItemId,
                itemSlot = itemSlot
            )
        }
    }
}
data class CropBlockState(
    val offset: BlockPos,
    val blockState: BlockState,
    val required: Boolean = true
){

    companion object {
        fun atPositions(
            positions: List<BlockPos>,
            blockState: BlockState,
            required: Boolean = true
        ): List<CropBlockState> = positions.map { CropBlockState(it, blockState, required) }
    }
}



open class CropStage(
    val blocks: List<CropBlockState>? = null,
    val armorStands: List<CropArmorStand>? = null,
    val stageRange: IntRange,
    val traits: Map<String, Int> = emptyMap(),
    val readers: List<CropStandReader> = emptyList()
) {

    /** the highest value when several stands are found */
    fun readValues(stands: List<ArmorStand>): Map<String, Int> = readers.mapNotNull { reader ->
        stands.filter { reader.matches(it) }
            .mapNotNull { reader.read(it) }
            .maxOrNull()
            ?.let { reader.key to it }
    }.toMap()

    class StandCache {
        class CachedStand(
            val position: Vec3,
            val skullHash: String?,
            val name: String?,
            val stand: ArmorStand
        ) {
            private val items = HashMap<EquipmentSlot, String?>()

            fun itemIn(slot: EquipmentSlot): String? =
                items.getOrPut(slot) { EntityUtils.itemIdIn(stand, slot) }
        }

        private val cachedStands = HashMap<Int, CachedStand>()

        fun lookUp(stand: ArmorStand): CachedStand = cachedStands.getOrPut(stand.id) {
            CachedStand(
                stand.position(),
                PlayerUtils.getSkullHash(stand),
                stand.customName?.string,
                stand
            )
        }
    }

    fun matchesStage(
        origin: BlockPos,
        remainingStands: List<ArmorStand>,
        footprint: Footprint,
        rotatesWithPlot: Boolean = true,
        ignoreStemAge: Boolean = false,
        standCache: StandCache = StandCache()
    ): StageMatchResult? {
        val level = Minecraft.getInstance().level ?: return null

        var score = 0
        val matchedBlocks = mutableMapOf<BlockPos, BlockState>()

        this.blocks?.forEach { recordedBlock ->
            if (!recordedBlock.required) return@forEach

            val blockPos = origin.offset(recordedBlock.offset)
            val worldBlockState = level.getBlockState(blockPos)

            if (!blocksMatch(worldBlockState, recordedBlock.blockState, ignoreStemAge)) return null

            matchedBlocks[blockPos] = worldBlockState
            score += 1
        }
        val footprintCenter = Vec3(
            origin.x + footprint.width / 2.0,
            origin.y.toDouble(),
            origin.z + footprint.height / 2.0
        )

        val quarterTurns = if (rotatesWithPlot) WorldRotation.quarterTurnsAt(origin.x, origin.z) else 0
        val matchedStands = mutableListOf<Entity>()

        for (recordedStand in this.armorStands.orEmpty()) {
            val expectedOffset = WorldRotation.turned(recordedStand.offset, quarterTurns)

            val matchingStand = remainingStands.firstOrNull { entity ->
                val cachedStand = standCache.lookUp(entity)

                offsetsMatch(cachedStand.position.subtract(footprintCenter), expectedOffset) &&
                        (recordedStand.hashString?.let { it == cachedStand.skullHash } ?: true) &&
                        (recordedStand.nameContains?.let { cachedStand.name?.contains(it) == true } ?: true) &&
                        (recordedStand.heldItemId?.let { it == cachedStand.itemIn(recordedStand.itemSlot) } ?: true)
            } ?: return null

            matchedStands.add(matchingStand)
            score += 2
        }

        val matchingHeadPoses = this.armorStands.orEmpty().zip(matchedStands).count { (recordedStand, stand) ->
            val recordedHeadPose = recordedStand.headRotation ?: return@count false
            (stand as? ArmorStand)?.headPose?.let { headPosesMatch(it, recordedHeadPose) } == true
        }

        return StageMatchResult(
            score = score,
            usedStands = matchedStands,
            matchedBlocks = matchedBlocks,
            matchingHeadPoses = matchingHeadPoses
        )
    }

    /** with [ignoreStemAge] a stem matches at any age */
    private fun blocksMatch(world: BlockState, recorded: BlockState, ignoreStemAge: Boolean): Boolean =
        world == recorded || (ignoreStemAge && recorded.block is StemBlock && world.block == recorded.block)

    private fun headPosesMatch(worldPose: Rotations, recordedPose: Rotations): Boolean =
        abs(Mth.wrapDegrees(worldPose.x() - recordedPose.x())) < HEAD_POSE_TOLERANCE_DEGREES &&
                abs(Mth.wrapDegrees(worldPose.y() - recordedPose.y())) < HEAD_POSE_TOLERANCE_DEGREES &&
                abs(Mth.wrapDegrees(worldPose.z() - recordedPose.z())) < HEAD_POSE_TOLERANCE_DEGREES

    private fun offsetsMatch(actual: Vec3, expected: Vec3): Boolean {
        return abs(actual.x - expected.x) < OFFSET_TOLERANCE &&
                abs(actual.y - expected.y) < OFFSET_TOLERANCE &&
                abs(actual.z - expected.z) < OFFSET_TOLERANCE
    }

    fun toRenderData(
        level: Level,
        baseBlock: BlockPos,
        footprint: Footprint,
        standPoses: Map<String, StandPose> = emptyMap(),
        rotatesWithPlot: Boolean = true
    ): RenderData{
        val renderStands = mutableListOf<ArmorStand>()
        val blockMap = mutableMapOf<BlockPos, BlockState>()

        val quarterTurns = if (rotatesWithPlot) WorldRotation.quarterTurnsAt(baseBlock.x, baseBlock.z) else 0
        val footprintCenter = Vec3(
            baseBlock.x + footprint.width / 2.0,
            baseBlock.y.toDouble(),
            baseBlock.z + footprint.height / 2.0
        )

        blocks?.forEach { recordedBlock ->
            blockMap[baseBlock.offset(recordedBlock.offset)] = recordedBlock.blockState
        }
        armorStands?.forEach { recordedStand ->
            val heldItem = recordedStand.hashString?.let { PlayerUtils.getItemFromHash(it) }
                ?: recordedStand.heldItemId?.let { EntityUtils.itemStackOf(it) }
                ?: return@forEach
            val turnedOffset = WorldRotation.turned(recordedStand.offset, quarterTurns)
            val stand = ArmorStand(
                level,
                footprintCenter.x + turnedOffset.x,
                footprintCenter.y + turnedOffset.y,
                footprintCenter.z + turnedOffset.z
            )

            // isSmall has no public setter
            if (recordedStand.isSmall) {
                stand.entityData.set(
                    ArmorStand.DATA_CLIENT_FLAGS,
                    ArmorStand.CLIENT_FLAG_SMALL.toByte()
                )
            }

            // rendering a held item needs an entity id the world never hands out
            stand.id = FAKE_ENTITY_ID

            stand.isInvisible = true
            // the stand's own rotation wins over the crop's standPoses
            val cropStandPose = recordedStand.hashString?.let { standPoses[it] }
            val headPose = recordedStand.headRotation
                ?: cropStandPose?.headAt(baseBlock.x, baseBlock.z, recordedStand.offset)

            headPose?.let { stand.headPose = it }
            val yaw = Mth.wrapDegrees((recordedStand.yRotation ?: cropStandPose?.yRotation ?: 0f) + 90f * quarterTurns)

            // never ticked, so the previous-tick yaws are set too
            stand.yRot = yaw
            stand.yRotO = yaw
            stand.yBodyRot = yaw
            stand.yBodyRotO = yaw
            stand.yHeadRot = yaw
            stand.yHeadRotO = yaw
            stand.xRot = recordedStand.xRotation ?: cropStandPose?.xRotation ?: 0f
            stand.setItemSlot(if (recordedStand.hashString != null) EquipmentSlot.HEAD else recordedStand.itemSlot, heldItem)
            renderStands.add(stand)
        }
        return RenderData(
            renderStands.toList(),
            blockMap.toMap()
        )
    }


    data class RenderData(
        val stands: List<ArmorStand>,
        val blockMap: Map<BlockPos, BlockState>
    )

    companion object {
        private const val FAKE_ENTITY_ID: Int = -1
        private const val OFFSET_TOLERANCE: Double = 0.01

        /** recorded head poses are several degrees apart at the closest */
        private const val HEAD_POSE_TOLERANCE_DEGREES: Float = 1f
    }
}


class CropStagePattern(
    blocks: List<CropBlockState>? = null,
    armorStands: List<CropArmorStand>? = null,
    stageRange: IntRange,
    traits: Map<String, Int> = emptyMap(),
    val baseStandOffset: Vec3,
    val baseStandStageMultipliers: Map<Int, Int> = emptyMap()
) : CropStage(
    blocks = blocks,
    armorStands = armorStands,
    stageRange = stageRange,
    traits = traits
){
    fun expandToStages(): List<CropStage> = stageRange.map { stage ->
        val offsetMultiplier = baseStandStageMultipliers[stage] ?: (stage - stageRange.first)

        val offsetStands = armorStands?.map { stand ->
            stand.copy(offset = stand.offset.add(baseStandOffset.scale(offsetMultiplier.toDouble())))
        }

        CropStage(
            blocks = blocks,
            armorStands = offsetStands,
            stageRange = stage..stage,
            traits = traits
        )
    }

}
/** how skyblock turns its plants, a quarter turn per `(z - x) mod 4` of the base block */
object WorldRotation {

    fun quarterTurnsAt(x: Int, z: Int): Int = Math.floorMod(z - x, 4)

    fun turned(offset: Vec3, quarterTurns: Int): Vec3 = when (Math.floorMod(quarterTurns, 4)) {
        1 -> Vec3(-offset.z, offset.y, offset.x)
        2 -> Vec3(-offset.x, offset.y, -offset.z)
        3 -> Vec3(offset.z, offset.y, -offset.x)
        else -> offset
    }
}

const val NEVER_DECAYS: Long = -1L

const val THREE_DAY_DECAY_TIME_MS: Long = 3L * 24 * 60 * 60 * 1000
const val FIVE_DAY_DECAY_TIME_MS: Long = 5L * 24 * 60 * 60 * 1000
const val SIX_DAY_DECAY_TIME_MS: Long = 6L * 24 * 60 * 60 * 1000
const val TEN_DAY_DECAY_TIME_MS: Long = 10L * 24 * 60 * 60 * 1000

sealed interface StandPose {

    fun headAt(x: Int, z: Int, offset: Vec3): Rotations

    val xRotation: Float get() = 0f
    val yRotation: Float get() = 0f

    data class Fixed(
        val headRotation: Rotations,
        override val xRotation: Float = 0f,
        override val yRotation: Float = 0f
    ) : StandPose {
        override fun headAt(x: Int, z: Int, offset: Vec3): Rotations = headRotation
    }

    /** poses[(x + z + height) mod size], as the jellybean's canes do */
    data class Cycle(val poses: List<Rotations>) : StandPose {
        override fun headAt(x: Int, z: Int, offset: Vec3): Rotations =
            poses[Math.floorMod(x + z + floor(offset.y + 0.5).toInt(), poses.size)]
    }
}

data class CropDefinition(
    val name: String,
    val skyblockId: SkyBlockId?,
    val aliases: List<SkyBlockId>? = null,
    val stageDefs: List<CropStage>,
    val maxStage: Int = 1,

    val decayTimeMs: Long = THREE_DAY_DECAY_TIME_MS,
    val footprint: Footprint = Footprint(1,1),
    val requiredSoil: Set<Block> = setOf(Blocks.FARMLAND),
    val needsWater: Boolean = true,

    val drainsNeighbours: Boolean = false,
    val isBaseCrop: Boolean = false,
    val isMutation: Boolean = false,
    val displayItem: Item? = null,

    val effects: Set<CropEffect> = emptySet(),

    val standPoses: Map<String, StandPose> = emptyMap(),
    val sleepStages: Set<Int> = emptySet(),
    val rotatesWithPlot: Boolean = true,
    val spawnRule: SpawnRule? = null,
    val chargeRule: ChargeRule? = null,
    val dropMultiplier: Double? = null,
    /** the stems' age changes with something other than the stage */
    val stemAgeVaries: Boolean = false
){
    val stagePlacedAt: Int get() = if (isMutation) maxStage else 1
    val elementId: String get() = skyblockId?.id ?: name
    val stages: List<CropStage> = stageDefs.flatMap { if (it is CropStagePattern) it.expandToStages() else listOf(it) }

    val hasHungerBar: Boolean get() = stages.any { stage -> stage.readers.any { it.key == CropStandReader.HUNGER } }

    override fun toString(): String {
        return name
    }
}

data class StageMatchResult(
    val score: Int,
    val usedStands: List<Entity>,
    val matchedBlocks: Map<BlockPos, BlockState>,
    val matchingHeadPoses: Int
)


data class ScannedPlant(
    val plant: Plant,
    val stands: List<Entity>?,
    val blocks: Map<BlockPos,BlockState>?
)

data class Plant(
    val elementId: String,
    val slot: LayoutSlot,
    var waterLevel: Double? = null,
    var growthStage: GrowthStageInfo? = null,
    var age: Long? = null,
    val cropDef: CropDefinition,
    val readings: MutableMap<String, Int> = mutableMapOf(),
    val alternatives: MutableList<CropDefinition> = mutableListOf(),
) {
    val hasAlternatives: Boolean get() = alternatives.isNotEmpty()

    fun acceptsCrop(crop: CropDefinition): Boolean = crop == cropDef || crop in alternatives

    val acceptedCrops: List<CropDefinition> get() = listOf(cropDef) + alternatives

    val isAsleep: Boolean get() = readings[CropStandReader.ASLEEP] == 1

    val timeOfDayNeeded: Int? get() = readings[CropStandReader.NEEDS_TIME]

    /** 0 to 100, null without a hunger bar */
    val hunger: Int? get() = readings[CropStandReader.HUNGER]

    /** if a tick has passed with negative water, then we don't know if it truly passed or not */
    var waterPredictedInDebt: Boolean = false

    var waterExact: Boolean = false

    var firstSeenStage: Int? = null

    var placed: Boolean = false

    /** electricity gained since the last look, for a crop with a [CropDefinition.chargeRule] */
    var charge: Int = 0

    /** read off its bar or set by a discharge; until then the charge is what the stage implies */
    var chargeKnown: Boolean = false

    var waterBestCase: Double? = null

    val isPlacedMutation: Boolean get() = placed && cropDef.isMutation

    val isCollectable: Boolean get() = isPlacedMutation && (age ?: 1L) <= 0L

    val readyToHarvest: Boolean
        get() = (cropDef.isMutation || cropDef.isBaseCrop) && !isPlacedMutation && (highestStage ?: 0) >= cropDef.maxStage

    val consumesWater: Boolean get() = cropDef.needsWater && !isPlacedMutation && !isFullyGrown

    fun copyForPrediction(slot: LayoutSlot): Plant =
        copy(slot = slot, readings = readings.toMutableMap(), alternatives = alternatives.toMutableList()).also {
            it.waterPredictedInDebt = waterPredictedInDebt
            it.waterExact = waterExact
            it.firstSeenStage = firstSeenStage
            it.placed = placed
            it.waterBestCase = waterBestCase
            it.charge = charge
            it.chargeKnown = chargeKnown
        }

    /** null when the stage is unknown or the crop has one stage */
    fun waterLastsUntilGrown(waterEffectPercent: Int): Boolean? {
        if (!consumesWater || cropDef.drainsNeighbours) return true

        val water = waterLevel ?: return null
        if (water <= WaterModel.DEATH_LEVEL) return false

        val ticksLeft = WaterModel.ticksUntilDeath(water, waterEffectPercent) ?: return true

        // in debt the highest stage is the one the water paid for
        val stage = (if (waterPredictedInDebt) highestStage else lowestStage) ?: return null
        if (cropDef.maxStage <= 1) return null

        return ticksLeft > cropDef.maxStage - stage
    }

    val isFullyGrown: Boolean get() = (lowestStage ?: 0) >= cropDef.maxStage

    val lowestStage: Int?
        get() = when (val stage = growthStage) {
            is GrowthStageInfo.Known -> stage.stage
            is GrowthStageInfo.Estimated -> stage.range.first
            null -> null
        }

    val highestStage: Int?
        get() = when (val stage = growthStage) {
            is GrowthStageInfo.Known -> stage.stage
            is GrowthStageInfo.Estimated -> stage.range.last
            null -> null
        }

    val grewInPlace: Boolean
        get() {
            if (placed) return false
            val firstStage = firstSeenStage ?: return false
            val currentStage = lowestStage ?: return false

            return currentStage > firstStage
        }

    fun needsOtherTimeOfDay(dayOrNight: Int): Boolean {
        val needed = timeOfDayNeeded ?: return false
        val stage = lowestStage
        return needed != dayOrNight && (stage == null || stage < cropDef.maxStage)
    }

    /** null when it never decays or its age is unknown */
    val decayRemainingMs: Long?
        get() {
            val decayTime = cropDef.decayTimeMs
            if (decayTime == NEVER_DECAYS) return null
            val age = age ?: return null
            return (decayTime - age).coerceAtLeast(0L)
        }
}


interface CropDefinitionProvider {
    val definition: CropDefinition
}