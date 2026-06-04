package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Phantomleaf : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Phantomleaf",
        skyblockId = SkyBlockItemId.item("PHANTOMLEAF"),
        stageDefs = listOf(
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0,1,0),
                        BlockPos(0,2,0)
                    ),
                    blockState = melonStemState(5)
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "57f6c922e742b5c571b1cf091d6d4bc06360f4f03443d79c5174097b0b373d7e"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.84375, 0.0),
                        hashString = "f15bd3a726eee1f2f8ffd3a92ae95c44a2f37f6b0345a795b44e0360564c67fe"
                    )
                ),
                5..5 //todo check for rotation
            )

        ),
        maxStage = 15,
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true
    )
}