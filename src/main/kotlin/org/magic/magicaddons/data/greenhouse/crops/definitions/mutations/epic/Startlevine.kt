package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.melonStemState
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.sunflowerState
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.wheatState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.FIVE_DAY_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Blastberry
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Cheesebite
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Startlevine {
    val definition = CropDefinition(
        name = "Startlevine",
        tier = CropTier.Epic,
        dropMultiplier = 7.5,
        effects = setOf(
            CropEffect.ImprovedWaterRetain,
            CropEffect.ImprovedXpBoost,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("STARTLEVINE"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.8125, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(0)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.8125, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.8125, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.6875, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.6875, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.59375, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(5)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.59375, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                8..8
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
                        offset = Vec3(0.0, -0.5, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                9..9
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.5, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                10..10
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
                        offset = Vec3(0.0, -0.4375, 0.0),
                        hashString = "a7c545c10c035790615642a9ed6d689448b778cc16ac423c0f7fb19a0d057c6a",
                        isSmall = false
                    )
                ),
                11..11
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = sunflowerState()
                    )
                ),
                armorStands =
                    listOf(
                        StageStand(
                            offset = Vec3(0.0, -0.4375, 0.0),
                            hashString = "98bef15a64354093d26b8f002e476b8012ed3ad9b061796953b6b1dad447d7",
                            isSmall = false
                        )
                    )
                ,
                12..12
            )




        ),
        decayTimeMs = FIVE_DAY_DECAY_TIME_MS,
        maxStage = 12,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Blastberry" to 4, "Cheesebite" to 4))
    )
}