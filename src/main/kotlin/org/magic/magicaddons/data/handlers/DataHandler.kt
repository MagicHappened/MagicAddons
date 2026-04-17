package org.magic.magicaddons.data.handlers

import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object DataHandler {
    val configDir: Path = FabricLoader.getInstance().configDir
    val modDir: Path = configDir.resolve("MagicAddons")
    val dataDir: Path = modDir.resolve("data")

    fun init() {
        createIfMissing(modDir)
        createIfMissing(dataDir)
    }

    private fun createIfMissing(path: Path) {
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        }
    }

    fun createFile(path: Path) {
        val parent = path.parent
        if (!Files.exists(parent)) {
            Files.createDirectories(parent)
        }
    }
}