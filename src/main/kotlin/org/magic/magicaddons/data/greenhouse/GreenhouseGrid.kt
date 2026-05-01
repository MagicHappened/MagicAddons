package org.magic.magicaddons.data.greenhouse

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData.getBuildableArea
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

    val elementInstances = mutableListOf<GreenhouseElementInstance>()

    val elements = mutableListOf<CropDefinition>() // runtime (derived)

    fun getPosForSlot(slot: GreenhouseSlot): BlockPos? {
        val box = plot?.getBuildableArea() ?: return null

        val minX = box.minX.toInt()
        val minZ = box.minZ.toInt()

        val worldX = minX + slot.x
        val worldZ = minZ + slot.y

        return BlockPos(worldX, 73, worldZ)
    }


    fun getSlotAt(blockPos: BlockPos): GreenhouseSlot? {
        if (blockPos.y != 73) return null

        val buildArea = plot?.getBuildableArea() ?: return null
        if (!buildArea.contains(Vec3.atCenterOf(blockPos))) return null

        val minX = buildArea.minX.toInt()
        val minZ = buildArea.minZ.toInt()

        val gridX = blockPos.x - minX
        val gridY = blockPos.z - minZ
        return getSlot(gridX, gridY)
    }

    fun getSlot(x: Int, y: Int): GreenhouseSlot? {
        return slots.getOrNull(y)?.getOrNull(x)
    }
    fun setSlot(slot: GreenhouseSlot) {
        slots.getOrNull(slot.y)
            ?.set(slot.x, slot)
    }

    fun rebuildElements() {
        elements.clear()

        for (instance in elementInstances) {
            val element = GreenhouseElementFactory.create(instance.elementId)
            elements.add(element.definition)
        }
    }

    companion object {
        val CODEC: Codec<GreenhouseGrid> = RecordCodecBuilder.create { instance ->
            instance.group(

                GreenhouseSlot.CODEC.listOf()
                    .fieldOf("slots")
                    .forGetter { grid ->
                        grid.slots.flatten()
                    },

                GreenhouseElementInstance.CODEC.listOf()
                    .fieldOf("elements")
                    .forGetter { it.elementInstances }

            ).apply(instance) { slots, elements ->

                val grid = GreenhouseGrid()
                slots.forEach {
                    val slot = GreenhouseSlot(it.x,it.y,it.unlocked,it.placedBlock)
                    grid.setSlot(slot)
                }
                grid.elementInstances.addAll(elements)

                grid
            }
        }
    }

}