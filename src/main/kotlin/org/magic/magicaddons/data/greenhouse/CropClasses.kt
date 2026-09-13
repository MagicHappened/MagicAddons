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
    /** The box a crop of this footprint fills from [soil] up to [height] blocks above it. */
    fun spaceAbove(soil: BlockPos, height: Int): AABB = AABB(
        soil.x.toDouble(), soil.y.toDouble(), soil.z.toDouble(),
        (soil.x + width).toDouble(),
        (soil.y + height).toDouble(),
        (soil.z + this.height).toDouble()
    )
}

data class CropArmorStand(
    /** Where the stand's feet are, measured from the centre of the footprint at soil height. */
    val offset: Vec3,
    val headRotation: Rotations? = null,
    val xRotation: Float? = null,
    val yRotation: Float? = null,
    val hashString: String? = null,
    val containsCustomName: String? = null,
    /** What the stand holds when it holds something other than a skull, as "minecraft:gold_block". */
    val itemId: String? = null,
    /** Which slot [itemId] is carried in. Skulls and nearly everything else ride on the head. */
    val itemSlot: EquipmentSlot = EquipmentSlot.HEAD,
    /** A stand's position is its feet, so size decides where its skull lands. Nearly all are small. */
    val isSmall: Boolean = true,
) {
    companion object {
        fun matcherPattern(
            offsets: List<Vec3>,
            rotations: List<Rotations>? = null,
            xRotations: List<Float>? = null,
            yRotations: List<Float>? = null,
            hashString: String? = null,
            customName: String? = null,
            itemId: String? = null,
            itemSlot: EquipmentSlot = EquipmentSlot.HEAD,
            isSmall: Boolean = true
        ): List<CropArmorStand> {
            val result = mutableListOf<CropArmorStand>()
            offsets.forEachIndexed { i, offset ->
                result.add(
                    CropArmorStand(
                        offset = offset,
                        headRotation = rotations?.getOrNull(i),
                        xRotation = xRotations?.getOrNull(i),
                        yRotation = yRotations?.getOrNull(i),
                        hashString = hashString,
                        containsCustomName = customName,
                        itemId = itemId,
                        itemSlot = itemSlot,
                        isSmall = isSmall
                    )
                )
            }
            return result
        }
    }
}
data class CropBlockState(
    val offset: BlockPos,
    val blockState: BlockState,
    val required: Boolean = true
){

    companion object {
        fun blockStatePattern(
            positions: List<BlockPos>,
            blockState: BlockState,
            required: Boolean = true
        ): List<CropBlockState> {
            val result = mutableListOf<CropBlockState>()
            positions.forEach {
                result.add(
                    CropBlockState(
                        it,
                        blockState,
                        required
                    )
                )
            }
            return result
        }
    }
}



