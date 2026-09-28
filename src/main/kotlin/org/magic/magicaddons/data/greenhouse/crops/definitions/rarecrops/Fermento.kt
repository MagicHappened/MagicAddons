package org.magic.magicaddons.data.greenhouse.crops.definitions.rarecrops

import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.NEVER_DECAYS
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Fermento {
    val definition = CropDefinition(
        name = "Fermento",
        tier = CropTier.RareCrop,
        skyblockId = SkyBlockItemId.item("FERMENTO"),
        stages = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.4, 0.0),
                        hashString = "cb41daeb57d2ae62c66e58eb6debb2a7d446e34541a771350728c9db15beafba",
                        isSmall = false
                    )
                ),
                1..1
            )
        ),
        decayTimeMs = NEVER_DECAYS,
        needsWater = false,
    )
}