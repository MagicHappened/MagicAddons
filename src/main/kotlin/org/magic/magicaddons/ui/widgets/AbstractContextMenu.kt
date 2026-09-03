package org.magic.magicaddons.ui.widgets

import org.magic.magicaddons.ui.Focusable
import org.magic.magicaddons.ui.OverlayRenderable

abstract class AbstractContextMenu : OverlayRenderable, Focusable {

    override var focusedState: Boolean = false

    abstract override val overlayX: Int
    abstract override val overlayY: Int

    abstract override val overlayWidth: Int
    abstract override val overlayHeight: Int

    override val renderPriority: Int = 0


}