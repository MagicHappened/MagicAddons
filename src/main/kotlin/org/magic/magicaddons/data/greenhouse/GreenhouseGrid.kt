package org.magic.magicaddons.data.greenhouse

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData.getBuildableArea
import tech.thatgravyboat.skyblockapi.api.profile.garden.Plot

class GreenhouseGrid {

    var plot: Plot? = null
    var state: GridState? = null
    val width = 10
    val height = 10

    private val slots = Array(height) { y ->
        Array(width) { x ->
            GreenhouseSlot(x, y, false, null)
        }
    }

    val elementInstances = mutableListOf<GreenhouseElementInstance>()

    val elements = mutableListOf<ElementRuntimeState>()

    fun getPosForSlot(slot: GreenhouseSlot): BlockPos? {
        val box = plot?.getBuildableArea() ?: return null

        val minX = box.minX.toInt()
        val minZ = box.minZ.toInt()

        val worldX = minX + slot.x
        val worldZ = minZ + slot.y

        return BlockPos(worldX, 73, worldZ)
    }


    fun getSlotAt(blockPos: BlockPos, matchY: Boolean = true): GreenhouseSlot? {
        val buildArea = plot?.getBuildableArea() ?: return null

        if (!buildArea.contains(Vec3.atCenterOf(blockPos))) return null

        // Only enforce Y when requested
        if (matchY && blockPos.y != 73) return null

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

    fun getUnassignedBlockMap(): Map<BlockPos, BlockState> {
        val level = Minecraft.getInstance().level ?: return emptyMap()
        val area = plot?.getBuildableArea() ?: return emptyMap()

        val allBlocks = mutableMapOf<BlockPos, BlockState>()

        val minX = area.minX.toInt()
        val minZ = area.minZ.toInt()
        val maxX = area.maxX.toInt()
        val maxZ = area.maxZ.toInt()

        val minY = 74
        val maxY = 84

        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                for (y in minY..maxY) {
                    val pos = BlockPos(x, y, z)
                    val state = level.getBlockState(pos)

                    if (!state.isAir) {
                        allBlocks[pos] = state
                    }
                }
            }
        }

        val usedPositions = elements
            .flatMap { it.blocksMap?.keys ?: emptySet() }
            .toSet()

        return allBlocks.filterKeys { it !in usedPositions }
    }

    fun getUnassignedArmorStands(): List<ArmorStand>? {
        val level = Minecraft.getInstance().level ?: return null
        val area = plot?.getBuildableArea() ?: return null
        val stands = level.getEntities(null, area)
            .filterIsInstance<ArmorStand>()
            .toMutableList()
        elements.forEach {
            it.standEntities?.let { elements -> stands.removeAll(elements) }
        }
        return stands.toList()
    }

    data class GridState(
        var lastUpdateTimestamp: Long,
        var needsUpdate: Boolean = false,


    ){

        companion object {
            val CODEC: Codec<GridState> = RecordCodecBuilder.create { instance ->
                instance.group(
                    Codec.LONG.fieldOf("lastUpdateTimestamp").forGetter {
                        it.lastUpdateTimestamp
                    },
                        Codec.BOOL.fieldOf("needsUpdate").forGetter {
                            it.needsUpdate
                        }


                ).apply(instance) { lastUpdate, needsUpdate ->
                    GridState(lastUpdate, needsUpdate)
                }
            }

        }
    }


    companion object {
        val CODEC: Codec<GreenhouseGrid> = RecordCodecBuilder.create { instance ->
            instance.group(
                GridState.CODEC
                .fieldOf("state")
                .forGetter { it.state }
                ,
                GreenhouseSlot.CODEC.listOf()
                    .fieldOf("slots")
                    .forGetter { grid ->
                        grid.slots.flatten()
                    },

                GreenhouseElementInstance.CODEC.listOf()
                    .fieldOf("elements")
                    .forGetter { it.elementInstances }

            ).apply(instance) { state, slots, elements ->
                val grid = GreenhouseGrid()
                grid.state = state
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