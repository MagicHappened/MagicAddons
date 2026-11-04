package org.magic.magicaddons.data.server

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.magic.magicaddons.data.greenhouse.plot.Codecs.GREENHOUSE_GRID_CODEC
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseGrid

object GridBlob {

    private val gson = Gson()

    private const val MAX_UNZIPPED_GRID_BYTES: Int = 1024 * 1024

    fun encode(grid: GreenhouseGrid): String {
        val json = gson.toJson(GREENHOUSE_GRID_CODEC.encodeStart(JsonOps.INSTANCE, grid).getOrThrow())
        val bytes = ByteArrayOutputStream().also { output -> GZIPOutputStream(output).use { it.write(json.toByteArray(Charsets.UTF_8)) } }.toByteArray()

        return Base64.getEncoder().encodeToString(bytes)
    }

    fun decode(blob: String): GreenhouseGrid {
        val bytes = Base64.getDecoder().decode(blob)
        val unzippedBytes = GZIPInputStream(bytes.inputStream()).use { it.readNBytes(MAX_UNZIPPED_GRID_BYTES + 1) }
        check(unzippedBytes.size <= MAX_UNZIPPED_GRID_BYTES) { "grid blob unzips past $MAX_UNZIPPED_GRID_BYTES bytes" }
        val json = String(unzippedBytes, Charsets.UTF_8)

        return GREENHOUSE_GRID_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow()
    }
}
