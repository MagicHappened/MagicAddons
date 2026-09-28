package org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropEffect
import org.magic.magicaddons.data.greenhouse.crops.CropStage
import org.magic.magicaddons.data.greenhouse.crops.CropTier
import org.magic.magicaddons.data.greenhouse.crops.SpawnRule
import org.magic.magicaddons.data.greenhouse.crops.StageStand
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Veilshroom {

    val definition = CropDefinition(
        name = "Veilshroom",
        tier = CropTier.Common,
        dropMultiplier = 0.15,
        effects = setOf(
            CropEffect.ImprovedHarvestBoost,
            CropEffect.WaterDrain
        ),
        skyblockId = SkyBlockItemId.item("VEILSHROOM"),
        stages = listOf(
            CropStage(
                blocks = listOf(),
                armorStands = listOf(
                    StageStand(
                        isSmall = false,
                        offset = Vec3(0.0, -0.5, 0.0),
                        hashString = "266754af4859ef6f0adb03e6c58e9e348a507debce6b5a7f660d1269401de674"
                    )
                ),
                1..1
            )
        ),
        requiredSoil = setOf(Blocks.MYCELIUM, Blocks.PODZOL),
        needsWater = false,
        isMutation = true,
        spawnRule = SpawnRule(weight = 30, requiredNeighbourCells = mapOf("Red Mushroom" to 1, "Brown Mushroom" to 1))
    )
}