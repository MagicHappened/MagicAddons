package org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Thornshade : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Thornshade",
        effects = setOf(
            CropEffect.EffectSpread
        ),
        skyblockId = SkyBlockItemId.item("THORNSHADE"),
        /** Each skull's pose, found constant across every stage it appears in. */
        standPoses = mapOf(
            "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f)),
            "dcc9a4a7aadb373adc3be05242924c8985e2f993dd8e4d96f20721052ff7e7a8" to StandPose.Fixed(Rotations(0.0f, 0.0f, 0.0f))
        ),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.5625, 0.0),
                        hashString = "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f"
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.34375, 0.0),
                        hashString = "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f"
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.25, 0.0),
                        hashString = "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f"
                    )
                ),
                3..3
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.15625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f",
                        isSmall = false
                    )
                ),
                4..4
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
                        offset = Vec3(0.0, -0.0625, 0.0),
                        hashString = "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f",
                        isSmall = false
                    )
                ),
                5..5
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
                        isSmall = false,
                        offset = Vec3(0.0, 0.0625, 0.0),
                        hashString = "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f"
                    )
                ),
                6..6
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
                        offset = Vec3(0.0, 0.15625, 0.0),
                        hashString = "f847308b40613358974ba94675da63759b442dc50a241a506a77e5ca446f130f",
                        isSmall = false
                    )
                ),
                7..7
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            isSmall = false,
                            offset = Vec3(0.0, 0.15625, 0.0),
                            hashString = "dcc9a4a7aadb373adc3be05242924c8985e2f993dd8e4d96f20721052ff7e7a8",
                        )
                    )
                ,
                8..8
            )
        ,
            // as placed: the page says stage eight, the stem is still the seventh
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, 0.15625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "dcc9a4a7aadb373adc3be05242924c8985e2f993dd8e4d96f20721052ff7e7a8",
                        isSmall = false
                    )
                ),
                8..8,
                placed = true
            )),
        maxStage = 8,
        isMutation = true
    )
}