package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
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
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Gloomgourd
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Chocoberry {
    val definition = CropDefinition(
        name = "Chocoberry",
        tier = CropTier.Uncommon,
        dropMultiplier = 2.2,
        effects = setOf(
            CropEffect.WaterRetain
        ),
        skyblockId = SkyBlockItemId.item("CHOCOBERRY"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.21875, -0.75, -0.03125),
                        headRotation = Rotations(0.0f, 0.0f, -45.0f),
                        hashString = "4478a25a4c5189aa292e6076ae5938cf6c8253b7719e310118fb8312a9b62470",
                        isSmall = false
                    )
                ),
                1..1
            ),

            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.34375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, -65.0f),
                        hashString = "4478a25a4c5189aa292e6076ae5938cf6c8253b7719e310118fb8312a9b62470",
                        isSmall = false
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.34375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, -45.0f),
                        hashString = "4478a25a4c5189aa292e6076ae5938cf6c8253b7719e310118fb8312a9b62470",
                        isSmall = false
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.25, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, -45.0f),
                        hashString = "4478a25a4c5189aa292e6076ae5938cf6c8253b7719e310118fb8312a9b62470",
                        isSmall = false
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.15625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, -22.5f),
                        hashString = "4478a25a4c5189aa292e6076ae5938cf6c8253b7719e310118fb8312a9b62470",
                        isSmall = false
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.0625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "167bb9880a3ab37435a21b1f135a01a96cca45b49daeb4a1e91baf358e37d89d",
                        isSmall = false
                    )
                ),
                6..6
            )
        ),
        maxStage = 6,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Choconut" to 6, "Gloomgourd" to 2))
    )
}