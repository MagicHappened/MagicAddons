package org.magic.magicaddons.data.greenhouse

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot

class GreenhouseGrid {

    var plot: Plot? = null
    val width = 10
    val height = 10

    private val slots = Array(height) { y ->
        Array(width) { x ->
            GreenhouseSlot(x, y, false, null)
        }
    }

    val plants = mutableListOf<PlantInstance>()


    fun getSlot(x: Int, y: Int): GreenhouseSlot? {
        return slots.getOrNull(y)?.getOrNull(x)
    }
    fun setSlot(slot: GreenhouseSlot) {
        slots.getOrNull(slot.y)
            ?.set(slot.x, slot)
    }




    companion object {
        val CODEC: Codec<GreenhouseGrid> = RecordCodecBuilder.create { instance ->
            instance.group(

                GreenhouseSlot.CODEC.listOf()
                    .fieldOf("slots")
                    .forGetter { grid ->
                        grid.slots.flatten()
                    },

                PlantInstance.CODEC.listOf()
                    .fieldOf("plants")
                    .forGetter { it.plants }

            ).apply(instance) { slots, plants ->

                val grid = GreenhouseGrid()
                slots.forEach {
                    val slot = GreenhouseSlot(it.x,it.y,it.unlocked,it.placedBlock)
                    grid.setSlot(slot)
                }
                grid.plants.addAll(plants)

                grid
            }
        }
    }

}