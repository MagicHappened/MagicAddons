package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Scourroot : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Scourroot",
        effects = setOf(
            CropEffect.XpBoost,
            CropEffect.Immunity
        ),
        skyblockId = SkyBlockItemId.item("SCOURROOT"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.75, -0.125),
                        headRotation = Rotations(45.0f, 0.0f, 0.0f),
                        xRotation = 0.0f,
                        yRotation = 0.0f,
                        hashString = "a9da3b8dcffbb5dd9708b83e54746fced475f0ee16c6c0ce4668cca7999c4d1e"
                    )
                ),
                1..1,
                allowRotation = true
            )

        ),
        needsWater = false,
        isMutation = true
    )
}