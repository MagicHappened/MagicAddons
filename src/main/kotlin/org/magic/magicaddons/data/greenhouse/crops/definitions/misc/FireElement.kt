package org.magic.magicaddons.data.greenhouse.crops.definitions.misc

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.NEVER_DECAYS
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.crops.ScannedPlant
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object FireElement {
    val definition = CropDefinition(
        name = "Fire",
        tier = CropTier.Other,
        skyblockId = null,
        aliases = listOf(SkyBlockItemId.item("FLINT_AND_STEEL")),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = Blocks.FIRE.defaultBlockState()
                    )
                ),
                stageRange = 1..1
            ),
        ),
        decayTimeMs = NEVER_DECAYS,
        needsWater = false,
        requiredSoil = setOf(Blocks.SOUL_SAND, Blocks.NETHERRACK)

    )
    fun getFireAtSlot(slot: LayoutSlot, fireBlockMap: Map<BlockPos, BlockState>): ScannedPlant {
        val instance = Plant(
            elementId = "Fire",
            slot = slot,
            waterLevel = null,
            growthStage = null,
            cropDef = CropRegistry.findByIdOrName("Fire") ?: throw IllegalStateException("Can't find \"Fire\" Crop Definition")
        )

        return ScannedPlant(
            plant = instance,
            stands = null,
            blocks = fireBlockMap
        )
    }
}