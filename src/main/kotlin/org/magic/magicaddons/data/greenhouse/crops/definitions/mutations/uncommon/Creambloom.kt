package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.melonStemState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Choconut
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Creambloom {
    val definition = CropDefinition(
        name = "Creambloom",
        tier = CropTier.Uncommon,
        dropMultiplier = 2.8,
        effects = setOf(
            CropEffect.Immunity
        ),
        skyblockId = SkyBlockItemId.item("CREAMBLOOM"),
        stages = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "c777c2f2d01bd8d93f8694d63e1fcbee74aef4664625720e9062c6c7675f35a",
                        isSmall = false
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "c777c2f2d01bd8d93f8694d63e1fcbee74aef4664625720e9062c6c7675f35a",
                        isSmall = false
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.25, 0.0),
                        hashString = "c777c2f2d01bd8d93f8694d63e1fcbee74aef4664625720e9062c6c7675f35a",
                        isSmall = false
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.0625, 0.0),
                        hashString = "c777c2f2d01bd8d93f8694d63e1fcbee74aef4664625720e9062c6c7675f35a",
                        isSmall = false
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "c777c2f2d01bd8d93f8694d63e1fcbee74aef4664625720e9062c6c7675f35a",
                        isSmall = false
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "60e98c0b598e2f5ce7c5ee8183ed157c5436a00585da711f8a87f24ea1ff055b"
                    )
                ),
                6..6
            )
        ),
        maxStage = 6,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Choconut" to 8))
    )
}