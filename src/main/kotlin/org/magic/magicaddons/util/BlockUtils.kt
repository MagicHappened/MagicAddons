package org.magic.magicaddons.util

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.IntegerProperty

object BlockUtils {

    fun BlockState.isBlock(id: String): Boolean {
        return BuiltInRegistries.BLOCK.getKey(this.block).toString() == id
    }
    fun BlockState.getId(): String {
        return BuiltInRegistries.BLOCK.getKey(this.block).toString()
    }

    fun BlockState.getIntProperty(name: String): Int? {
        val prop = this.block.stateDefinition.properties
            .find { it.name == name } as? IntegerProperty

        return prop?.let { this.getValue(it) }
    }
}