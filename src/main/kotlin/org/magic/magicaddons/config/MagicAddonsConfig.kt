package org.magic.magicaddons.config

import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import org.magic.magicaddons.Common
import org.magic.magicaddons.config.categories.MiningCategory

class MagicAddonsConfig : Config() {

    override fun getTitle(): StructuredText? {
        return StructuredText.of(Common.MOD_NAME + " Config");
    }

    @Category(name = "MiningCategory", desc = "Mining related features")
    val mining = MiningCategory()


}