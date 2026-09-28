package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic

import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.Choconut
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.Soggybud
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Turtlellini {
    val definition = CropDefinition(
        name = "Turtlellini",
        tier = CropTier.Epic,
        dropMultiplier = 0.5,
        effects = setOf(
            CropEffect.WaterRetain,
            CropEffect.Immunity
        ),
        skyblockId = SkyBlockItemId.item("TURTLELLINI"),
        stages = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        offset = Vec3(0.0, -0.46875, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "1d1bd06a6738d0da5053eae49a1362b89489d1ac004c222504536f7bcd07679d",
                        isSmall = false
                    )
                ),
                1..1
            )
        ),
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 25, requiredNeighbourCells = mapOf("Soggybud" to 4, "Choconut" to 4))
    )
}