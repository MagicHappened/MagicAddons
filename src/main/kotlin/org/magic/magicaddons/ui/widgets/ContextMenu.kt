package org.magic.magicaddons.ui.widgets

import org.magic.magicaddons.ui.OverlayRenderable

abstract class ContextMenu : OverlayRenderable {

    override val renderPriority: Int = OverlayRenderable.MENU_PRIORITY
}
