package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Chloronite : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Chloronite",
        effects = setOf(
            CropEffect.Immunity
        ),
        skyblockId = SkyBlockItemId.item("CHLORONITE"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.84375, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "4696299926a2fd000f519f6b4690670914004e634c8c6546ca5b69f028e43c40"
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                    offset = Vec3(0.0, -0.84375, 0.0),
                    headRotation = Rotations(0.0f, 45.0f, 0.0f),
                    xRotation = 0.0f,
                    yRotation = 90.0f,
                    hashString = "4696299926a2fd000f519f6b4690670914004e634c8c6546ca5b69f028e43c40"
                    )
                ),
                2..2
            ),
                    CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.75, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "4696299926a2fd000f519f6b4690670914004e634c8c6546ca5b69f028e43c40"
                    )
                ),
                4..4,
                allowRotation = true
            ),
            CropStagePattern(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(2)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.65625, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "4696299926a2fd000f519f6b4690670914004e634c8c6546ca5b69f028e43c40"
                    )
                ),
                stageRange = 5..6,
                allowRotation = true,
                baseStageStandOffset = Vec3(0.0, 0.0, 0.0)
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(2),
                        required = false
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.46875, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "3d9bcd3946c162aa361e537a455eddae3b55fb4bcf6208e84662b622b3ff6737"
                    )
                ),
                10..10,
                allowRotation = true
            ),
            CropStage(
                // the head is the whole of the match. A chloronite that grew to its last stage
                // stands under green glass and one placed by hand does not, so neither block can
                // be asked for; the glass is drawn because it is what the finished crop looks
                // like, and asked of nothing
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = Blocks.STAINED_GLASS.green.defaultBlockState(),
                        required = false
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.4, 0.0),
                        hashString = "98056b960ff385c20cffc3d1524500fcd3bf8c31b6dcafd8520f41dfa749dd28"
                    )
                ),
                10..10
            )

        ),
        maxStage = 10,
        isMutation = true
    )
}