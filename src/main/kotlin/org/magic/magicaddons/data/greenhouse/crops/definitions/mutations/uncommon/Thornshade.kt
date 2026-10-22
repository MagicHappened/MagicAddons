package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.melonStemState
import org.magic.magicaddons.data.greenhouse.crops.CropDrop
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Veilshroom
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.Wildrose
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Thornshade {
    val definition = CropDefinition(
        name = "Thornshade",
        minMutationsBeforeDecay = 8,
        tier = CropTier.Uncommon,
        dropMultiplier = 2.0,
        drops = mapOf(CropDrop.BrownMushroom to 190, CropDrop.RedMushroom to 190, CropDrop.WildRose to 800),
        effects = setOf(
            CropEffect.EffectSpread
        ),
        skyblockId = SkyBlockItemId.item("THORNSHADE"),
        growingStages = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.5625, 0.0),
                        hashString = "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f"
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
                        isSmall = false,
                        offset = Vec3(0.0, -0.34375, 0.0),
                        hashString = "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f"
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
                        isSmall = false,
                        offset = Vec3(0.0, -0.25, 0.0),
                        hashString = "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f"
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
                        offset = Vec3(0.0, -0.15625, 0.0),
                        hashString = "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f",
                        isSmall = false
                    )
                ),
                4..4
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
                        offset = Vec3(0.0, -0.0625, 0.0),
                        hashString = "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f",
                        isSmall = false
                    )
                ),
                5..5
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
                        isSmall = false,
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f"
                    )
                ),
                6..6
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
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f",
                        isSmall = false
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands =
                    listOf(
                        StageStand(
                            isSmall = false,
                            offset = Vec3(0.0, 0.15625, 0.0),
                            hashString = "dcc9a4a7aadb373adc3be05242924c8985e2f993dd8e4d96f20721052ff7e7a8"
                        )
                    )
                ,
                8..8
            )
        ),
        maxStage = 8,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf(Wildrose.definition to 4, Veilshroom.definition to 4))
    )
}