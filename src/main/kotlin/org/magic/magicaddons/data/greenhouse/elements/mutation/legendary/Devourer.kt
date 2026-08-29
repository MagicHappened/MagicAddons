package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Devourer : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Devourer",
        effects = setOf(
            CropEffect.BonusDrops,
            CropEffect.ImprovedHarvestBoost,
            CropEffect.WaterDrain
        ),
        skyblockId = SkyBlockItemId.item("DEVOURER"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                    offset = Vec3(0.0, -0.4375, 0.0),
                    headRotation = Rotations(0.0f, 45.0f, 0.0f),
                    xRotation = 0.0f,
                    yRotation = 90.0f,
                    hashString = "d5dcd6e26e5ab3c3a60ccc824c05b0fd195f526961019d3249776e8d57399d27"
                    )
                ),
                5..5
            ),
                    CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.0625, 0.0),
                        hashString = "ed83f2f247c8a9374ac9e14eb67b55dbb1f17b7db3a5052342968af71cc2c2a0"
                    )
                ),
                10..10
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.03125, 0.0),
                        hashString = "ed83f2f247c8a9374ac9e14eb67b55dbb1f17b7db3a5052342968af71cc2c2a0",
                    )
                ),
                11..11
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.03125, 0.0),
                        hashString = "4100d3b81c8dd0af22af3b42c97045bd844438d2f0297b0f267a46bd35ffb33f",
                    )
                ),
                12..12
            )

        ),
        // five days decay timer
        maxStage = 16,
        isMutation = true
    )
}