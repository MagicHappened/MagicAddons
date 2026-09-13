package org.magic.magicaddons.ui.widgets.greenhouse

import org.magic.magicaddons.data.greenhouse.transfer.LayoutFormat
import org.magic.magicaddons.data.greenhouse.transfer.ShareCodeFormat
import org.magic.magicaddons.data.greenhouse.transfer.SkyLayoutsFormat
import org.magic.magicaddons.data.greenhouse.transfer.SkyMutationsFormat
import org.magic.magicaddons.data.greenhouse.transfer.SkyShardsFormat

/** The formats a layout can be imported from or exported to, each with the format behind it. */
enum class LayoutFormatType(val format: LayoutFormat) {
    MagicAddons(ShareCodeFormat),
    SkyLayouts(SkyLayoutsFormat),
    SkyShards(SkyShardsFormat),
    SkyMutations(SkyMutationsFormat)
}
