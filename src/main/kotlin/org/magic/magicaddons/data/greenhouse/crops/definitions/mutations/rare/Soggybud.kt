package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare

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
import org.magic.magicaddons.data.greenhouse.crops.StandPose
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Melon
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Gloomgourd
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Soggybud {
    val definition = CropDefinition(
        name = "Soggybud",
        tier = CropTier.Rare,
        dropMultiplier = 1.2,
        effects = setOf(
            CropEffect.WaterRetain
        ),
        skyblockId = SkyBlockItemId.item("SOGGYBUD"),
        maxStage = 10,
        drainsNeighbours = true,
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stages = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356",
                        isSmall = true
                    )
                ),
                1..2
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356",
                        isSmall = true
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.1875, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356",
                        isSmall = true
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356",
                        isSmall = false
                    )
                ),
                5..5
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.5625, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356",
                        isSmall = false
                    )
                ),
                6..6
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
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356",
                        isSmall = false
                    )
                ),
                7..7
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
                        offset = Vec3(0.0, -0.1875, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356",
                        isSmall = false
                    )
                ),
                8..8
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
                        offset = Vec3(0.0, -0.09375, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356",
                        isSmall = false
                    )
                ),
                9..9
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
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "b4bdf477d2f417f75798ad6377b131aca787be9bc05a2fddc1972d81d40c7356",
                        isSmall = false
                    )
                ),
                10..10
            )
        ),
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Melon" to 2, "Gloomgourd" to 2))
    )
}