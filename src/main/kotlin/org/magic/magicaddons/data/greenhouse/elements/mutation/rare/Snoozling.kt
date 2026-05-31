package org.magic.magicaddons.data.greenhouse.elements.mutation.rare

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.*
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Snoozling : CropDefinitionProvider {
    private val wheatPositions = listOf(
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 2),
        BlockPos(2, 1, 0),
        BlockPos(2, 1, 2),
    )

    sealed interface SnoozlingInfo : CropExtraInfo {
        data object Sleeping : SnoozlingInfo
    }

    override val definition = CropDefinition(
        name = "Snoozling",
        skyblockId = SkyBlockItemId.item("SNOOZLING"),
        stageDefs = listOf(
            CropStage(
                blocks = CropBlockState.matcherPattern(
                    wheatPositions,
                    matcher = {
                        it.isBlock("minecraft:wheat") &&
                                it.getIntProperty("age") == 5
                    }
                ),
                armorStands =
                    listOf(
                        CropArmorStand(
                            offset = Vec3(0.0, 0.0, 0.84375),
                            hashMatches = {
                                it == "24c64afa58bef69ff567b012a2b1638cf475c5bdb050d382308399ffa0b06a8d"
                            }
                        )
                    ) +
                            CropArmorStand.matcherPattern(
                                listOf(
                                    Vec3(0.0, -0.25, -0.625),
                                    Vec3(0.0, 0.9375, -0.90625),
                                    Vec3(0.0, 0.4375, 0.40625),
                                    Vec3(0.0, -0.5625, 0.0),
                                    Vec3(0.5, -0.65625, -0.375),
                                    Vec3(-0.46875, -0.59375, -0.375)
                                ),
                                hashMatches = {
                                    it == "b82d442528456547474dd88166a97818f057ecc4b3ed350ef9a5e4dbd27f98d7"
                                }
                            ),
                20..20,
                allowRotation = true
            )

        ),
        maxStage = 20,
        footprint = Footprint(3, 3),
        isMutation = true
    )
}

/*








sleeping hash^^

 */