package org.magic.magicaddons.data.greenhouse.crops.definitions.rarecrops

import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.NEVER_DECAYS
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Helianthus {
    val definition = CropDefinition(
        name = "Helianthus",
        tier = CropTier.RareCrop,
        skyblockId = SkyBlockItemId.item("HELIANTHUS"),
        stages = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.4, 0.0),
                        hashString = "a98ac9224491a9dd480531bd971591724ce29921ec8f141461276f1bf3a59ed3"
                    )
                ),
                1..1,
            )
        ),
        decayTimeMs = NEVER_DECAYS,
        needsWater = false,
    )
}