package org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.redMushroomState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Redmushroom {
    val definition = CropDefinition(
        name = "Red Mushroom",
        tier = CropTier.BaseCrop,
        dropMultiplier = 0.08,
        effects = setOf(
            CropEffect.ImprovedHarvestBoost,
            CropEffect.WaterDrain
        ),
        skyblockId = SkyBlockItemId.item("RED_MUSHROOM"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = redMushroomState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.75, 0.0),
                        hashString = "162f0e8d32f3db02ffdf1a16efb21d614a3f0417d49138fdc18bfe52ead705b9"
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = redMushroomState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.69, 0.0),
                        hashString = "162f0e8d32f3db02ffdf1a16efb21d614a3f0417d49138fdc18bfe52ead705b9"
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = redMushroomState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.63, 0.0),
                        hashString = "162f0e8d32f3db02ffdf1a16efb21d614a3f0417d49138fdc18bfe52ead705b9"
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = redMushroomState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.57, 0.0),
                        hashString = "162f0e8d32f3db02ffdf1a16efb21d614a3f0417d49138fdc18bfe52ead705b9"
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = redMushroomState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.51, 0.0),
                        hashString = "162f0e8d32f3db02ffdf1a16efb21d614a3f0417d49138fdc18bfe52ead705b9"
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = redMushroomState()
                    )
                ),
                armorStands =
                    listOf(
                        StageStand(
                            isSmall = false,
                            offset = Vec3(0.0, -0.45, 0.0),
                            hashString = "2278b4061f63755ba5a85c1d2491c261d6bd4a5d0536a17bdad934570f3cbfee",
                        )
                    ),
                6..6
            )

        ),
        maxStage = 6,
        requiredSoil = setOf(Blocks.MYCELIUM, Blocks.PODZOL),
        needsWater = false,
        isBaseCrop = true


    )
}