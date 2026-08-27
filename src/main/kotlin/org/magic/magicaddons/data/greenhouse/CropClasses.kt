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
) {
    companion object {
        fun matcherPattern(
            offsets: List<Vec3>,
            rotations: List<Rotations>? = null,
            xRotations: List<Float>? = null,
            yRotations: List<Float>? = null,
            hashString: String? = null,
            customName: String? = null
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
                        customName
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
    val extraInfo: CropExtraInfo? = null
) {
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
            this.armorStands?.forEach { standDef ->



                val match = remainingStands
                    .map { entity ->

                        val offset = entity.position().subtract(center)
                        val head = entity.getItemBySlot(EquipmentSlot.HEAD)
                        val hash = PlayerUtils.getSkinHash(head)
                        val name = entity.customName?.string

                        val offsetOk = matchesWithRotation(offset, standDef.offset, allowRotation)
                        val hashOk = standDef.hashString?.let { it == hash } ?: true
                        val nameOk = standDef.containsCustomName?.let { name?.contains(it) ?: return@let false } ?: true

                        if (debug) {
                            Common.LOGGER.info(
                                """
                    [ArmorStandMatch Debug]
                    offset=$offset expected=${standDef.offset}
                    hash=$hash expectedHash=${standDef.hashString}${standDef.containsCustomName?.let { "\nname: $name needs to contain: ${standDef.containsCustomName}" } ?: ""} 
                    """.trimIndent()
                            )
                        }

                        entity to (offsetOk && hashOk && nameOk)
                    }
                    .filter { it.second }
                    .map { it.first }
                    .firstOrNull()

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
            Vec3(expected.z, expected.y, -expected.x),
            Vec3(expected.z, expected.y, expected.x),
            Vec3(-expected.z, expected.y, -expected.x)
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
                    hashString = stand.hashString
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
    val decayTimeMs: Long? = null,
    val footprint: Footprint = Footprint(1,1),
    val requiredSoil: Set<Block> = setOf(Blocks.FARMLAND),
    val needsWater: Boolean = true,
    val isBaseCrop: Boolean = false,
    val isMutation: Boolean = false,
    val isRareCrop: Boolean = false, //todo buffs later
    /** Shown in the ui when skyblock has no item of its own for this crop, a dead plant has none. */
    val displayItem: Item? = null
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
    val instance: GreenhouseElementInstance,
    val standEntities: List<Entity>?,
    val blocksMap: Map<BlockPos,BlockState>?, // todo add handling of water level
    //todo add here an extra info thing (maybe use the original one?)
)

data class GreenhouseElementInstance(
    val elementId: String, //just the skyblock id or name
    val slot: LayoutSlot,
    var waterLevel: Int? = null,
    var growthStage: GrowthStageInfo? = null,
    var age: Long? = null,
    val cropDef: CropDefinition,
)


interface CropDefinitionProvider {
    val definition: CropDefinition
}