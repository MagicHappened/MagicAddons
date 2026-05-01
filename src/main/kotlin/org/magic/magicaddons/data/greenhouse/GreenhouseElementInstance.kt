package org.magic.magicaddons.data.greenhouse

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.util.Optional

data class GreenhouseElementInstance(
    val elementId: String, //just the skyblock id
    val originX: Int,
    val originY: Int,
    var waterLevel: Int? = null,
    var growthStage: GrowthStageInfo? = null){

    fun createElement(): CropDefinitionProvider {
        return GreenhouseElementFactory.create(elementId)
    }

    companion object {
        val CODEC: Codec<GreenhouseElementInstance> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter { it.elementId },
                Codec.INT.fieldOf("originX").forGetter { it.originX },
                Codec.INT.fieldOf("originY").forGetter { it.originY },

                Codec.INT.optionalFieldOf("waterLevel")
                    .forGetter { Optional.ofNullable(it.waterLevel) },

                GrowthStageInfo.CODEC.optionalFieldOf("growthStage")
                    .forGetter { Optional.ofNullable(it.growthStage) }
            ).apply(instance) { id, x, y, waterOpt, growthOpt ->
                GreenhouseElementInstance(
                    elementId = id,
                    originX = x,
                    originY = y,
                    waterLevel = waterOpt.orElse(null),
                    growthStage = growthOpt.orElse(null)
                )
            }
        }
    }

}