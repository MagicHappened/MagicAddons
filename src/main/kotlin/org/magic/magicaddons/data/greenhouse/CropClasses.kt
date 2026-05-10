package org.magic.magicaddons.data.greenhouse

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData.matchesWithRotation
import org.magic.magicaddons.util.PlayerUtils
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import java.util.Optional

sealed interface GrowthStageInfo {

    data class Known(val stage: Int) : GrowthStageInfo

    data class Estimated(val range: IntRange) : GrowthStageInfo

}



data class Footprint(val width: Int, val height: Int)

data class CropArmorStand(val offset: Vec3, val matcher: (String?) -> Boolean) //offset is defined from the soil top left block
data class CropBlockState(val offset: BlockPos, val matcher: (BlockState) -> Boolean)

open class CropStage(
    val blocks: List<CropBlockState>? = null,
    val armorStands: List<CropArmorStand>? = null,  // make sure on the matcher if its NULL it shouldnt have the respective thing on it!
    val stageRange: IntRange, // eg if its a wheat crop it CANNOT have any armor stands on it otherwise it will be considered something
    val allowRotation: Boolean = false // else at runtime (eg ashwreath having partially grown wheat block)
) {
    fun matchesStage(
        origin: BlockPos,
        remainingStands: List<Entity>
    ): StageMatchResult {

        val level = Minecraft.getInstance().level ?: return StageMatchResult(
            false,
            0,
            emptyList(),
            emptyMap()
        )

        var score = 0
        val usedStands = mutableListOf<Entity>()
        val matchedBlocks = mutableMapOf<BlockPos,BlockState>()

        this.blocks?.forEach { blockDef ->
            val pos = origin.offset(blockDef.offset)
            val state = level.getBlockState(pos)

            if (!blockDef.matcher(state)) {
                return StageMatchResult(false, 0, emptyList(), emptyMap())
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

                matchesWithRotation(offset,standDef.offset, this.allowRotation) &&
                        standDef.matcher(hash)
            } ?: return StageMatchResult(false, 0, emptyList(), emptyMap())

            usedStands.add(match)
            score += 2
        }

        return StageMatchResult(
            matched = true,
            score = score,
            usedStands = usedStands,
            matchedBlocks = matchedBlocks
        )
    }





}

class CropStagePattern(
    blocks: List<CropBlockState>? = null,
    armorStands: List<CropArmorStand>? = null,
    stageRange: IntRange,
    allowRotation: Boolean = false,
    val baseStageStandOffset: Vec3,
    val stageOffsetMultipliers: Map<Int, Int> = emptyMap()
) : CropStage(
    blocks = blocks,
    armorStands = armorStands,
    stageRange = stageRange,
    allowRotation = allowRotation
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
                    matcher = stand.matcher
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


data class CropDefinition(
    val name: String,
    val skyblockId: SkyBlockId?,
    val aliases: List<SkyBlockId>? = null,
    val stageDefs: List<CropStage>,
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