package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

class Veilshroom : CropDefinitionProvider {

    override val definition = CropDefinition(
        name = "Veilshroom",
        skyblockId = SkyBlockItemId.item("VEILSHROOM"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5, 0.0),
                        matcher = {
                            it == "266754af4859ef6f0adb03e6c58e9e348a507debce6b5a7f660d1269401de674"
                        }
                    )
                ),
                1..1
            )
        ),
        requiredSoil = setOf(Blocks.MYCELIUM),
        needsWater = false,
        isMutation = true
    )
}