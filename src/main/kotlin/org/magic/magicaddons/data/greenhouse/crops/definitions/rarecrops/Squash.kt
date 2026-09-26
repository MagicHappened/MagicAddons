package org.magic.magicaddons.data.greenhouse.crops.definitions.rarecrops

import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.NEVER_DECAYS
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Squash {
    val definition = CropDefinition(
        name = "Squash",
        tier = CropTier.RareCrop,
        skyblockId = SkyBlockItemId.item("SQUASH"),
        stages = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.4, 0.0),
                        hashString = "36ae076649ef22f60e8511831c68fd2b6ea63c32164dab33a8aebc18ff2a54c8"
                    )
                ),
                1..1,
            )
        ),
        decayTimeMs = NEVER_DECAYS,
        needsWater = false,
    )
}