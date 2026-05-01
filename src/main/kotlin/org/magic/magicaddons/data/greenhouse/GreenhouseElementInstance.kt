package org.magic.magicaddons.data.greenhouse

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.builtins.serializer
import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter
import java.util.Optional

data class GreenhouseElementInstance(
    val elementId: String, //just the skyblock id
    val slot: GreenhouseSlot? = null,
    var waterLevel: Int? = null,
    var growthStage: GrowthStageInfo? = null){

    fun createElement(): CropDefinitionProvider {
        return GreenhouseElementFactory.create(elementId)
    }

    companion object {
        val CODEC: Codec<GreenhouseElementInstance> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("id").forGetter { it.elementId },
                GreenhouseSlot.CODEC.optionalFieldOf("slot")
                    .forGetter { Optional.ofNullable(it.slot)},

                Codec.INT.optionalFieldOf("waterLevel")
                    .forGetter { Optional.ofNullable(it.waterLevel) },

                GrowthStageInfo.CODEC.optionalFieldOf("growthStage")
                    .forGetter { Optional.ofNullable(it.growthStage) }
            ).apply(instance) { id, slot, waterOpt, growthOpt ->
                GreenhouseElementInstance(
                    elementId = id,
                    slot = slot.orElse(null),
                    waterLevel = waterOpt.orElse(null),
                    growthStage = growthOpt.orElse(null)
                )
            }
        }
    }

}