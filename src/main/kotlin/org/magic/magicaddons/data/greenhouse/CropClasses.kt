package org.magic.magicaddons.data.greenhouse

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
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.Common
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import kotlin.math.abs

sealed interface GrowthStageInfo {

    data class Known(val stage: Int) : GrowthStageInfo

    data class Estimated(val range: IntRange) : GrowthStageInfo

}



data class Footprint(val width: Int, val height: Int)

data class CropArmorStand(
    val offset: Vec3, //offset is defined from the soil top left block
    val headRotation: Rotations? = null,
    val xRotation: Float? = null,
    val yRotation: Float? = null,
    val hashString: String? = null,
    val containsCustomName: String? = null,
    /**
     * How the stand is built, which decides where its head lands. A stand's position is its feet,
     * so rebuilding a small one at full size puts the skull it carries well above where it belongs.
     * Nearly every greenhouse stand is small, so that is the default and the odd one out says so.
     */
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
            isSmall: Boolean = true
        ): List<CropArmorStand> {
            val result = mutableListOf<CropArmorStand>()
            offsets.forEachIndexed { i, offset ->
                result.add(
                    CropArmorStand(
                        offset,
                        rotations?.getOrNull(i),
                        xRotations?.getOrNull(i),
                        yRotations?.getOrNull(i),
                        hashString,
                        customName,
                        isSmall
                    )
                )
            }
            return result
        }
    }
}
data class CropBlockState(
    val offset: BlockPos,
    val blockState: BlockState
){

    companion object {
        fun blockStatePattern(
            positions: List<BlockPos>,
            blockState: BlockState
        ): List<CropBlockState> {
            val result = mutableListOf<CropBlockState>()
            positions.forEach {
                result.add(
                    CropBlockState(
                        it,
                        blockState
                    )
                )
            }
            return result
        }
    }
}



