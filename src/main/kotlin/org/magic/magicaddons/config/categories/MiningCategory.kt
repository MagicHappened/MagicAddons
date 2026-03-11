package org.magic.magicaddons.config.categories

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption


class MiningCategory {
    @ConfigOption(name = "Hide Powder Coating", desc = "Hide powder coating particles while wearing divan armor")
    @ConfigEditorBoolean
    var hidePowderCoating = false

}