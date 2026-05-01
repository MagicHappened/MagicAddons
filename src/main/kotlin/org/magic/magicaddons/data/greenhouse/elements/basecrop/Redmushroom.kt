package org.magic.magicaddons.data.greenhouse.elements.basecrop

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStagePattern
import org.magic.magicaddons.util.BlockUtils.isBlock
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Redmushroom : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Red Mushroom",
        skyblockId = SkyBlockItemId.item("RED_MUSHROOM"),
        stageDefs = listOf(
            CropStagePattern(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:red_mushroom")
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.75, 0.0),
                        matcher = {
                            it == "162f0e8d32f3db02ffdf1a16efb21d614a3f0417d49138fdc18bfe52ead705b9"
                        }
                    )
                ),
                stageRange = 1..5,
                baseStageStandOffset = Vec3(0.0, 0.06, 0.0)
            ),

            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0,1,0),
                        matcher = {
                            it.isBlock("minecraft:red_mushroom")
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.45, 0.0),
                        matcher = {
                            it == "2278b4061f63755ba5a85c1d2491c261d6bd4a5d0536a17bdad934570f3cbfee"
                        }
                    )
                ),
                6..6
            )
        ),
        requiredSoil = setOf(Blocks.MYCELIUM),
        needsWater = false,
        isBaseCrop = true


    )
}