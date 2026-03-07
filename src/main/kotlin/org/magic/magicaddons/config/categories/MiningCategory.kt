package org.magic.magicaddons.config.categories

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption


class MiningCategory {
    @ConfigOption(name = "Hide Powder coating", desc = "Hide powder coating particles while wearing divan armor")
    @ConfigEditorBoolean
    var hidePowderCoating = false

}