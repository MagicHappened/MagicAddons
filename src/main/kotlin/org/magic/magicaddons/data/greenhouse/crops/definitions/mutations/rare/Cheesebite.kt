package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.melonStemState
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.sunflowerState
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.wheatState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon.Creambloom
import org.magic.magicaddons.data.greenhouse.crops.definitions.rarecrops.Fermento
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Cheesebite {
    val definition = CropDefinition(
        name = "Cheesebite",
        tier = CropTier.Rare,
        dropMultiplier = 4.0,
        effects = setOf(
            CropEffect.ImprovedWaterRetain,
            CropEffect.HarvestLoss
        ),
        skyblockId = SkyBlockItemId.item("CHEESEBITE"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.9375, 0.0),
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717",
                        isSmall = false
                    )
                ),
                1..1
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
                        offset = Vec3(0.0, -0.75, 0.0),
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717",
                        isSmall = false
                    )
                ),
                2..2
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
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717",
                        isSmall = false
                    )
                ),
                3..3
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
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717",
                        isSmall = false
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717",
                        isSmall = false
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(4)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717",
                        isSmall = false
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = wheatState(4)
                    )
                ),
                armorStands =
                    listOf(
                        StageStand(
                            offset = Vec3(0.0, -0.4375, 0.0),
                            hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717",
                            isSmall = false
                        )
                    )
                ,
                7..7
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
                        offset = Vec3(0.0, -0.15625, 0.0),
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717",
                        isSmall = false
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = sunflowerState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717",
                        isSmall = false
                    )
                ),
                9..9
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = sunflowerState()
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.34375, 0.0),
                        hashString = "411f19c783959807338e2bf7080b1e34abb8c452464c0dce5bdf434cdc250717",
                        isSmall = false
                    )
                ),
                10..10
            )
        ),
        maxStage = 10,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Creambloom" to 4, "Fermento" to 4))
    )
}