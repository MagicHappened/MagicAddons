package org.magic.magicaddons.data.greenhouse

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.level.block.state.BlockState
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class GreenhouseSlot(
    val x: Int,
    val y: Int,
    var unlocked: Boolean = false,
    var placedBlock: BlockState?,
) {
    //facing north top left slot is x=0 y=0
    // bottom left therefore is x=9 y=9


    companion object {
        val CODEC: Codec<GreenhouseSlot> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("x").forGetter { it.x },
                Codec.INT.fieldOf("y").forGetter { it.y },
                Codec.BOOL.optionalFieldOf("unlocked", false).forGetter { it.unlocked },

                BlockState.CODEC
                    .optionalFieldOf("block")
                    .forGetter { Optional.ofNullable(it.placedBlock) },
            ).apply(instance) { x, y, unlocked, block ->
                GreenhouseSlot(x, y, unlocked, block.getOrNull())
            }
        }
    }

}
