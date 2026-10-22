package org.magic.magicaddons.features.debug

import com.google.gson.JsonObject
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ItemStackJson

object ContainerItemDump : Feature() {

    override val id: String = "ContainerItemDump"
    override val displayName: String = "Dump Items Button"
    override val description: String = "Adds a Dump Items button beside container screens that copies every item in the container as json"
    override val category: String = "debug"

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false
    )

    private const val BUTTON_WIDTH: Int = 70
    private const val BUTTON_HEIGHT: Int = 20
    private const val BUTTON_GAP: Int = 4

    private val BUTTON_TEXT: Component = Component.literal("Dump Items")

    init {
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            if (!baseSetting.value || screen !is AbstractContainerScreen<*>) return@register
            val button = Button.builder(BUTTON_TEXT) { copyItems(screen) }
                .bounds(maxOf(0, screen.leftPos - BUTTON_WIDTH - BUTTON_GAP), screen.topPos, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()
            Screens.getWidgets(screen).add(button)
        }
    }

    private fun copyItems(screen: AbstractContainerScreen<*>) {
        val items = JsonObject()
        screen.menu.slots
            .filter { it.container !is Inventory && !it.item.isEmpty }
            .forEach { slot -> ItemStackJson.of(slot.item)?.let { items.add(slot.index.toString(), it) } }

        val dump = JsonObject()
        dump.addProperty("title", screen.title.string)
        dump.add("items", items)

        Minecraft.getInstance().keyboardHandler.clipboard = ItemStackJson.text(dump)
        ChatUtils.sendWithPrefix("Copied ${items.size()} items from ${screen.title.string} to your clipboard.")
    }
}
