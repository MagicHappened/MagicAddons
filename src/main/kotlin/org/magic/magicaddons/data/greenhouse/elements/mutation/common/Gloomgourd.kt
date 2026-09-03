package org.magic.magicaddons.data.greenhouse.elements.mutation.common

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropEffect
import org.magic.magicaddons.data.greenhouse.DEFAULT_DECAY_TIME_MS
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Gloomgourd : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Gloomgourd",
        effects = setOf(
            CropEffect.WaterRetain,
            CropEffect.BonusDrops
        ),
        skyblockId = SkyBlockItemId.item("GLOOMGOURD"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(6)
                    )
                ),
                armorStands = CropArmorStand.matcherPattern(
                    listOf(
                        Vec3(0.0, 0.78125, 0.0),
                        Vec3(0.0, -0.40625, 0.0)
                    ),
                    listOf(
                        Rotations(0.0f, 0.0f, -180.0f),
                        Rotations(0.0f, 0.0f, 0.0f)
                    ),
                    xRotations = listOf(0.0f, 0.0f),
                    yRotations = listOf(0.0f, 0.0f),
                    hashString = "7f693e42ba3b763292e7de26fd2b0a08fcee3bec2e017075dc66dfc4a932aa64",
                    isSmall = false
                ),
                1..1
            )


        ),
        needsWater = false,
        isMutation = true
    )

    /*
    override val standHashes: MutableList<String> = mutableListOf(
        "7f693e42ba3b763292e7de26fd2b0a08fcee3bec2e017075dc66dfc4a932aa64"
    )
    */
}