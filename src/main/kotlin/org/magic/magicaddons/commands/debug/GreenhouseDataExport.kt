package org.magic.magicaddons.commands.debug

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import net.minecraft.client.Minecraft
import org.magic.magicaddons.data.greenhouse.plot.Codecs.GREENHOUSE_GRID_CODEC
import org.magic.magicaddons.data.server.ServerGreenhouseData
import org.magic.magicaddons.data.server.ServerGreenhouseData.MissingData
import org.magic.magicaddons.features.farming.greenhousePresets.greenhousesState.GreenhouseData
import org.magic.magicaddons.util.ChatUtils

object GreenhouseDataExport {

    private const val BYTES_PER_KB: Int = 1024

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun copyServerUpload() {
        val upload = ServerGreenhouseData.ofActiveProfile()
        if (upload == null) {
            val missing = ServerGreenhouseData.missingDataOfActiveProfile().joinToString(", ") { missingDataText(it) }
            ChatUtils.sendWithPrefix("Nothing to copy yet: missing $missing.")
            return
        }

        val json = gson.toJson(upload)
        copyToClipboard(json)
        ChatUtils.sendWithPrefix("Copied the server upload to your clipboard (${upload.plots.size} plots, ${kilobytesOf(json)} KB).")
    }

    fun copyCurrentGreenhouse() {
        val grid = GreenhouseData.getCurrentGrid()
        if (grid == null) {
            ChatUtils.sendWithPrefix("Not standing in a greenhouse.")
            return
        }

        val json = gson.toJson(GREENHOUSE_GRID_CODEC.encodeStart(JsonOps.INSTANCE, grid).getOrThrow())
        copyToClipboard(json)
        ChatUtils.sendWithPrefix("Copied ${grid.layout.displayName()}'s saved data to your clipboard (${grid.layout.plants.size} plants, ${kilobytesOf(json)} KB).")
    }

    fun copyAllGreenhouses() {
        val grids = GreenhouseData.greenhouseGrids
        if (grids.isEmpty()) {
            ChatUtils.sendWithPrefix("No greenhouse saved yet for this profile.")
            return
        }

        val json = gson.toJson(GREENHOUSE_GRID_CODEC.listOf().encodeStart(JsonOps.INSTANCE, grids).getOrThrow() as JsonElement)
        copyToClipboard(json)
        val greenhouses = if (grids.size == 1) "1 greenhouse" else "${grids.size} greenhouses"
        ChatUtils.sendWithPrefix("Copied the saved data of $greenhouses to your clipboard (${kilobytesOf(json)} KB).")
    }

    private fun copyToClipboard(json: String) {
        Minecraft.getInstance().keyboardHandler.clipboard = json
    }

    private fun kilobytesOf(json: String): Int = (json.toByteArray().size / BYTES_PER_KB).coerceAtLeast(1)

    private fun missingDataText(missing: MissingData): String = when (missing) {
        MissingData.CropGrowth -> "crop growth"
        MissingData.CropSpeedUpgrade -> "crop speed upgrade"
        MissingData.TickTime -> "tick time"
        MissingData.ProfileName -> "profile name"
        MissingData.ScannedGreenhouse -> "scanned greenhouse"
    }
}
