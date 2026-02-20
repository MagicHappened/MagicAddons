package org.magic.magicaddons.features

abstract class Feature {
    abstract val id: String
    abstract val category: String
    open fun register() {
        FeatureManager.register(this)
    }
    fun onTick() {}
    fun onRender() {}
}
