package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Shellfruit : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Shellfruit",
        skyblockId = SkyBlockItemId.item("SHELLFRUIT"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.46875, 0.0),
                        matcher = {
                            it == "72d802cd207f1971a2eb826f1a7477740833c920db00cd5c992176c67672dbf5"
                        }
                    )
                ),
                1..1
            )

        ),
        needsWater = false,
        isMutation = true
    )
}