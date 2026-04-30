package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.commands.features.farming.GreenhouseScreenCommand
import org.magic.magicaddons.data.greenhouse.GreenhouseElementRegistry
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils
import tech.thatgravyboat.skyblockapi.api.profile.garden.PlotAPI

object DebugFarming : AbstractCommand() {
    override val argument: String = "farming"
    override val description: String = "farming debug"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(GreenhouseScreenCommand.argument).executes {

            val world = Minecraft.getInstance().level ?: return@executes 0
            val plot = PlotAPI.getCurrentPlot()?.aabb ?: return@executes 0
            val buildableArea = GreenhouseData.getBuildableArea(plot)
            val entities = world.getEntities(null, buildableArea)
            val foundHashes: MutableList<String> = mutableListOf()

            for (entity in entities) {
                if (entity !is ArmorStand) continue

                val pos = entity.position()

                val headItem = entity.getItemBySlot(EquipmentSlot.HEAD)
                if (headItem.isEmpty) continue

                val hash = PlayerUtils.getSkinHash(headItem) ?: continue
                foundHashes += hash
                ChatUtils.sendWithPrefix("Found stand at $pos hash=$hash")
            }


            val minX = buildableArea.minX.toInt()
            val minZ = buildableArea.minZ.toInt()
            val maxX = buildableArea.maxX.toInt()
            val maxZ = buildableArea.maxZ.toInt()
            // iterate through all armor stands, then find a placement that matches a crop placement
            // if none is found skip
            // then do the same for blocks
            // we will need to gather all the orientation of all the blocks/stands for every growth stage for every crop
            // :sob:
            // for starters we will do regular crops
            // then regular crops with stands
            // then 1 growth mutations
            // then multi stage mutations
            // then multi area mutations


            for (x in minX until maxX) {
                for (z in minZ until maxZ) {
                    val pos = BlockPos(x, 74, z)
                    val state = world.getBlockState(pos)

                    if (!state.isAir) {
                        ChatUtils.sendWithPrefix("Found block at $pos: ${state.block}")
                    }
//                    for (factory in GreenhouseElementRegistry.getAllFactories()) {
//                        val element = factory.invoke()
//
//                        // skip empty definitions
//                        if (element.blocks.isEmpty() && element.standHashes.isEmpty()) continue
//
//                        val blockMatch = element.blocks.contains(state.block)
//
//                        val hashMatch = element.standHashes.any { it in foundHashes }
//
//                        if (blockMatch || hashMatch) {
//                            ChatUtils.sendWithPrefix(
//                                "Detected element '${element.name}' at $pos (block=${state.block})"
//                            )
//                        }
//                    }
                }
            }
            return@executes 1
        }
    }
}