package org.magic.magicaddons.data.handlers

import net.fabricmc.loader.api.FabricLoader
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.greenhouse.Codecs.GREENHOUSE_GRID_CODEC
import org.magic.magicaddons.data.greenhouse.Codecs.GREENHOUSE_LAYOUT_CODEC
import org.magic.magicaddons.data.greenhouse.Codecs.MASTER_LAYOUT_CODEC
import org.magic.magicaddons.data.greenhouse.Codecs.MISC_GREENHOUSE_INFO_CODEC
import org.magic.magicaddons.data.greenhouse.MiscGreenhouseInfo
import org.magic.magicaddons.features.farming.greenhousePresets.GreenhouseData
import org.magic.magicaddons.util.ChatUtils
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

        GreenhouseData.miscInfo = CodecStorage.load(
            greenhouseFile,
            MISC_GREENHOUSE_INFO_CODEC,
            wrapperKey = "misc_info"
        ) ?: run {
            Common.LOGGER.error("Failed to load greenhouse misc data")
            MiscGreenhouseInfo()
        }

        GreenhouseData.presetGrids = CodecStorage.load(
            greenhouseFile,
            MASTER_LAYOUT_CODEC.listOf(),
            wrapperKey = "presets"
        )?.toMutableList() ?: run {
            Common.LOGGER.error("Failed to load preset data")
            return@run mutableListOf()
        }

        GreenhouseData.greenhousesInitialized = true
        GreenhouseData.greenhouseGrids = CodecStorage.load(
            greenhouseFile,
            GREENHOUSE_GRID_CODEC.listOf(),
            wrapperKey = "greenhouses"
        )?.toMutableList() ?: run {
            GreenhouseData.greenhousesInitialized = false
            Common.LOGGER.error("Failed to load greenhouses data")
            return@run mutableListOf()
        }
    }

    fun saveGardenData(){

        CodecStorage.save(
            path = greenhouseFile,
            codec = MISC_GREENHOUSE_INFO_CODEC,
            value = GreenhouseData.miscInfo,
            wrapperKey = "misc_info"
        )

        CodecStorage.save(
            path = greenhouseFile,
            codec = MASTER_LAYOUT_CODEC.listOf(),
            value = GreenhouseData.presetGrids,
            wrapperKey = "presets"
        )

        CodecStorage.save(
            path = greenhouseFile,
            codec = GREENHOUSE_GRID_CODEC.listOf(),
            value = GreenhouseData.greenhouseGrids,
            wrapperKey = "greenhouses"
        )



    }


}