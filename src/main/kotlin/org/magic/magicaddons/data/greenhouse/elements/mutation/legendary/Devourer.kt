package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.core.BlockPos
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

object Devourer : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Devourer",
        skyblockId = SkyBlockItemId.item("DEVOURER"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        matcher = {
                            it.isBlock("minecraft:melon_stem") &&
                                    it.getIntProperty("age") == 4
                        }
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.0625, 0.0),
                        matcher = {
                            it == "ed83f2f247c8a9374ac9e14eb67b55dbb1f17b7db3a5052342968af71cc2c2a0"
                        }
                    )
                ),
                10..10
            )
        ),
        maxStage = 16,
        isMutation = true
    )
}