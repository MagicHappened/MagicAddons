package org.magic.magicaddons.data.handlers

import net.fabricmc.loader.api.FabricLoader
import org.magic.magicaddons.Common
import org.magic.magicaddons.config.MagicAddonsConfigJsonHandler
import org.magic.magicaddons.data.greenhouse.Codecs.GREENHOUSE_GRID_CODEC
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import java.nio.file.Files
import java.nio.file.Path

object DataHandler {
    val configDir: Path = FabricLoader.getInstance().configDir
    val modDir: Path = configDir.resolve("MagicAddons")
    val dataDir: Path = modDir.resolve("data")
    val greenhouseFile: Path = dataDir.resolve("greenhousepresets.json")

    fun init() {
        createIfMissing(modDir)
        createIfMissing(dataDir)
        loadGardenData()
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

    fun loadGardenData(){

        GreenhouseData.greenhousesInitialized = true
        GreenhouseData.greenhouseGrids = CodecStorage.load(
            greenhouseFile,
            GREENHOUSE_GRID_CODEC.listOf(),
            wrapperKey = "greenhouses"
        ) ?: run {
            GreenhouseData.greenhousesInitialized = false
            Common.LOGGER.error("Failed to load greenhouses data")
            return@run mutableListOf()
        }

        //todo add presets after.





    }


}