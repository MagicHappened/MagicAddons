package org.magic.magicaddons.commands.misc

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.ui.screens.HudEditorScreen
import org.magic.magicaddons.util.ScreenUtil

object HudCommand : AbstractCommand() {
    override val argument: String = "hud"
    override val description: String = "Opens the hud editor"

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> =
        LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument).executes {
            ScreenUtil.setScreen(HudEditorScreen())
            1
        }
}
