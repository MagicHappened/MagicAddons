package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.netherwartState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Cindershade : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Cindershade",
        effects = setOf(
            CropEffect.EffectSpread,
            CropEffect.ImprovedHarvestBoost,
            CropEffect.XpLoss
        ),
        skyblockId = SkyBlockItemId.item("CINDERSHADE"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.09375, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3"
                    )
                ),
                1..1,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.71875, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -90.0f,
                        hashString = "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3"
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.625, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 90.0f,
                        hashString = "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3"
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.40625, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -90.0f,
                        hashString = "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3"
                    )
                ),
                4..4
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(0)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.25, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -180.0f,
                        hashString = "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, -0.8125, 0.0),
                        hashString = "66aa7b369efc0186937373242fe406e196281f0caf76899a4661c960b47fb74c"
                    )
                ),
                5..5
            )
            ,
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(1)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.71875, 0.0),
                        hashString = "66aa7b369efc0186937373242fe406e196281f0caf76899a4661c960b47fb74c"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.03125, -0.15625, -0.03125),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = -90.0f,
                        hashString = "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3"
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(1)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.625, 0.0),
                        hashString = "66aa7b369efc0186937373242fe406e196281f0caf76899a4661c960b47fb74c"
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.03125, -0.03125, 0.03125),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 90.0f,
                        hashString = "7bd5a39c3f9b1f513ecc299afaa5f90040fdb7424a5cd592e9ff31de7a3aafb3"
                    )
                ),
                7..7
            )
            ,
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = netherwartState(3)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "66aa7b369efc0186937373242fe406e196281f0caf76899a4661c960b47fb74c"
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.09375, 0.0),
                        headRotation = Rotations(0.0f, 45.0f, 0.0f),
                        hashString = "a0646bc0558155207204711cf5d3d07920e0e98c9b2be0b6107becb409a97427"
                    )
                ),
                8..8
            )
        ),
        maxStage = 8,
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true
    )
}