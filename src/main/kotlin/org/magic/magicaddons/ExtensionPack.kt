package org.magic.magicaddons

object ExtensionPack {

    var isInstalled: Boolean = false
        private set

    // called from extension jar
    @JvmStatic
    fun install() {
        isInstalled = true
        Common.LOGGER.info("Enabling extra pack features.")
    }
}
