package org.magic.magicaddons.commands.internal.icons

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.util.ItemIconExport
import org.magic.magicaddons.util.ScreenUtil

object ExportCropIcons : AbstractCommand() {

    override val argument: String = "exportCropIcons"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument).executes {
            val requests = CropRegistry.allCrops.filter { it.skyblockId != null || it.displayItem != null }.map { crop ->
                val stack = ScreenUtil.itemStackFor(crop).takeUnless { it.`is`(Items.BARRIER) } ?: ItemStack.EMPTY
                ItemIconExport.Request(crop.name, ItemIconExport.fileNameFor(crop.name), stack)
            }

            ItemIconExport.export(requests)
                .thenAccept(IconExportMessages::sendSummary)
                .exceptionally { failure ->
                    Common.LOGGER.warn("Could not export the crop icons", failure)
                    null
                }
            return@executes 1
        }
}