open class CropStage(
    val blocks: List<CropBlockState>? = null,
    val armorStands: List<CropArmorStand>? = null,
    val stageRange: IntRange,
    val traits: Map<String, Int> = emptyMap(),
    val readers: List<CropStandReader> = emptyList()
) {

    fun read(stands: List<ArmorStand>): Map<String, Int> = readers.mapNotNull { reader ->
        stands.firstOrNull { reader.matches(it) }
            ?.let { reader.read(it) }
            ?.let { reader.key to it }
    }.toMap()

    class StandReadings {
        class Reading(
            val position: Vec3,
            val skullHash: String?,
            val name: String?,
            val stand: ArmorStand
        ) {
            private val items = HashMap<EquipmentSlot, String?>()

            fun itemIn(slot: EquipmentSlot): String? =
                items.getOrPut(slot) { EntityUtils.itemIdIn(stand, slot) }
        }

        private val readings = HashMap<Int, Reading>()

        fun of(stand: ArmorStand): Reading = readings.getOrPut(stand.id) {
            Reading(
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
        readings: StandReadings = StandReadings()
    ): StageMatchResult {
        val level = Minecraft.getInstance().level ?: return StageMatchResult.NONE

        var score = 0
        var matchedFirstCandidate = true
        val usedStands = mutableListOf<Entity>()
        val matchedBlocks = mutableMapOf<BlockPos, BlockState>()

        this.blocks?.forEach { blockDef ->
            if (!blockDef.required) return@forEach

            val pos = origin.offset(blockDef.offset)
            val state = level.getBlockState(pos)

            if (state != blockDef.blockState) return StageMatchResult.NONE

            matchedBlocks[pos] = state
            score += 1
        }
        val center = Vec3(
            origin.x + footprint.width / 2.0,
            origin.y.toDouble(),
            origin.z + footprint.height / 2.0
        )

        val worldStep = WorldRotation.step(origin.x, origin.z)

        val candidateSteps = when {
            this.armorStands.isNullOrEmpty() || !rotatesWithPlot -> listOf(0)
            else -> listOf(worldStep, 0).distinct()
        }

        var matchedStands: List<Entity>? = null

        for (step in candidateSteps) {
            val used = mutableListOf<Entity>()
            var allFound = true

            for (standDef in this.armorStands.orEmpty()) {
                val expected = WorldRotation.rotate(standDef.offset, step)

                val match = remainingStands.firstOrNull { entity ->
                    val reading = readings.of(entity)

                    isClose(reading.position.subtract(center), expected) &&
                            (standDef.hashString?.let { it == reading.skullHash } ?: true) &&
                            (standDef.containsCustomName?.let { reading.name?.contains(it) == true } ?: true) &&
                            (standDef.itemId?.let { it == reading.itemIn(standDef.itemSlot) } ?: true)
                }

                if (match == null) {
                    allFound = false
                    break
                }

                used.add(match)
            }

            if (allFound) {
                matchedStands = used
                matchedFirstCandidate = step == candidateSteps.first()
                break
            }
        }

        if (matchedStands == null) return StageMatchResult.NONE

        matchedStands.forEach { match ->
            usedStands.add(match)
            score += 2
        }

        return StageMatchResult(
            matched = true,
            score = score,
            usedStands = usedStands,
            matchedBlocks = matchedBlocks,
            rotationLegacy = !matchedFirstCandidate
        )
    }
    private fun isClose(a: Vec3, b: Vec3, epsilon: Double = 0.01): Boolean {
        return abs(a.x - b.x) < epsilon &&
                abs(a.y - b.y) < epsilon &&
                abs(a.z - b.z) < epsilon
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

        val worldStep = if (rotatesWithPlot) WorldRotation.step(baseBlock.x, baseBlock.z) else 0
        val center = Vec3(
            baseBlock.x + footprint.width / 2.0,
            baseBlock.y.toDouble(),
            baseBlock.z + footprint.height / 2.0
        )

        blocks?.forEach { blockDef ->
            val worldPos = baseBlock.offset(blockDef.offset)
            val state = blockDef.blockState
            blockMap[worldPos] = state
        }
        armorStands?.forEach { standDef ->
            val held = standDef.hashString?.let { PlayerUtils.getItemFromHash(it) }
                ?: standDef.itemId?.let { EntityUtils.itemStackOf(it) }
                ?: return@forEach
            val turned = WorldRotation.rotate(standDef.offset, worldStep)
            val stand = ArmorStand(
                level,
                center.x + turned.x,
                center.y + turned.y,
                center.z + turned.z
            )

            // the flags ride in synched data rather than in setters, which are not ours to call
            if (standDef.isSmall) {
                stand.entityData.set(
                    ArmorStand.DATA_CLIENT_FLAGS,
                    ArmorStand.CLIENT_FLAG_SMALL.toByte()
                )
            }

            // rendering a stand holding an item asks for its entity id and throws without one. Any
            // id will do, so long as the world never handed it out
            stand.id = FAKE_ENTITY_ID

            stand.isInvisible = true
            // an explicit pose on the stand wins; otherwise the role says, and the role may
            // care where in the world the plant stands
            val role = standDef.hashString?.let { standPoses[it] }
            val head = standDef.headRotation
                ?: role?.headAt(baseBlock.x, baseBlock.z, standDef.offset)

            head?.let { stand.headPose = it }
            val yaw = Mth.wrapDegrees((standDef.yRotation ?: role?.yRotation ?: 0f) + 90f * worldStep)

            // a ghost stand is never ticked, so every yaw field is set here
            stand.yRot = yaw
            stand.yRotO = yaw
            stand.yBodyRot = yaw
            stand.yBodyRotO = yaw
            stand.yHeadRot = yaw
            stand.yHeadRotO = yaw
            stand.xRot = standDef.xRotation ?: role?.xRotation ?: 0f
            stand.setItemSlot(if (standDef.hashString != null) EquipmentSlot.HEAD else standDef.itemSlot, held)
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
    }
}


class CropStagePattern(
    blocks: List<CropBlockState>? = null,
    armorStands: List<CropArmorStand>? = null,
    stageRange: IntRange,
    traits: Map<String, Int> = emptyMap(),
    val baseStageStandOffset: Vec3,
    val stageOffsetMultipliers: Map<Int, Int> = emptyMap()
) : CropStage(
    blocks = blocks,
    armorStands = armorStands,
    stageRange = stageRange,
    traits = traits
){
    fun expand(): List<CropStage> {
        val result = mutableListOf<CropStage>()

        val start = stageRange.first

        for (stage in stageRange) {

            val multiplier = stageOffsetMultipliers[stage]
                ?: (stage - start) // good fallback

            val newStands = armorStands?.map { stand ->
                stand.copy(
                    offset = stand.offset.add(
                        baseStageStandOffset.scale(multiplier.toDouble())
                    )
                )
            }

            result.add(
                CropStage(
                    blocks = blocks,
                    armorStands = newStands,
                    stageRange = stage..stage,
                    traits = traits
                )
            )
        }

        return result
    }

}
/** how skyblock turns its plants, a quarter turn per `(z - x) mod 4` of the base block */
object WorldRotation {

    fun step(x: Int, z: Int): Int = Math.floorMod(z - x, 4)

    fun rotate(offset: Vec3, steps: Int): Vec3 = when (Math.floorMod(steps, 4)) {
        1 -> Vec3(-offset.z, offset.y, offset.x)
        2 -> Vec3(-offset.x, offset.y, -offset.z)
        3 -> Vec3(offset.z, offset.y, -offset.x)
        else -> offset
    }
}

const val DEFAULT_DECAY_TIME_MS: Long = 3L * 24 * 60 * 60 * 1000

const val NEVER_DECAYS: Long = -1L

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

    /**
     * A pose walking a fixed cycle: poses[(x + z + height) mod size], as the jellybean's canes do.
     */
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

    val decayTimeMs: Long = DEFAULT_DECAY_TIME_MS,
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
    val rotatesWithPlot: Boolean = true
){
    val stagePlacedAt: Int get() = if (isMutation) maxStage else 1
    val elementId: String get() = skyblockId?.id ?: name
    val stages: List<CropStage> = stageDefs.flatMap { if (it is CropStagePattern) it.expand() else listOf(it) }

    override fun toString(): String {
        return name
    }
}

data class StageMatchResult(
    val matched: Boolean,
    val score: Int,
    val usedStands: List<Entity>,
    val matchedBlocks: Map<BlockPos, BlockState>,
    /** Matched, but only at rotation zero: a pre-normalization recording that wants re-exporting. */
    val rotationLegacy: Boolean = false
) {
    companion object {
        val NONE = StageMatchResult(false, 0, emptyList(), emptyMap())
    }
}


data class ElementRuntimeState(
    val instance: GreenhouseElementInstance,
    val standEntities: List<Entity>?,
    val blocksMap: Map<BlockPos,BlockState>?,

    val rotationLegacy: Boolean = false
)

data class GreenhouseElementInstance(
    val elementId: String,
    val slot: LayoutSlot,
    var waterLevel: Double? = null,
    var growthStage: GrowthStageInfo? = null,
    var age: Long? = null,
    val cropDef: CropDefinition,
    val readings: MutableMap<String, Int> = mutableMapOf(),
    val alternatives: MutableList<CropDefinition> = mutableListOf(),
) {
    val merged: Boolean get() = alternatives.isNotEmpty()

    fun defInSlot(def: CropDefinition): Boolean = def == cropDef || def in alternatives

    val everyCrop: List<CropDefinition> get() = listOf(cropDef) + alternatives

    val isAsleep: Boolean get() = readings[CropStandReader.ASLEEP] == 1

    val needsTime: Int? get() = readings[CropStandReader.NEEDS_TIME]

    val isStarving: Boolean get() = readings[CropStandReader.HUNGER] == 0

    /** if a tick has passed with negative water, then we don't know if it truly passed or not */
    var waterPredictedInDebt: Boolean = false

    var waterExact: Boolean = false

    var firstSeenStage: Int? = null

    var placed: Boolean = false

    var waterBestCase: Double? = null

    val fullyGrownByPlacing: Boolean get() = placed && cropDef.isMutation

    /**
     * A crop with nothing left to grow, worth harvesting: a mutation that grew here or a base crop at
     * its last stage. Judged by the highest stage it might be at, so a plant possibly grown is looked
     * at rather than left standing. A placed mutation is finished but was never grown, so never.
     */
    val readyToHarvest: Boolean
        get() = (cropDef.isMutation || cropDef.isBaseCrop) && !fullyGrownByPlacing && (highestStage ?: 0) >= cropDef.maxStage

    /** Whether this plant still takes water each tick: one of a crop that needs it, neither placed nor grown out. */
    val consumesWater: Boolean get() = cropDef.needsWater && !fullyGrownByPlacing && !isFullyGrown

    /** A copy on [slot], readings included, for a prediction that must not move the real plant. */
    fun copyForPrediction(slot: LayoutSlot): GreenhouseElementInstance =
        copy(slot = slot, readings = readings.toMutableMap(), alternatives = alternatives.toMutableList()).also {
            it.waterPredictedInDebt = waterPredictedInDebt
            it.waterExact = waterExact
            it.firstSeenStage = firstSeenStage
            it.placed = placed
            it.waterBestCase = waterBestCase
        }

    /**
     * Whether the water it holds now sees it to its last stage. Null when the stage is unknown or
     * the crop has only the one stage, so there is nothing to outlast and nothing to say.
     */
    fun outlastsGrowth(waterEffectPercent: Int): Boolean? {
        if (!consumesWater || cropDef.drainsNeighbours) return true

        val water = waterLevel ?: return null
        if (water <= WaterModel.DEATH) return false

        val ticksLeft = WaterModel.ticksUntilDeath(water, waterEffectPercent) ?: return true

        // in debt the water was charged for every tick while the low end of the stage took none,
        // and a skipped tick costs no water: only the stages the high end took are what the water
        // paid for, so that end is the one the water agrees with. Outside debt the lowest is the
        // stage with the most left to pay for
        val stage = (if (waterPredictedInDebt) highestStage else lowestStage) ?: return null
        if (cropDef.maxStage <= 1) return null

        return ticksLeft > cropDef.maxStage - stage
    }

    val isFullyGrown: Boolean get() = (lowestStage ?: 0) >= cropDef.maxStage

    /** The lowest stage this plant might be at now, which is all a scan can promise about most. */
    val lowestStage: Int?
        get() = when (val stage = growthStage) {
            is GrowthStageInfo.Known -> stage.stage
            is GrowthStageInfo.Estimated -> stage.range.first
            null -> null
        }

    /**
     * The highest stage this plant might be at, which is what anything about profit asks for: better
     * a wasted look than a grown mutation left standing.
     */
    val highestStage: Int?
        get() = when (val stage = growthStage) {
            is GrowthStageInfo.Known -> stage.stage
            is GrowthStageInfo.Estimated -> stage.range.last
            null -> null
        }

    /** Whether this plant grew where it stands rather than being placed there. */
    val grewInPlace: Boolean
        get() {
            if (placed) return false
            val first = firstSeenStage ?: return false
            val now = lowestStage ?: return false

            return now > first
        }

    /** Whether this plant craves a time of day other than [now], while it still has stages to grow. */
    fun cravesOtherTime(now: Int): Boolean {
        val wants = needsTime ?: return false
        val stage = lowestStage
        return wants != now && (stage == null || stage < cropDef.maxStage)
    }

    /** Time left before this plant rots. Null when it never rots, or its age was never measured. */
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