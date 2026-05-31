package org.magic.magicaddons.data.greenhouse

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.Common
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
    val hashMatches: ((String?) -> Boolean)? = null,
    val customNameMatches: ((String?) -> Boolean)? = null,
) {
    companion object {
        fun matcherPattern(
            offsets: List<Vec3>,
            hashMatches: ((String?) -> Boolean)? = null,
            customNameMatches: ((String?) -> Boolean)? = null
        ): List<CropArmorStand> {
            val result = mutableListOf<CropArmorStand>()
            offsets.forEach {
                result.add(
                    CropArmorStand(
                        it,
                        hashMatches,
                        customNameMatches
                    )
                )
            }
            return result
        }
    }
}
data class CropBlockState(
    val offset: BlockPos,
    val matcher: (BlockState) -> Boolean){

    companion object {
        fun matcherPattern(
            positions: List<BlockPos>,
            matcher: (BlockState) -> Boolean
        ): List<CropBlockState> {
            val result = mutableListOf<CropBlockState>()
            positions.forEach {
                result.add(
                    CropBlockState(
                        it,
                        matcher
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
    val extraInfo: CropExtraInfo? = null
) {
    // for now leaving debug in just in case
    fun matchesStage(
        origin: BlockPos,
        remainingStands: List<Entity>,
        debug: Boolean = false
    ): StageMatchResult {

        val level = Minecraft.getInstance().level ?: return StageMatchResult(
            false,
            0,
            emptyList(),
            emptyMap()
        )

        var score = 0
        val usedStands = mutableListOf<Entity>()
        val matchedBlocks = mutableMapOf<BlockPos, BlockState>()

        this.blocks?.forEach { blockDef ->
            val pos = origin.offset(blockDef.offset)
            val state = level.getBlockState(pos)

            if (!blockDef.matcher(state)) {
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

        this.armorStands?.forEach { standDef ->

            val center = Vec3(
                origin.x + 0.5,
                origin.y.toDouble(),
                origin.z + 0.5
            )

            val match = remainingStands.firstOrNull { entity ->
                if (entity !is ArmorStand) return@firstOrNull false

                val offset = entity.position().subtract(center)

                val head = entity.getItemBySlot(EquipmentSlot.HEAD)
                val hash = PlayerUtils.getSkinHash(head)
                val name = entity.customName?.string

                val offsetMatches =
                    matchesWithRotation(
                        offset,
                        standDef.offset,
                        this.allowRotation
                    )

                val hashMatches =
                    standDef.hashMatches?.invoke(hash) ?: true

                val nameMatches =
                    standDef.customNameMatches?.invoke(name) ?: true

                if (debug) {
                    Common.LOGGER.info(
                        """
                    Checking stand:
                    offset=$offset
                    expectedOffset=${standDef.offset}
                    hash=$hash
                    name=$name
                    offsetMatches=$offsetMatches
                    hashMatches=$hashMatches
                    nameMatches=$nameMatches
                    """.trimIndent()
                    )
                }

                offsetMatches && hashMatches && nameMatches
            }

            if (match == null) {
                if (debug) {
                    Common.LOGGER.info(
                        "Failed to find matching armor stand for expected offset ${standDef.offset}"
                    )
                }

                return StageMatchResult(false, 0, emptyList(), emptyMap())
            }

            if (debug) {
                Common.LOGGER.info(
                    "Matched armor stand at ${match.position()}"
                )
            }

            usedStands.add(match)
            score += 2
        }

        if (debug) {
            Common.LOGGER.info(
                "Stage matched successfully. Score=$score"
            )
        }

        return StageMatchResult(
            matched = true,
            score = score,
            usedStands = usedStands,
            matchedBlocks = matchedBlocks
        )
    }
    fun matchesWithRotation(
        actual: Vec3,
        expected: Vec3,
        allowRotation: Boolean
    ): Boolean {
        if (!allowRotation) {
            return isClose(actual, expected)
        }

        val rotations = listOf(
            expected,
            Vec3(-expected.z, expected.y, expected.x),
            Vec3(-expected.x, expected.y, -expected.z),
            Vec3(expected.z, expected.y, -expected.x)
        )

        return rotations.any { rotated ->
            isClose(actual, rotated)
        }
    }

    private fun isClose(a: Vec3, b: Vec3, epsilon: Double = 0.01): Boolean {
        return abs(a.x - b.x) < epsilon &&
                abs(a.y - b.y) < epsilon &&
                abs(a.z - b.z) < epsilon
    }
}


class CropStagePattern(
    blocks: List<CropBlockState>? = null,
    armorStands: List<CropArmorStand>? = null,
    stageRange: IntRange,
    allowRotation: Boolean = false,
    extraInfo: CropExtraInfo? = null,
    val baseStageStandOffset: Vec3,
    val stageOffsetMultipliers: Map<Int, Int> = emptyMap()
) : CropStage(
    blocks = blocks,
    armorStands = armorStands,
    stageRange = stageRange,
    allowRotation = allowRotation,
    extraInfo = extraInfo
){
    fun expand(): List<CropStage> {
        val result = mutableListOf<CropStage>()

        val start = stageRange.first

        for (stage in stageRange) {

            val multiplier = stageOffsetMultipliers[stage]
                ?: (stage - start) // good fallback

            val newStands = armorStands?.map { stand ->
                val offset = stand.offset.add(
                    baseStageStandOffset.scale(multiplier.toDouble())
                )

                CropArmorStand(
                    offset = offset,
                    hashMatches = stand.hashMatches
                )
            }

            result.add(
                CropStage(
                    blocks = blocks,
                    armorStands = newStands,
                    stageRange = stage..stage,
                    allowRotation = allowRotation
                )
            )
        }

        return result
    }

}
interface CropExtraInfo

data class CropDefinition(
    val name: String,
    val skyblockId: SkyBlockId?,
    val aliases: List<SkyBlockId>? = null,
    val stageDefs: List<CropStage>,
    val maxStage: Int = 1,
    val footprint: Footprint = Footprint(1,1),
    val requiredSoil: Set<Block> = setOf(Blocks.FARMLAND),
    val needsWater: Boolean = true,
    val isBaseCrop: Boolean = false,
    val isMutation: Boolean = false,
    val isRareCrop: Boolean = false //todo buffs later
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
    val matchedBlocks: Map<BlockPos, BlockState>
)


data class ElementRuntimeState(
    val cropDef: CropDefinition,
    val instance: GreenhouseElementInstance,
    val standEntities: List<Entity>?,
    val blocksMap: Map<BlockPos,BlockState>?, // todo add water level
)

interface CropDefinitionProvider {
    val definition: CropDefinition
}