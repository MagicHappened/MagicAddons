package org.magic.magicaddons.commands

import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.features.ToggleFeature
import org.magic.magicaddons.config.MagicAddonsConfig
import org.magic.magicaddons.util.ScreenUtil

object MainCommand {
    val commandList = mutableListOf<AbstractCommand>(
        ToggleFeature
    )


    init {

        ClientCommandRegistrationCallback.EVENT.register(
            ClientCommandRegistrationCallback { dispatcher, _ ->

                val main = literal(Common.MOD_NAME)
                    .executes {
                        val maConfig = MagicAddonsConfig()
                        // todo add config implementation here
                        return@executes 1
                    }

                commandList.forEach { command ->
                    main.then(command.build())
                }

                dispatcher.register(main)
            }
        )
    }


}