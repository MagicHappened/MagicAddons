package org.magic.magicaddons.commands.internal.icons

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ItemIconExport

object IconExportMessages {

    fun sendSummary(results: List<Pair<ItemIconExport.Request, ItemIconExport.Outcome>>) = onClientThread {
        val exported = results.mapNotNull { it.second as? ItemIconExport.Outcome.Exported }
        val fromPack = exported.count { it.source == ItemIconExport.Source.ResourcePack }
        val missing = results.filter { it.second == ItemIconExport.Outcome.NoIcon }.map { it.first.label }

        val text = buildString {
            append("Exported ${exported.size} icons to ${ItemIconExport.shownIconDir} ")
            append("($fromPack from the resource pack, ${exported.size - fromPack} from skulls).")
            if (missing.isNotEmpty()) append("\nNo icon for: ${missing.joinToString(", ")}")
        }
        sendOpeningFolder(text)
    }

    fun sendSingle(request: ItemIconExport.Request, outcome: ItemIconExport.Outcome) = onClientThread {
        when (outcome) {
            is ItemIconExport.Outcome.Exported -> {
                val from = if (outcome.source == ItemIconExport.Source.Skull) "its skull" else "the resource pack"
                sendOpeningFolder("Exported ${outcome.file} from $from.")
            }
            ItemIconExport.Outcome.NoIcon -> ChatUtils.sendWithPrefix("${request.label} has no resource-pack texture or skull.")
        }
    }

    private fun sendOpeningFolder(text: String) {
        ChatUtils.sendWithPrefix(ChatUtils.buildStyled(text, ChatFormatting.WHITE, click = ClickEvent.OpenFile(ItemIconExport.iconDir)))
    }

    private fun onClientThread(action: () -> Unit) = Minecraft.getInstance().execute(action)
}
