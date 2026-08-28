package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object AllinAloe : CropDefinitionProvider {
    val fragmentSkyblockId: SkyBlockId = SkyBlockItemId.item("ALL_IN_ALOE_FRAGMENT")

    override val definition = CropDefinition(
        name = "All-in Aloe",
        effects = setOf(
            CropEffect.HarvestBoost
        ),
        skyblockId = SkyBlockItemId.item("ALL_IN_ALOE"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(7)
                    ),
                    CropBlockState(
                        offset = BlockPos(0, 2, 0),
                        blockState = melonStemState(4)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.09375, 0.15625, 0.0),
                        hashString = "241163612258d30dc6ef63b21f61ba89c622e5dcebd99fd36a3b507e80cdc725"
                    )
                ),
                7..7,
                allowRotation = true
            )

        ),
        maxStage = 27,
        requiredSoil = setOf(Blocks.SAND),
        needsWater = false,
        isMutation = true
    )
}