package org.magic.magicaddons.features.farming.greenhousePresets

import com.mojang.math.Transformation
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.state.BlockState
import org.joml.Quaternionf
import org.joml.Vector3f
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyNonGuest
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
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

    fun generateStands(){
        standsToRender = listOf()
        val stack = PlayerUtils.getItemFromHash("44d72eed58354ce14bfc497138a13564070fb4653898aeb3e66c73082ae1f993")
        val testStack = ItemStack(Items.DIRT)
        val level = Minecraft.getInstance().level ?: return
        val player = Minecraft.getInstance().player ?: return
        val stand = ArmorStand(
            level,
            player.x,
            player.y,
            player.z + 3
        )
        standsToRender = listOf(
            stand
        )

    }




}