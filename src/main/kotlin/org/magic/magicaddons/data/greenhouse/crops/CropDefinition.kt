package org.magic.magicaddons.data.greenhouse.crops

import kotlin.math.roundToInt
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId

data class Footprint(val width: Int, val height: Int) {
    fun spaceAbove(soil: BlockPos, height: Int): AABB = AABB(
        soil.x.toDouble(), soil.y.toDouble(), soil.z.toDouble(),
        (soil.x + width).toDouble(),
        (soil.y + height).toDouble(),
        (soil.z + this.height).toDouble()
    )
}

data class CropDefinition(
    val name: String,
    val tier: CropTier,
    val skyblockId: SkyBlockId?,
    val aliases: List<SkyBlockId>? = null,
    val stages: List<CropStage>,
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
    /** what the player has to do for a plant stopped at one of [sleepStages] to grow on */
    val stallExplanation: String? = null,
    val rotatesWithPlot: Boolean = true,
    val spawnRule: SpawnRule? = null,
    val chargeRule: ChargeRule? = null,
    val dropMultiplier: Double? = null,
    /** the stems' age changes with something other than the stage */
    val stemAgeVaries: Boolean = false,
    val resetsToFirstStage: Boolean = false
){
    val stagePlacedAt: Int get() = if (isMutation) maxStage else 1
    val elementId: String get() = skyblockId?.id ?: name

    val hasHungerBar: Boolean get() = stages.any { stage -> stage.readers.any { it.key == StandReader.HUNGER } }

    override fun toString(): String {
        return name
    }
}

enum class CropTier(val listingName: String, val heading: String) {
    BaseCrop("base crops", "Base Crops"),
    Common("common mutations", "Common"),
    Uncommon("uncommon mutations", "Uncommon"),
    Rare("rare mutations", "Rare"),
    Epic("epic mutations", "Epic"),
    Legendary("legendary mutations", "Legendary"),
    RareCrop("rare crops", "Rare Crops"),
    Other("other", "Misc")
}

data class SpawnRule(
    val weight: Int,
    val requiredNeighbourCells: Map<String, Int> = emptyMap(),
    val needsNoNeighbours: Boolean = false,
    val needsAllPositiveEffects: Boolean = false
)

data class ChargeRule(
    val perStage: Int,
    val limit: Int
) {
    fun stagesUntilOverload(charge: Int): Int = (limit - charge) / perStage

    /** the most a plant at [stage] can hold if it has never been discharged: nothing at stage 1 */
    fun chargeImpliedBy(stage: Int): Int = perStage * (stage - 1).coerceAtLeast(0)

    /** the charge a bar filled to [percent] stands for, which can only ever be whole stages */
    fun chargeShownBy(percent: Int): Int = (limit * percent / 100.0 / perStage).roundToInt() * perStage
}

const val NEVER_DECAYS: Long = -1L

const val THREE_DAY_DECAY_TIME_MS: Long = 3L * 24 * 60 * 60 * 1000
const val FIVE_DAY_DECAY_TIME_MS: Long = 5L * 24 * 60 * 60 * 1000
const val SIX_DAY_DECAY_TIME_MS: Long = 6L * 24 * 60 * 60 * 1000
const val TEN_DAY_DECAY_TIME_MS: Long = 10L * 24 * 60 * 60 * 1000
