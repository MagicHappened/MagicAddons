package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import org.magic.magicaddons.data.greenhouse.CropStates.sunflowerState
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import net.minecraft.world.phys.Vec3
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.Footprint
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Glasscorn : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Glasscorn",
        effects = setOf(
            CropEffect.Immunity,
            CropEffect.ImprovedWaterRetain,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("GLASSCORN"),
        stageDefs = listOf(
            // the two stages look alike, so they are one stage that says so rather than two that
            // pretend to be told apart
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 0),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = sunflowerState()
                ) + CropBlockState.blockStatePattern(
                    positions = listOf(
                        BlockPos(0, 2, 0),
                        BlockPos(0, 2, 1),
                        BlockPos(1, 2, 0),
                        BlockPos(1, 2, 1)
                    ),
                    blockState = sunflowerState(DoubleBlockHalf.UPPER)
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(-0.5, 0.9375, -0.5),
                        Vec3(0.5, 0.9375, -0.5),
                        Vec3(-0.5, 0.9375, 0.5),
                        Vec3(0.5, 0.9375, 0.5)
                    ),
                    hashString = "a9f8488c7566989ff5b52a23b47058d4f75b3c178e8a3651bbf70b546ad2e64",
                    isSmall = false
                ) + CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(0.5, 0.375, -0.5),
                        Vec3(-0.5, 0.375, -0.5),
                        Vec3(-0.5, 0.375, 0.5),
                        Vec3(0.5, 0.375, 0.5)
                    ),
                    hashString = "ac18dc1867944dd869d085e4084e3e4013a6be4860608e3c54f0d6e542c5149f",
                    isSmall = false
                ),
                4..5,
                allowRotation = true
            )
        ),
        maxStage = 9,
        footprint = Footprint(2, 2),
        requiredSoil = setOf(Blocks.SAND),
        isMutation = true
    )
}