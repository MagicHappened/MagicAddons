package org.magic.magicaddons.data.greenhouse.elements.mutation.epic

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropArmorStand
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropDefinitionProvider
import org.magic.magicaddons.data.greenhouse.CropStage
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object ChorusFruit : CropDefinitionProvider {
    override val definition = CropDefinition(
        name = "Chorus Fruit",
        skyblockId = SkyBlockItemId.item("CHORUS_FRUIT"),
        stageDefs = listOf(
            CropStage(
                blocks = listOf(
                ),
                armorStands = listOf(
                    CropArmorStand(
                        offset = Vec3(0.0, -0.46875, 0.0),
                        hashMatches = {
                            it == "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.125, 0.0),
                        hashMatches = {
                            it == "c5214cc92140cdf9b402b3e7ed1fa0bac7fb4b39e39b46d11b06301caf0f9c3d"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.1875, 0.53125, 0.21875),
                        hashMatches = {
                            it == "e044238503102f64f30f4d58f0843b215d8dd54fc9f1f70cc17d8dd891bb3e5e"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(-0.09375, 0.25, -0.21875),
                        hashMatches = {
                            it == "e044238503102f64f30f4d58f0843b215d8dd54fc9f1f70cc17d8dd891bb3e5e"
                        }
                    ),
                    CropArmorStand(
                        offset = Vec3(0.0, 0.6875, 0.0),
                        hashMatches = {
                            it == "e044238503102f64f30f4d58f0843b215d8dd54fc9f1f70cc17d8dd891bb3e5e"
                        }
                    )
                ),
                12..12,
                allowRotation = true
            )

        ),
        maxStage = 12,
        requiredSoil = setOf(Blocks.END_STONE),
        needsWater = false,
        isMutation = true
    )
}