package org.magic.magicaddons.data.greenhouse

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.builtins.serializer
import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter
import java.util.Optional

data class GreenhouseElementInstance(
    val elementId: String, //just the skyblock id
    val slot: GreenhouseSlot,
    var waterLevel: Int? = null,
    var growthStage: GrowthStageInfo? = null){




}