package org.magic.magicaddons.ui.widgets

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenDirection
import org.magic.magicaddons.ui.OverlayRenderable
import java.awt.Graphics

abstract class AbstractContextMenu : OverlayRenderable {
    abstract override val overlayX: Int
    abstract override val overlayY: Int

    abstract override val overlayWidth: Int
    abstract override val overlayHeight: Int

    override val renderPriority: Int = 0


}