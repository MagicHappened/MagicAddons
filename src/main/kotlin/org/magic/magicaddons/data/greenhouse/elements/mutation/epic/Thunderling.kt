package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Thunderling : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Thunderling",
        effects = setOf(
            CropEffect.EffectSpread
        ),
        skyblockId = SkyBlockItemId.item("THUNDERLING"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                    offset = Vec3(0.0, 0.1875, 0.0),
                    hashString = "b35914deb539a1fde1b1c473f8e05cacca257b959e7270d444c1dc5ad2bf7cc8"
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.09375, 0.0),
                        hashString = "b35914deb539a1fde1b1c473f8e05cacca257b959e7270d444c1dc5ad2bf7cc8"
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                    offset = Vec3(-0.0625, -0.71875, 0.25),
                    headRotation = Rotations(-22.5f, 0.0f, 22.5f),
                    xRotation = 0.0f,
                    yRotation = -180.0f,
                    hashString = "b68fb1ff4ecbf2e1c6e9f11c71f8f915f2d05e58a4ced08998f8b040bd671a08"
                    )
                ),
                6..6
            ),
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0625, -0.71875, -0.25),
                        headRotation = Rotations(-22.5f, 0.0f, 22.5f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b68fb1ff4ecbf2e1c6e9f11c71f8f915f2d05e58a4ced08998f8b040bd671a08"
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.21875, 0.1875, 0.03125),
                        headRotation = Rotations(22.5f, 22.5f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "b35914deb539a1fde1b1c473f8e05cacca257b959e7270d444c1dc5ad2bf7cc8"
                    )
                ),
                8..8,
                allowRotation = true
            ),
            CropStage(
                blocks = listOf(),
                armorStands = CropArmorStand.matcherPattern(
                    offsets = listOf(
                        Vec3(0.15625, -0.5, 0.21875),
                        Vec3(0.0625, -0.625, -0.25),
                        Vec3(-0.21875, -0.5, 0.03125)
                    ),
                    rotations = listOf(
                        Rotations(22.5f, 22.5f, 22.5f),
                        Rotations(-22.5f, 0.0f, 22.5f),
                        Rotations(22.5f, 22.5f, 0.0f)
                    ),
                    xRotations = listOf(
                        0.0f,
                        0.0f,
                        0.0f
                    ),
                    yRotations = listOf(
                        0.0f,
                        0.0f,
                        0.0f
                    ),
                    hashString = "3724327576a20876fc95f41bb37fd0e2f2c79014455f19262f185ce88b155385"
                ),
                16..16,
                allowRotation = true
            )



        ),
        // five days decay timer
        maxStage = 16,
        needsWater = false,
        isMutation = true
    )
}