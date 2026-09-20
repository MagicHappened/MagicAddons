package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStandReader
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import org.magic.magicaddons.data.greenhouse.TEN_DAY_DECAY_TIME_MS
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Jerryflower : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Jerryflower",
        dropMultiplier = 2.0,
        skyblockId = SkyBlockItemId.item("JERRYFLOWER"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(0)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
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
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
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
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
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
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
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
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.09375),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "54b6a4659873ebda6ecfe4a0a230f98a6f317f1e72cf6a9a1419e3d0f83f3f60",
                        isSmall = false
                    )
                ),
                5..5,
                // stalled at stage 5 until it is given its ten spawn jerries; the skull is the whole
                // of the difference, so the skull is what says which look this is
                readers = listOf(CropStandReader.skullPresence(CropStandReader.ASLEEP, "54b6a4659873ebda6ecfe4a0a230f98a6f317f1e72cf6a9a1419e3d0f83f3f60"))
            ),
            // fed at stage 5: the same pose with the next skull, growing again
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.09375),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "aab9167d41116447940cd492fafcf4680f967dcc6e894089d83b4e5b82bb909b",
                        isSmall = false
                    )
                ),
                5..5,
                readers = listOf(CropStandReader.skullPresence(CropStandReader.ASLEEP, "aab9167d41116447940cd492fafcf4680f967dcc6e894089d83b4e5b82bb909b", value = 0))
            )
        ),
        // it stops at stage 5 and grows no further until fed
        sleepStages = setOf(5),
        maxStage = 10,
        isMutation = true,
        needsWater = false,
        decayTimeMs = TEN_DAY_DECAY_TIME_MS
    )
}
