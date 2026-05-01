package org.magic.magicaddons.data.greenhouse


import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import java.util.Optional

sealed interface GrowthStageInfo {

    data class Known(val stage: Int) : GrowthStageInfo

    data class Estimated(val range: IntRange) : GrowthStageInfo

    companion object {
        val CODEC: Codec<GrowthStageInfo> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("type").forGetter {
                    when (it) {
                        is Known -> "known"
                        is Estimated -> "estimated"
                    }
                },

                Codec.INT.optionalFieldOf("stage").forGetter {
                    Optional.ofNullable((it as? Known)?.stage)
                },

                Codec.INT.fieldOf("min").forGetter {
                    (it as? Estimated)?.range?.first ?: 0
                },

                Codec.INT.fieldOf("max").forGetter {
                    (it as? Estimated)?.range?.last ?: 0
                }
            ).apply(instance) { type, stageOpt, min, max ->
                when (type) {
                    "known" -> Known(stageOpt.orElse(0))
                    else -> Estimated(min..max)
                }
            }
        }
    }

}



data class Footprint(val width: Int, val height: Int)

data class CropArmorStand(val offset: Vec3, val matcher: (String?) -> Boolean) //offset is defined from the soil top left block
data class CropBlockState(val offset: BlockPos, val matcher: (BlockState) -> Boolean)

open class CropStage(
    val blocks: List<CropBlockState>? = null,
    val armorStands: List<CropArmorStand>? = null,  // make sure on the matcher if its NULL it shouldnt have the respective thing on it!
    val stageRange: IntRange, // eg if its a wheat crop it CANNOT have any armor stands on it otherwise it will be considered something
    val allowRotation: Boolean = false // else at runtime (eg ashwreath having partially grown wheat block)
)

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
    allowRotation = true
){
    fun expand(): List<CropStage> {
        val result = mutableListOf<CropStage>()

        val start = stageRange.first

        var accumulated = 0

        for (stage in stageRange) {

            if (stage == start) {
                accumulated = 0
            } else {
                accumulated += stageOffsetMultipliers[stage] ?: 1
            }

            val newStands = armorStands?.map { stand ->

                val offset = stand.offset.add(
                    baseStageStandOffset.scale(accumulated.toDouble())
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
    val skyblockId: SkyBlockId,
    val stageDefs: List<CropStage>,
    val footprint: Footprint = Footprint(1,1),
    val requiredSoil: Set<Block> = setOf(Blocks.FARMLAND),
    val needsWater: Boolean = true,
    val isBaseCrop: Boolean = false,
    val isMutation: Boolean = false,
    val isRareCrop: Boolean = false //todo buffs later
)

interface CropDefinitionProvider {
    val definition: CropDefinition
}