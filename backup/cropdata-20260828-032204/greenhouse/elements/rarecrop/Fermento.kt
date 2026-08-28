package org.magic.magicaddons.data.greenhouse.elements.rarecrop

import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Fermento : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Fermento",
        skyblockId = SkyBlockItemId.item("FERMENTO"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.4000000000000057, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "cb41daeb57d2ae62c66e58eb6debb2a7d446e34541a771350728c9db15beafba"
                    )
                ),
                1..1
            )
        ),
        needsWater = false,
        isRareCrop = true
    )
}