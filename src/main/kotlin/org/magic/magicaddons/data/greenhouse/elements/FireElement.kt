package org.magic.magicaddons.data.greenhouse.elements

import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.ElementRuntimeState
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseSlot
import org.magic.magicaddons.util.BlockUtils.isBlock
import org.magic.magicaddons.util.ScreenUtil
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
                        matcher = {
                            it.isBlock("minecraft:fire") //todo check if true
                        }
                    )
                ),
                stageRange = 1..1
            ),
        ),
        needsWater = false,
        requiredSoil = setOf(Blocks.SOUL_SAND, Blocks.NETHERRACK)

    )
    fun getFireAtSlot(slot: GreenhouseSlot, fireBlockMap: Map<BlockPos, BlockState>): ElementRuntimeState {
        val instance = GreenhouseElementInstance(
            elementId = "Fire",
            slot = slot,
            waterLevel = null,
            growthStage = null
        )

        return ElementRuntimeState(
            cropDef = definition,
            instance = instance,
            standEntities = null,
            blocksMap = fireBlockMap
        )
    }
}