open class CropStage(
    val blocks: List<CropBlockState>? = null,
    val armorStands: List<CropArmorStand>? = null,  // make sure on the matcher if its NULL it shouldnt have the respective thing on it!
    val stageRange: IntRange, // eg if its a wheat crop it CANNOT have any armor stands on it otherwise it will be considered something
    val allowRotation: Boolean = false, // else at runtime (eg ashwreath having partially grown wheat block)
    /**
     * Facts this stage's identity implies, copied into the instance's readings when it wins.
     *
     * A reader takes values off the stands after matching; a trait is known by the matching
     * itself, such as which time of day the noctilume whose skull matched is craving. Both land
     * in the same readings map, because both answer the same question: what is this plant beyond
     * its stage number.
     */
    val traits: Map<String, Int> = emptyMap(),
    /**
     * Values to take off the stands around the plant once it has matched. These never decide
     * whether it matched: a bar that happens to be empty is a plant that is starving, not a plant
     * that is something else.
     */
    val readers: List<CropStandReader> = emptyList()
) {

    /**
     * Runs every reader against [stands], for a plant that has already matched.
     *
     * A reader takes the first stand it recognises and is dropped if it recognises none, so a bar
     * that has not appeared yet leaves no reading behind rather than a wrong one.
     */
    fun read(stands: List<ArmorStand>): Map<String, Int> = readers.mapNotNull { reader ->
        stands.firstOrNull { reader.matches(it) }
            ?.let { reader.read(it) }
            ?.let { reader.key to it }
    }.toMap()

    // for now leaving debug in just in case
    fun matchesStage(
        origin: BlockPos,
        remainingStands: List<ArmorStand>,
        footprint: Footprint,
        debug: Boolean = false
    ): StageMatchResult {
        val level = Minecraft.getInstance().level ?: return StageMatchResult(
            false,
            0,
            emptyList(),
            emptyMap()
        )

        var score = 0
        var matchedFirstCandidate = true
        val usedStands = mutableListOf<Entity>()
        val matchedBlocks = mutableMapOf<BlockPos, BlockState>()
        if (debug){
            Common.LOGGER.info("Trying to match stage: ${stageRange.first}, ${stageRange.last}")
        }
        try {
            this.blocks?.forEach { blockDef ->
                val pos = origin.offset(blockDef.offset)
                val state = level.getBlockState(pos)

                if (state != blockDef.blockState) {
                    if (debug) {
                        Common.LOGGER.info(
                            "Block mismatch at $pos. State=${state.block}"
                        )
                    }

                    return StageMatchResult(false, 0, emptyList(), emptyMap())
                }

                if (debug) {
                    Common.LOGGER.info("Matched block at $pos")
                }

                matchedBlocks[pos] = state
                score += 1
            }
            val center = Vec3(
                origin.x + footprint.width / 2.0,
                origin.y.toDouble(),
                origin.z + footprint.height / 2.0
            )
            // one rotation for the whole stage, and the world's own comes first. Exports are
            // written as the plant would stand at rotation zero, so canonical data matches on the
            // first candidate, the rotation the grid gives this block. A stage that only matches
            // further down the list was recorded before normalization and never rewritten, which
            // the result reports so the caller can ask for a fresh export. The reflections stay
            // last for the same reason: nothing canonical ever needs them
            val worldStep = WorldRotation.step(origin.x, origin.z)

            val candidateSteps = when {
                this.armorStands.isNullOrEmpty() -> listOf(0)
                allowRotation -> (listOf(worldStep) + listOf(0, 1, 2, 3, 4, 5)).distinct()
                else -> listOf(worldStep, 0).distinct()
            }

            var matchedStands: List<Entity>? = null

            for (step in candidateSteps) {
                val used = mutableListOf<Entity>()
                var allFound = true

                for (standDef in this.armorStands.orEmpty()) {
                    val expected = WorldRotation.rotate(standDef.offset, step)

                    val match = remainingStands.firstOrNull { entity ->
                        val offset = entity.position().subtract(center)
                        val hash = PlayerUtils.getSkullHash(entity)
                        val name = entity.customName?.string

                        isClose(offset, expected) &&
                                (standDef.hashString?.let { it == hash } ?: true) &&
                                (standDef.containsCustomName?.let { name?.contains(it) == true } ?: true)
                    }

                    if (match == null) {
                        if (debug) {
                            Common.LOGGER.info(
                                "step=$step: no stand at ${standDef.offset} (rotated $expected)"
                            )
                        }
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

            if (matchedStands == null) {
                return StageMatchResult(false, 0, emptyList(), emptyMap())
            }

            matchedStands.forEach { match ->
                if (debug) {
                    Common.LOGGER.info("Matched armor stand at ${match.position()}")
                }
                usedStands.add(match)
                score += 2
            }

            if (debug) {
                Common.LOGGER.info(
                    "Stage matched successfully. Score=$score"
                )
            }
        } catch (e: NoSuchMethodException) {
            ChatUtils.sendWithPrefix("Caught NoSuchMethodException in matchesStage. for $e")
            return StageMatchResult(
                matched = false,
                score = 0,
                usedStands = emptyList(),
                matchedBlocks = emptyMap()
            )
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

    fun toRenderData(level: Level, baseBlock: BlockPos, footprint: Footprint): RenderData{
        val renderStands = mutableListOf<ArmorStand>()
        val blockMap = mutableMapOf<BlockPos, BlockState>()
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
            standDef.hashString ?: return@forEach
            val stand = ArmorStand(
                level,
                center.x + standDef.offset.x,
                center.y + standDef.offset.y,
                center.z + standDef.offset.z
            )

            // the flags ride in synched data rather than in setters, which are not ours to call
            if (standDef.isSmall) {
                stand.entityData.set(
                    ArmorStand.DATA_CLIENT_FLAGS,
                    ArmorStand.CLIENT_FLAG_SMALL.toByte()
                )
            }

            // a stand built to be drawn and never put in the world has no id of its own, and
            // rendering one holding an item asks for that id, which throws rather than returning
            // nothing. Any id will do, so long as it is not one the world handed out.
            stand.id = FAKE_ENTITY_ID

            stand.isInvisible = true
            standDef.headRotation?.let { stand.headPose = it }
            val stack = PlayerUtils.getItemFromHash(standDef.hashString)
            stand.setItemSlot(EquipmentSlot.HEAD, stack)
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
        /** Below every id the world assigns, so a stand of ours is never taken for a real one. */
        private const val FAKE_ENTITY_ID: Int = -1
    }


}


class CropStagePattern(
    blocks: List<CropBlockState>? = null,
    armorStands: List<CropArmorStand>? = null,
    stageRange: IntRange,
    allowRotation: Boolean = false,
    traits: Map<String, Int> = emptyMap(),
    val baseStageStandOffset: Vec3,
    val stageOffsetMultipliers: Map<Int, Int> = emptyMap()
) : CropStage(
    blocks = blocks,
    armorStands = armorStands,
    stageRange = stageRange,
    allowRotation = allowRotation,
    traits = traits
){
    fun expand(): List<CropStage> {
        val result = mutableListOf<CropStage>()

        val start = stageRange.first

        for (stage in stageRange) {

            val multiplier = stageOffsetMultipliers[stage]
                ?: (stage - start) // good fallback

            // the stand as described, moved along by the stage, rather than a new one built from
            // two of its fields. Rebuilding dropped everything else it was given: its rotations,
            // the name it looks for, and how it is built, so an expanded stage matched loosely and
            // was drawn as a full sized stand however the crop actually stands
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
                    allowRotation = allowRotation,
                    traits = traits
                )
            )
        }

        return result
    }

}
/**
 * How the greenhouse turns its plants.
 *
 * Every plant is rotated a quarter turn per diagonal of the grid it stands on: measured across four
 * greenhouses and three hundred stands, a plant's rotation is always ninety degrees times
 * `(z - x) mod 4` of its base block, and for a plant wider than one block any block on its main
 * diagonal gives the same answer, so the base corner serves for every footprint.
 */
object WorldRotation {

    /** The quarter turns the world gives a plant whose base block is at ([x], [z]). */
    fun step(x: Int, z: Int): Int = Math.floorMod(z - x, 4)

    /**
     * [offset] turned by [steps] quarter turns about the plant's centre.
     *
     * Steps four and five are not rotations but the two reflections the matcher has always also
     * tried; they are kept for stages recorded before the rotation rule was known, whose canonical
     * step nobody wrote down.
     */
    fun rotate(offset: Vec3, steps: Int): Vec3 = when (Math.floorMod(steps, 6)) {
        1 -> Vec3(-offset.z, offset.y, offset.x)
        2 -> Vec3(-offset.x, offset.y, -offset.z)
        3 -> Vec3(offset.z, offset.y, -offset.x)
        4 -> Vec3(offset.z, offset.y, offset.x)
        5 -> Vec3(-offset.z, offset.y, -offset.x)
        else -> offset
    }
}

/** Everything in a greenhouse that decays does so three days after it was planted. */
const val DECAY_TIME_MS: Long = 3L * 24 * 60 * 60 * 1000

/** [CropDefinition.decayTimeMs] for a plant that never decays, such as the fleshtrap. */
const val NEVER_DECAYS: Long = -1L

data class CropDefinition(
    val name: String,
    val skyblockId: SkyBlockId?,
    val aliases: List<SkyBlockId>? = null,
    val stageDefs: List<CropStage>,
    val maxStage: Int = 1,
    val decayTimeMs: Long? = null,
    val footprint: Footprint = Footprint(1,1),
    val requiredSoil: Set<Block> = setOf(Blocks.FARMLAND),
    val needsWater: Boolean = true,
    val isBaseCrop: Boolean = false,
    val isMutation: Boolean = false,
    val isRareCrop: Boolean = false,
    /** Shown in the ui when skyblock has no item of its own for this crop, a dead plant has none. */
    val displayItem: Item? = null,
    /** The buffs and debuffs this crop carries, which are what make a layout worth planning. */
    val effects: Set<CropEffect> = emptySet()
){
    fun matchesId(id: SkyBlockId): Boolean{
        return skyblockId == id || (aliases?.any { it == id } ?: false)
    }

    override fun toString(): String {
        return name
    }
}

data class StageMatchResult(
    val matched: Boolean,
    val score: Int,
    val usedStands: List<Entity>,
    val matchedBlocks: Map<BlockPos, BlockState>,
    /**
     * The stage matched, but not at the rotation the world gives its block: its offsets were
     * recorded before exports were normalized. Everything about the plant is still right, only
     * the recording is turned, so this asks for a fresh export rather than doubting the match.
     */
    val rotationLegacy: Boolean = false
)


data class ElementRuntimeState(
    val instance: GreenhouseElementInstance,
    val standEntities: List<Entity>?,
    val blocksMap: Map<BlockPos,BlockState>?, // todo add handling of water level
    //todo add here an extra info thing (maybe use the original one?),
    /** See [StageMatchResult.rotationLegacy]: matched, but from a pre-normalization recording. */
    val rotationLegacy: Boolean = false
)

data class GreenhouseElementInstance(
    val elementId: String, //just the skyblock id or name
    val slot: LayoutSlot,
    var waterLevel: Int? = null,
    var growthStage: GrowthStageInfo? = null,
    var age: Long? = null,
    val cropDef: CropDefinition,
    /**
     * What the scan learned about this plant, by name: what the stands around it said through
     * readers, and what the matched stage's traits implied. A fleshtrap's hunger, a snoozling's
     * sleep and a noctilume's craving all live here, since they are things a plant is rather than
     * things that decide what it is.
     */
    val readings: MutableMap<String, Int> = mutableMapOf(),
) {
    /** Whether this plant is asleep and will not grow until it is woken. */
    val isAsleep: Boolean get() = readings[CropStandReader.ASLEEP] == 1

    /**
     * The time of day this plant craves, or null for one that craves nothing. A noctilume only
     * advances while the garden's clock matches its craving, and flips it on every advance.
     */
    val craving: Int? get() = readings[CropStandReader.CRAVES]
}


interface CropDefinitionProvider {
    val definition: CropDefinition
}