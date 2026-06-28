package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.farming.greenhousePresets.LayoutRenderState.NO_TINT
import org.magic.magicaddons.features.farming.greenhousePresets.LayoutRenderState.RED_TINT
import org.magic.magicaddons.util.ChatUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.render.RenderWorldEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

object GreenhousePresets : Feature() {

    init {
        SkyBlockAPI.eventBus.register(this)
    }
    override val id = "GreenhousePresets"
    override val displayName = "Greenhouse Presets"
    override val tooltipMessage = "Enables Greenhouse Presets..."
    override val category = "farming"

    override val baseSetting = BooleanSetting(
        displayName = displayName,
        tooltip = tooltipMessage,
        value = true,
    )


    @JvmField
    var standsToRender: List<Pair<ArmorStand, Int>> = listOf()
    @JvmField
    var blockMapRenderStates: Map<BlockPos, Pair<BlockState, Int>> = mapOf()

    @Subscription
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    private fun onIslandChange(event: IslandChangeEvent){
        GreenhouseData //for now for initialization
        CropRegistry


    }


    //todo dont render on top of other blocks just render a red outline and then when breaking said block
    // will render what to place
    @Subscription
    private fun onRenderWorld(event: RenderWorldEvent.AfterTranslucent){
        val dispatcher = Minecraft.getInstance().blockModelResolver

        blockMapRenderStates.forEach { (pos, pair) ->
//            event.renderSingleBlock(
//                blockRenderer = dispatcher,
//                pos,
//                pair.first,
//                pair.second
//            )
            //todo need to find another method
        }
    }

    fun generateRenderData(){
        standsToRender = listOf()
        blockMapRenderStates = mapOf()

        val level = Minecraft.getInstance().level ?: return
        val player = Minecraft.getInstance().player ?: return
        val base = player.blockPosition().below()

        val layout = GreenhouseData.currentPreset ?: return

        val notFound = mutableListOf<String>()
        //todo need to somehow transmit information to mark red blocks and armor stands (incorrect.)

        val standList = mutableListOf<Pair<ArmorStand, Int>>()
        val blockMap = mutableMapOf<BlockPos, Pair<BlockState, Int>>()

        LayoutRenderState.slotRenders.clear()
        LayoutRenderState.cropRenders.clear()

        layout.slots.forEach {
            val block = it.placedBlock ?: return@forEach
            val pos = BlockPos(base.x + it.x, base.y, base.z + it.y)
            val currentBlock = level.getBlockState(pos)

            if (currentBlock == block) return@forEach
            val (finalBlock, tint) = when (currentBlock) {
                Blocks.AIR.defaultBlockState() -> block to NO_TINT
                else -> currentBlock to RED_TINT
            }

            val slotRender = LayoutRenderState.SlotRenderGroup(
                blockPos = pos,
                blockState = finalBlock,
                tint = tint
            )
            LayoutRenderState.slotRenders.add(slotRender)
        }

        layout.elementInstances.forEach { instance ->
            val baseElementBlockPos = BlockPos(base.x + instance.slot.x , base.y, base.z + instance.slot.y)
            val renderData = instance.cropDef.stageDefs.find {
                it.stageRange.last == instance.cropDef.maxStage
            }?.toRenderData(level, baseElementBlockPos, instance.cropDef.footprint )
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

            renderData.blockMap.forEach { (pos, state) ->


                blockMap[pos] = Pair(state, tintColor)
            }
            renderData.stands.forEach {
                standList.add(Pair(it, tintColor))
            }

        }
        ChatUtils.sendWithPrefix("Unable to find: ${notFound.joinToString(", ")}")

        blockMapRenderStates = blockMap
        standsToRender = standList
    }





}