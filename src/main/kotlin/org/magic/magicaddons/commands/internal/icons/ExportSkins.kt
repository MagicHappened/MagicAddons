package org.magic.magicaddons.commands.internal.icons

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ItemIconExport
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockItemId

object ExportSkins : AbstractCommand() {

    override val argument: String = "exportSkins"

    private const val HAND: String = "hand"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes { exportHand() }
            .then(
                RequiredArgumentBuilder.argument<FabricClientCommandSource, String>("identifier", StringArgumentType.greedyString())
                    .executes {
                        val identifier = StringArgumentType.getString(it, "identifier").trim()
                        if (identifier.equals(HAND, ignoreCase = true)) exportHand() else exportItem(identifier.uppercase())
                    }
            )

    private fun exportHand(): Int {
        val stack = Minecraft.getInstance().player?.mainHandItem?.takeUnless { it.isEmpty }
        if (stack == null) {
            ChatUtils.sendWithPrefix("Your hand is empty.")
            return 0
        }

        val id = stack.getSkyBlockId()?.id ?: BuiltInRegistries.ITEM.getKey(stack.item).path
        return export(ItemIconExport.Request(id, ItemIconExport.fileNameFor(id), stack))
    }

    private fun exportItem(id: String): Int {
        val stack = SkyBlockItemId.item(id).toItem().takeUnless { it.isEmpty }
        if (stack == null) {
            ChatUtils.sendWithPrefix("No item with the id $id.")
            return 0
        }

        return export(ItemIconExport.Request(id, ItemIconExport.fileNameFor(id), stack))
    }

    private fun export(request: ItemIconExport.Request): Int {
        ItemIconExport.export(listOf(request))
            .thenAccept { results -> results.forEach { (exported, outcome) -> IconExportMessages.sendSingle(exported, outcome) } }
            .exceptionally { failure ->
                Common.LOGGER.warn("Could not export the icon for {}", request.label, failure)
                null
            }
        return 1
    }
}
