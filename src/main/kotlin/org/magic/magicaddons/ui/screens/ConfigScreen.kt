package org.magic.magicaddons.ui.screens


import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.ui.widgets.config.ConfigCategoryWidget
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.features.FeatureManager
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.ScreenUtil.drawMultilineBoxCentered

class ConfigScreen(title: Component, val parent: Screen?) : Screen(title) {

    val categoryWidgets = mutableListOf<ConfigCategoryWidget>()
    lateinit var categories: MutableMap<String, MutableList<Feature>>

    val categoryPadding: Int = 20

    val helpText: String = """
        Welcome to MagicAddons!
        Features are togglable by the check mark
        You can toggle in depth settings by right clicking the objects
    """.trimIndent()



    override fun init() {
        MagicAddonsConfigJsonHandler.load()
        categories = FeatureManager.features
            .groupBy { it.category }
            .mapValues { it.value.toMutableList() }
            .toMutableMap()

        if (categories.isEmpty()) {
            ChatUtils.sendWithPrefix("Unexpected empty category map. Cannot initialize screen.")
            return
        }

        categoryWidgets.clear()

        categories.forEach { (categoryName, featureList) ->
            categoryWidgets.add(ConfigCategoryWidget(categoryName, featureList))
        }

        var currentX = 50
        val baseY = 60

        categoryWidgets.forEach { category ->
            category.init(currentX, baseY)

            currentX += category.width + categoryPadding

            addRenderableWidget(category)
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, deltaTick)

        graphics.drawMultilineBoxCentered(helpText, width/2, 35)
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, deltaTick: Float) {
        if (this.minecraft.level == null) {
            this.extractBackground(graphics, mouseX, mouseY, deltaTick)
        }
        this.extractMenuBackground(graphics)
        this.minecraft.gui.extractDeferredSubtitles()
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, doubled: Boolean): Boolean {
        categoryWidgets.forEach {
            it.mouseClicked(mouseButtonEvent, doubled)
        }
        return super.mouseClicked(mouseButtonEvent, doubled)
    }

    override fun onClose() {
        Minecraft.getInstance().setScreen(parent)
    }

    override fun removed() {
        MagicAddonsConfigJsonHandler.save()
    }

}