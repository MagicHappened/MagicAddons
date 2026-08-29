package org.magic.magicaddons.data.greenhouse.elements

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.ElementRuntimeState
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.data.greenhouse.NEVER_DECAYS
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object FireElement : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Fire",
        skyblockId = null,
        aliases = listOf(SkyBlockItemId.item("FLINT_AND_STEEL")),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
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
    fun getFireAtSlot(slot: LayoutSlot, fireBlockMap: Map<BlockPos, BlockState>): ElementRuntimeState {
        val instance = GreenhouseElementInstance(
            elementId = "Fire",
            slot = slot,
            waterLevel = null,
            growthStage = null,
            cropDef = CropRegistry.get("Fire") ?: throw IllegalStateException("Can't find \"Fire\" Crop Definition")
        )

        return ElementRuntimeState(
            instance = instance,
            standEntities = null,
            blocksMap = fireBlockMap
        )
    }
}