package org.magic.magicaddons.commands

import me.shedaniel.clothconfig2.api.ConfigBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import org.magic.magicaddons.Common
import org.magic.magicaddons.commands.features.ToggleFeature
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
                        val builder = ConfigBuilder.create()
                            .setParentScreen(null)
                            .setTitle(Text.literal("Magic Addons"))
                        val config = builder.build()
                        ScreenUtil.setScreen(config)
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