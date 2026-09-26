package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.legendary

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.melonStemState
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.wheatState
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.StageBlock
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.StandReader
import org.magic.magicaddons.data.greenhouse.crops.TEN_DAY_DECAY_TIME_MS
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Jerryflower {
    val definition = CropDefinition(
        name = "Jerryflower",
        tier = CropTier.Legendary,
        dropMultiplier = 2.0,
        skyblockId = SkyBlockItemId.item("JERRYFLOWER"),
        stages = listOf(
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(0)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.75, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "d6a223c5610763ed43cecf187ff1cb061eb1049eca0596ab424379f9106890fb",
                        isSmall = false
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "d6a223c5610763ed43cecf187ff1cb061eb1049eca0596ab424379f9106890fb",
                        isSmall = false
                    )
                ),
                2..2
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
                        offset = Vec3(0.0, -0.5625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "d6a223c5610763ed43cecf187ff1cb061eb1049eca0596ab424379f9106890fb",
                        isSmall = false
                    )
                ),
                3..3
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
                        offset = Vec3(0.0, -0.5625, 0.09375),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "d6a223c5610763ed43cecf187ff1cb061eb1049eca0596ab424379f9106890fb",
                        isSmall = false
                    )
                ),
                4..4
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
                        offset = Vec3(0.0, -0.5625, 0.09375),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "54b6a4659873ebda6ecfe4a0a230f98a6f317f1e72cf6a9a1419e3d0f83f3f60",
                        isSmall = false
                    )
                ),
                5..5,
                readers = listOf(StandReader.skullPresence(StandReader.ASLEEP, "54b6a4659873ebda6ecfe4a0a230f98a6f317f1e72cf6a9a1419e3d0f83f3f60"))
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
                        offset = Vec3(0.0, -0.5625, 0.09375),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "aab9167d41116447940cd492fafcf4680f967dcc6e894089d83b4e5b82bb909b",
                        isSmall = false
                    )
                ),
                5..5
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
                        offset = Vec3(0.0, -0.5625, 0.09375),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "aab9167d41116447940cd492fafcf4680f967dcc6e894089d83b4e5b82bb909b",
                        isSmall = false
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(3)
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(3)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.0625, -0.03125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "aab9167d41116447940cd492fafcf4680f967dcc6e894089d83b4e5b82bb909b",
                        isSmall = false
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(4)
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.15625, -0.03125),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "aab9167d41116447940cd492fafcf4680f967dcc6e894089d83b4e5b82bb909b",
                        isSmall = false
                    )
                ),
                8..8
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(5)
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(6)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.25, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "aab9167d41116447940cd492fafcf4680f967dcc6e894089d83b4e5b82bb909b",
                        isSmall = false
                    )
                ),
                9..9
            ),
            CropStage(
                blocks = listOf(
                    StageBlock(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(6)
                    ),
                    StageBlock(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, 0.34375, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "185f06d98a3f44adb8968ca490b5a7190a7d7472b00a3016a840b944ba917b72",
                        isSmall = false
                    )
                ),
                10..10
            )
        ),
        sleepStages = setOf(5),
        stallExplanation = "This plant needs 10 Move Jerries in order to continue growing.",
        maxStage = 10,
        isMutation = true,
        needsWater = false,
        decayTimeMs = TEN_DAY_DECAY_TIME_MS
    )
}
