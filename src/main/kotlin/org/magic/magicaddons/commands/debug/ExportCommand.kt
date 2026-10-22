package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractDebugCommand
import org.magic.magicaddons.util.ChatUtils

object ExportCommand : AbstractDebugCommand() {

    override val argument: String = "export"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument)
            .executes {
                it.source.sendError(ChatUtils.buildWithPrefix("missing export argument"))
                return@executes 0
            }
            .then(ExportCropIcons.build())
            .then(ExportSkins.build())
            .then(ExportHeldItem.build())
            .then(ExportSackMessage.build())
}
