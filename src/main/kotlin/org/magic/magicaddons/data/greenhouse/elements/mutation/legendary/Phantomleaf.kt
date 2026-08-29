package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Phantomleaf : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Phantomleaf",
        effects = setOf(
            CropEffect.XpBoost,
            CropEffect.Immunity
        ),
        skyblockId = SkyBlockItemId.item("PHANTOMLEAF"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "92fb1e0e18cadb45a4d96721a9ee9c1d2c36d99826b3c23c19ee18801f721dd3" to StandPose.Fixed(Rotations(20.0f, 0.0f, 0.0f), yRotation = -90.0f),
            "988eaca2c41056ed3fb34669548843c62bae0b406441ea9d224fd7bd2f73f86e" to StandPose.Fixed(Rotations(45.0f, 0.0f, 0.0f))
        ),
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
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.375, -0.28125),
                        hashString = "988eaca2c41056ed3fb34669548843c62bae0b406441ea9d224fd7bd2f73f86e"
                    )
                ),
                5..5,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                    offset = Vec3(-0.125, -0.375, 0.0),
                    hashString = "92fb1e0e18cadb45a4d96721a9ee9c1d2c36d99826b3c23c19ee18801f721dd3")
                ),
                10..10
            )

        ),
        //five days decay time
        maxStage = 15,
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true
    )
}