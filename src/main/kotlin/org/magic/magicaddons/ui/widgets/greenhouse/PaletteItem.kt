package org.magic.magicaddons.ui.widgets.greenhouse

import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.util.ScreenUtil

sealed interface PaletteItem {
    val name: String
    val stack: ItemStack

    data class Crop(val def: CropDefinition) : PaletteItem {
        override val name: String get() = def.name
        override val stack: ItemStack get() = ScreenUtil.itemStackFor(def)
    }

    data class Soil(val block: Block) : PaletteItem {
        override val name: String get() = block.name.string
        override val stack: ItemStack get() = ItemStack(block.asItem())
    }
}
