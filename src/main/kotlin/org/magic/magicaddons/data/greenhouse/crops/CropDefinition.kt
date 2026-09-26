package org.magic.magicaddons.data.greenhouse.crops

import kotlin.math.roundToInt
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
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

    fun cellsFrom(x: Int, y: Int): List<Pair<Int, Int>> =
        (0 until width).flatMap { offsetX -> (0 until height).map { offsetY -> (x + offsetX) to (y + offsetY) } }
}

data class CropDefinition(
    val name: String,

    val skyblockId: SkyBlockId?,
    val aliases: List<SkyBlockId>? = null,
    val stages: List<CropStage>,
    val maxStage: Int = 1,
    val isBaseCrop: Boolean = false,
    val isMutation: Boolean = false,
    val decayTimeMs: Long = THREE_DAY_DECAY_TIME_MS,
    val footprint: Footprint = Footprint(1,1),
    val requiredSoil: Set<Block> = setOf(Blocks.FARMLAND),
    val needsWater: Boolean = true,
    val effects: Set<CropEffect> = emptySet(),
    val spawnRule: SpawnRule? = null,
    val dropMultiplier: Double? = null,

    val tier: CropTier,
    val displayItem: Item? = null,
    val standPoses: Map<String, StandPose> = emptyMap(),
    val rotatesWithPlot: Boolean = true,

    val drainsNeighbours: Boolean = false,
    val resetsToFirstStage: Boolean = false,
    val sleepStages: Set<Int> = emptySet(),
    val stallExplanation: String? = null,
    val chargeRule: ChargeRule? = null,
    val stemAgeVaries: Boolean = false
){
    fun headPoseFor(stand: StageStand, x: Int, z: Int): Rotations? =
        stand.headRotation ?: standPoses[stand.hashString]?.headAt(x, z, stand.offset)

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

enum class CropEffect(val kind: EffectKind, val percent: Int, val label: String) {

    HarvestBoost(EffectKind.Yield, 20, "Harvest Boost"),
    ImprovedHarvestBoost(EffectKind.Yield, 30, "Improved Harvest Boost"),
    HarvestLoss(EffectKind.Yield, -20, "Harvest Loss"),

    XpBoost(EffectKind.Xp, 20, "XP Boost"),
    ImprovedXpBoost(EffectKind.Xp, 30, "Improved XP Boost"),
    XpLoss(EffectKind.Xp, -20, "XP Loss"),

    WaterRetain(EffectKind.Water, 50, "Water Retain"),
    ImprovedWaterRetain(EffectKind.Water, 100, "Improved Water Retain"),
    WaterDrain(EffectKind.Water, -30, "Water Drain"),

    BonusDrops(EffectKind.Drops, 0, "Bonus Drops"),

    Immunity(EffectKind.Immunity, 0, "Immunity"),

    EffectSpread(EffectKind.Spread, 0, "Effect Spread");

    enum class EffectKind(val positiveLabel: String) {
        Yield("Harvest Boost"),
        Xp("XP Boost"),
        Water("Water Retain"),
        Drops("Bonus Drops"),
        Immunity("Immunity"),
        Spread("Effect Spread")
    }


    companion object {
        fun appliedEffect(effects: Iterable<CropEffect>, kind: EffectKind): Int {
            val percents = effects.filter { it.kind == kind }.map { it.percent }
            val maxEffect = percents.maxOrNull() ?: 0
            if (maxEffect <= 0) return maxEffect
            return maxEffect + (percents.minOrNull()?.coerceAtMost(0) ?: 0)
        }

    }
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

    fun chargeByStageNum(stage: Int): Int = perStage * (stage - 1).coerceAtLeast(0)

    fun clampToNearest2k(percent: Int): Int = (limit * percent / 100.0 / perStage).roundToInt() * perStage
}

const val NEVER_DECAYS: Long = -1L

const val THREE_DAY_DECAY_TIME_MS: Long = 3L * 24 * 60 * 60 * 1000
const val FIVE_DAY_DECAY_TIME_MS: Long = 5L * 24 * 60 * 60 * 1000
const val SIX_DAY_DECAY_TIME_MS: Long = 6L * 24 * 60 * 60 * 1000
const val TEN_DAY_DECAY_TIME_MS: Long = 10L * 24 * 60 * 60 * 1000
