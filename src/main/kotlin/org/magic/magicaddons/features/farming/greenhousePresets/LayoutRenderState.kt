package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseGrid
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.render.RenderWorldEvent
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2

object LayoutRenderState {
    init {
        SkyBlockAPI.eventBus.register(this)
        EventBus.register(this)
    }

    const val RED_TINT: Int = 0x90ff0000.toInt()
    const val NO_TINT: Int = 0x70FFFFFF

    @Volatile
    var slotRenders = listOf<SlotRenderGroup>()
    @Volatile
    var cropRenders = listOf<CropRenderGroup>()
    @Volatile
    var badStandsUUID = setOf<UUID>()
    @Volatile
    var badBlocks = mapOf<BlockPos, BlockState>()

    fun AABB.collectBlocks(
        level: Level,
        predicate: ((BlockState) -> Boolean)? = null
    ): Map<BlockPos, BlockState> {

        val result = mutableMapOf<BlockPos, BlockState>()
        val pos = BlockPos.MutableBlockPos()

        val minX = Mth.floor(minX)
        val minY = Mth.floor(minY)
        val minZ = Mth.floor(minZ)

        val maxX = Mth.floor(maxX)
        val maxY = Mth.floor(maxY)
        val maxZ = Mth.floor(maxZ)

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {

                    pos.set(x, y, z)
                    val state = level.getBlockState(pos)

                    if (predicate != null && !predicate(state)) continue

                    result[pos.immutable()] = state
                }
            }
        }

        return result
    }



    @Subscription
    private fun onRenderWorld(event: RenderWorldEvent.AfterTranslucent){
        val dispatcher = Minecraft.getInstance()

        badBlocks.forEach { (pos, state) ->
//            event.renderSingleBlock(
//                blockRenderer = dispatcher,
//                blockPos = pos,
//                blockState = state,
//                tintColor = RED_TINT
//            )
        }

        slotRenders.forEach {
//            event.renderSingleBlock(
//                blockRenderer = dispatcher,
//                blockPos = it.blockPos,
//                blockState = it.blockState,
//                tintColor = it.tint
//            )
        }

        cropRenders.forEach { cropRender ->
            cropRender.blockMap.forEach { (pos, state) ->
//                event.renderSingleBlock(
//                    blockRenderer = dispatcher,
//                    blockPos = pos,
//                    blockState = state,
//                    tintColor = cropRender.tint
//                )
            }
        }
    }

    fun generateRenderData(layout: GreenhouseLayout, grid: GreenhouseGrid){
        val level = Minecraft.getInstance().level ?: return

        val notFound = mutableListOf<String>()

        val slotRenders = mutableListOf<SlotRenderGroup>()
        val cropRenders = mutableListOf<CropRenderGroup>()
        val badStands = mutableSetOf<UUID>()
        val badBlocks = mutableMapOf<BlockPos, BlockState>()

        layout.slots.forEach {
            val block = it.placedBlock ?: return@forEach
            if (block == Blocks.AIR.defaultBlockState()) return@forEach
            if (grid.slotEquals(it)) return@forEach

            val pos = grid.getPosForSlotCoords(it.x, it.y) ?: return@forEach
            val currentBlock = level.getBlockState(pos)

            val (finalBlock, tint) = when (currentBlock) {
                Blocks.AIR.defaultBlockState() -> block to NO_TINT
                else -> currentBlock to RED_TINT
            }

            val slotRender = SlotRenderGroup(
                blockPos = pos,
                blockState = finalBlock,
                tint = tint
            )
            slotRenders.add(slotRender)
        }

        layout.elementInstances.forEach { instance ->
            if (grid.elements.any { it.instance.slot.isCoordsEqual(instance.slot)}) return@forEach

            val start = grid.getPosForSlotCoords(instance.slot.x, instance.slot.y) ?: return@forEach

            val footprint = instance.cropDef.footprint

            val max = BlockPos(
                start.x + footprint.width - 1,
                start.y + 11,
                start.z + footprint.height - 1
            )

            val entityBox = AABB(
                start.x.toDouble(), start.y.toDouble(), start.z.toDouble(),
                (max.x + 1).toDouble(), (max.y + 1).toDouble(), (max.z + 1).toDouble()
            )
            val blockBox = entityBox.setMinY(entityBox.minY + 1)

            val blockingBlocks = blockBox.collectBlocks(level){ !it.isAir }
            val blockingStands = level.getEntitiesOfClass(ArmorStand::class.java,entityBox)

            badStands.addAll(blockingStands.map { it.uuid } )
            badBlocks.putAll(blockingBlocks)

            val renderData = instance.cropDef.stageDefs.find {
                it.stageRange.last == instance.cropDef.maxStage
            }?.toRenderData(level, start, instance.cropDef.footprint )
            renderData ?: run {
                notFound.add(instance.cropDef.name)
                return@forEach
            }

            val tintColor = if (renderData.blockMap.any {
                    val currentState = level.getBlockState(it.key)
                    currentState != it.value && currentState != Blocks.AIR.defaultBlockState()
                }){
                RED_TINT
            } else {
                NO_TINT
            }

            val cropRender = CropRenderGroup(
                instance = instance,
                basePos = start,
                blockMap = renderData.blockMap,
                stands = renderData.stands,
                tint = tintColor
            )
            cropRenders.add(cropRender)
        }

        this.cropRenders = cropRenders
        this.slotRenders = slotRenders
        this.badBlocks = badBlocks
        this.badStandsUUID = badStands


        if (notFound.isNotEmpty()) {
            ChatUtils.sendWithPrefix("Unable to find ${notFound.joinToString(", ")}.")
        }
    }





    data class SlotRenderGroup(
        val blockPos: BlockPos,
        val blockState: BlockState,
        var tint: Int = NO_TINT
    )


    data class CropRenderGroup(
        val instance: GreenhouseElementInstance,
        val basePos: BlockPos,
        val blockMap: Map<BlockPos, BlockState>,
        val stands: List<ArmorStand>,
        var tint: Int = NO_TINT
    )
}