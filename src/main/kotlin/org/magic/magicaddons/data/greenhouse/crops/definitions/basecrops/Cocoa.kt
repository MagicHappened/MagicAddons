package org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.melonStemState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Cocoa {
    val definition = CropDefinition(
        name = "Cocoa Beans",
        tier = CropTier.BaseCrop,
        dropMultiplier = 0.15,
        effects = setOf(
            CropEffect.Immunity
        ),
        skyblockId = SkyBlockItemId.item("INK_SACK-3"),
        aliases = listOf(SkyBlockItemId.item("INK_SACK:3")),
        displayItem = Items.COCOA_BEANS,
        standPoses = mapOf(
            "db8f7d08f93594e385058afda93b0a077b218345751c1b9415d2623110e6afbd" to StandPose.Fixed(Rotations(0.0f, 22.5f, 22.5f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(2)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                    offset = Vec3(0.0, 0.34375, 0.0),
                    headRotation = Rotations(0.0f, 22.5f, 22.5f),
                    hashString = "e1f5cb495ba97bf9c05c15b8c9cc866c14c1fe14807fed5802a0bf68deec8912",
                    isSmall = true
                    )
                ),
                1..1,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.25, 0.0),
                        headRotation = Rotations(0.0f, 22.5f, 22.5f),
                        hashString = "e1f5cb495ba97bf9c05c15b8c9cc866c14c1fe14807fed5802a0bf68deec8912",
                        isSmall = false
                    )
                ),
                2..2,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.125, 0.0),
                        hashString = "db8f7d08f93594e385058afda93b0a077b218345751c1b9415d2623110e6afbd",
                        isSmall = false
                    )
                ),
                3..3,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(-0.125, 0.46875, 0.0625),
                        headRotation = Rotations(0.0f, -22.5f, -22.5f),
                        hashString = "e1f5cb495ba97bf9c05c15b8c9cc866c14c1fe14807fed5802a0bf68deec8912",
                        isSmall = true
                    ),
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.125, 0.0),
                        hashString = "db8f7d08f93594e385058afda93b0a077b218345751c1b9415d2623110e6afbd"
                    )
                ),
                4..4,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0, 0.0),
                        headRotation = Rotations(0.0f, 22.5f, 22.5f),
                        hashString = "db8f7d08f93594e385058afda93b0a077b218345751c1b9415d2623110e6afbd",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.125, 0.5625, 0.0625),
                        headRotation = Rotations(0.0f, -22.5f, -22.5f),
                        hashString = "db8f7d08f93594e385058afda93b0a077b218345751c1b9415d2623110e6afbd",
                        isSmall = true
                    )
                ),
                5..5,
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.09375, 0.0),
                        headRotation = Rotations(0.0f, 22.5f, 22.5f),
                        hashString = "44d72eed58354ce14bfc497138a13564070fb4653898aeb3e66c73082ae1f993",
                        isSmall = false
                    ),
                    StageStand(
                        offset = Vec3(-0.125, 0.65625, 0.0625),
                        headRotation = Rotations(0.0f, -22.5f, -22.5f),
                        hashString = "44d72eed58354ce14bfc497138a13564070fb4653898aeb3e66c73082ae1f993",
                        isSmall = true
                    )
                ),
                6..6
            )),
        maxStage = 6,
        needsWater = false,
        isBaseCrop = true


    )
}