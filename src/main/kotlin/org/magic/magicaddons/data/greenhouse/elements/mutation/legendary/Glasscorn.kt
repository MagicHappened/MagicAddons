package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import org.magic.magicaddons.data.greenhouse.CropStates.sunflowerState
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import net.minecraft.world.phys.Vec3
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.DEFAULT_DECAY_TIME_MS
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
                4..4,
            ),
            // the glass on top changes with the stage, so it is neither matched nor drawn
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 0),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = sunflowerState(DoubleBlockHalf.LOWER)
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(-0.5, 0.9375, 0.5),
                        Vec3(-0.5, 0.9375, -0.5),
                        Vec3(0.5, 0.9375, -0.5),
                        Vec3(0.5, 0.9375, 0.5)
                    ),
                    rotations = listOf(
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f)
                    ),
                    xRotations = listOf(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f
                    ),
                    yRotations = listOf(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f
                    ),
                    hashString = "a9f8488c7566989ff5b52a23b47058d4f75b3c178e8a3651bbf70b546ad2e64",
                    isSmall = false
                ) +
                CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(-0.5, 0.375, 0.5),
                        Vec3(0.5, 0.375, 0.5),
                        Vec3(0.5, 0.375, -0.5),
                        Vec3(-0.5, 0.375, -0.5)
                    ),
                    rotations = listOf(
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f)
                    ),
                    xRotations = listOf(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f
                    ),
                    yRotations = listOf(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f
                    ),
                    hashString = "ac18dc1867944dd869d085e4084e3e4013a6be4860608e3c54f0d6e542c5149f",
                    isSmall = false
                ),
                5..5
            ),
            // as placed
            CropStage(
                blocks = CropBlockState.blockStatePattern(
                    listOf(
                        BlockPos(0, 1, 0),
                        BlockPos(0, 1, 1),
                        BlockPos(1, 1, 0),
                        BlockPos(1, 1, 1)
                    ),
                    blockState = sunflowerState(DoubleBlockHalf.LOWER)
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(0.5, 0.5, 0.5),
                        Vec3(-0.5, 0.5, 0.5),
                        Vec3(-0.5, 0.5, -0.5),
                        Vec3(0.5, 0.5, -0.5)
                    ),
                    rotations = listOf(
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f)
                    ),
                    xRotations = listOf(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f
                    ),
                    yRotations = listOf(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f
                    ),
                    hashString = "297de27338b9f876e570d1cc01fe1beccfc940467c5c97c467e93e79c81c25ee",
                    isSmall = false
                ) +
                CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(0.5, 1.03125, 0.5),
                        Vec3(-0.5, 1.03125, 0.5),
                        Vec3(-0.5, 1.03125, -0.5),
                        Vec3(0.5, 1.03125, -0.5)
                    ),
                    rotations = listOf(
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f),
                        Rotations(0.0f, 0.0f, 0.0f)
                    ),
                    xRotations = listOf(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f
                    ),
                    yRotations = listOf(
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f
                    ),
                    hashString = "c85e9fa773337f43de3afc9e2a60b26299f5eea9fd8490d19ed2ab34fc0c9cbb",
                    isSmall = false
                ),
                9..9,
                placed = true
            )),
        maxStage = 9,
        footprint = Footprint(2, 2),
        requiredSoil = setOf(Blocks.SAND),
        isMutation = true
    )
}