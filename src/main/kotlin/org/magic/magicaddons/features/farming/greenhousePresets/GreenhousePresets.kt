package org.magic.magicaddons.features.farming.greenhousePresets

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.state.BlockState
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.elements.basecrop.Melon
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.render.RenderExtensions.renderSingleBlock
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
    var standsToRender: List<ArmorStand> = listOf()
    @JvmField
    var blockMapRenderStates: Map<BlockPos, BlockState> = mapOf()

    @Subscription
    @OnlyNonGuest
    @OnlyIn(SkyBlockIsland.GARDEN)
    private fun onIslandChange(event: IslandChangeEvent){
        GreenhouseData //for now for initialization
        CropRegistry


    }

    @Subscription
    private fun onRenderWorld(event: RenderWorldEvent){
        val dispatcher = Minecraft.getInstance().blockRenderer

        blockMapRenderStates.forEach { (pos, state) ->
            event.renderSingleBlock(
                blockRenderer = dispatcher,
                pos,
                state,
                OverlayTexture.NO_OVERLAY,
                0.4f
            )
        }
    }

    fun generatePrototype(){
        standsToRender = listOf()
        blockMapRenderStates = mapOf()

        val level = Minecraft.getInstance().level ?: return
        val player = Minecraft.getInstance().player ?: return
        val blockBelow = player.blockPosition().below()
        val melon = Melon.definition.stageDefs.last().toRenderData(level,blockBelow , Melon.definition.footprint)
        blockMapRenderStates = melon.blockMap

        standsToRender = melon.stands
    }





}