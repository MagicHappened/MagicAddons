package org.magic.magicaddons.data.greenhouse.elements.basecrop

import org.magic.magicaddons.data.greenhouse.StandPose
import org.magic.magicaddons.data.greenhouse.CropEffect
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.DEFAULT_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Cocoa : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Cocoa Beans",
        effects = setOf(
            CropEffect.Immunity
        ),
        skyblockId = SkyBlockItemId.item("INK_SACK-3"),
        // the same item written the other way round, so whichever separator the game hands us matches
        aliases = listOf(SkyBlockItemId.item("INK_SACK:3")),
        displayItem = Items.COCOA_BEANS,
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "db8f7d08f93594e385058afda93b0a077b218345751c1b9415d2623110e6afbd" to StandPose.Fixed(Rotations(0.0f, 22.5f, 22.5f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(2)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                    offset = Vec3(0.0, 0.34375, 0.0),
                    headRotation = Rotations(0.0f, 22.5f, 22.5f),
                    xRotation = 0.0f,
                    yRotation = 0.0f,
                    hashString = "e1f5cb495ba97bf9c05c15b8c9cc866c14c1fe14807fed5802a0bf68deec8912"
                    )
                ),
                1..1,
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
                        offset = Vec3(0.0, -0.25, 0.0),
                        headRotation = Rotations(0.0f, 22.5f, 22.5f),
                        hashString = "e1f5cb495ba97bf9c05c15b8c9cc866c14c1fe14807fed5802a0bf68deec8912"
                    )
                ),
                2..2,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.125, 0.0),
                        hashString = "db8f7d08f93594e385058afda93b0a077b218345751c1b9415d2623110e6afbd"
                    )
                ),
                3..3,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(-0.125, 0.46875, 0.0625),
                        headRotation = Rotations(0.0f, -22.5f, -22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "e1f5cb495ba97bf9c05c15b8c9cc866c14c1fe14807fed5802a0bf68deec8912"
                    ),
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.125, 0.0),
                        hashString = "db8f7d08f93594e385058afda93b0a077b218345751c1b9415d2623110e6afbd"
                    )
                ),
                4..4,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.0, 0.0),
                        hashString = "db8f7d08f93594e385058afda93b0a077b218345751c1b9415d2623110e6afbd"
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.125, 0.5625, 0.0625),
                        hashString = "db8f7d08f93594e385058afda93b0a077b218345751c1b9415d2623110e6afbd"
                    )
                ),
                5..5,
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(0.0, 0.09375, 0.0),
                        Vec3(-0.125, 0.65625, 0.0625)
                    ),
                    rotations = listOf(
                        Rotations(0.0f, 22.5f, 22.5f),
                        Rotations(0.0f, -22.5f, -22.5f)
                    ),
                    xRotations = listOf(
                        0.0f,
                        0.0f
                    ),
                    yRotations = listOf(
                        0.0f,
                        0.0f
                    ),
                    hashString = "44d72eed58354ce14bfc497138a13564070fb4653898aeb3e66c73082ae1f993",
                    isSmall = false
                ),
                6..6,
            )


        ),
        maxStage = 6,
        needsWater = false,
        isBaseCrop = true


    )
}