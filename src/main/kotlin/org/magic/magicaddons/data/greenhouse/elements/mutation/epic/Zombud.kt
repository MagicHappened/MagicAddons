package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Zombud : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Zombud",
        skyblockId = SkyBlockItemId.item("ZOMBUD"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:nether_wart") &&
                                    it.getIntProperty("age") == 2
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.375, 0.0),
                        matcher = {
                            it == "de090b85462e85f7f44be07e55f1486602c141bb6fd0c277d5bb7c68deda265d"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.21875, 0.0),
                        matcher = {
                            it == "b2c4994b7a1c45231b623b8245c117382b267c8856c57cffa2d808c241027a51"
                        }
                    )
                ),
                16..16,
                allowRotation = true
            )

        ),
        maxStage = 16,
        requiredSoil = setOf(Blocks.SOUL_SAND),
        needsWater = false,
        isMutation = true
    )
}