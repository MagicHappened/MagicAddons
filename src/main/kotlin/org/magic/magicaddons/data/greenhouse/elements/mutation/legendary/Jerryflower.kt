package org.magic.magicaddons.data.greenhouse.elements.mutation.legendary

import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropBlockState
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import org.magic.magicaddons.data.greenhouse.CropStates.melonStemState
import org.magic.magicaddons.data.greenhouse.CropStates.wheatState
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object Jerryflower : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Jerryflower",
        skyblockId = SkyBlockItemId.item("JERRYFLOWER"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(0)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.75, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "d6a223c5610763ed43cecf187ff1cb061eb1049eca0596ab424379f9106890fb",
                        isSmall = false
                    )
                ),
                1..1
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = wheatState(1)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.65625, 0.0),
                        headRotation = Rotations(0.0f, 0.0f, 0.0f),
                        hashString = "d6a223c5610763ed43cecf187ff1cb061eb1049eca0596ab424379f9106890fb",
                        isSmall = false
                    )
                ),
                2..2
            ),
            CropStage(
                blocks = listOf(
                    CropBlockState(
                        offset = BlockPos(0, 1, 0),
                        blockState = melonStemState(5)
                    )
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.5625, 0.09375),
                        headRotation = Rotations(-22.5f, 0.0f, 0.0f),
                        hashString = "54b6a4659873ebda6ecfe4a0a230f98a6f317f1e72cf6a9a1419e3d0f83f3f60",
                        isSmall = false
                    )
                ),
                5..5
            )
        ),
        maxStage = 10,
        isMutation = true
    ) // todo: decay time unknown
}
