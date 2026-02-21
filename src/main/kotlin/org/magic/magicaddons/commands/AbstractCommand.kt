package org.magic.magicaddons.commands

import com.mojang.brigadier.Command
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

abstract class AbstractCommand : {
    abstract var argument: String
    abstract var description: String
    abstract fun onExecute()
}