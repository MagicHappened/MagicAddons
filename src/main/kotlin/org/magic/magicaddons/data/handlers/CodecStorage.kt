package org.magic.magicaddons.data.handlers

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import org.magic.magicaddons.Common
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object CodecStorage {

    private val jsonOps = JsonOps.INSTANCE

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /**
     * Writes the file whole to a temporary name and moves it over the old one, so a game killed
     * mid-write leaves the old file rather than half of the new one. The old file is kept as .bak.
     */
    fun <T> save(
        path: Path,
        codec: Codec<T>,
        value: T,
        wrapperKey: String? = null
    ) {
        val encoded = codec.encodeStart(jsonOps, value)
            .resultOrPartial { error ->
                throw IllegalStateException("Codec encode error: $error")
            }
            .orElseThrow()

        DataHandler.createFile(path)

        val rootObject = readRoot(path) ?: JsonObject()

        if (wrapperKey != null) {
            rootObject.add(wrapperKey, encoded)
        } else {
            if (encoded is JsonObject) {
                encoded.entrySet().forEach {
                    rootObject.add(it.key, it.value)
                }
            } else {
                throw IllegalStateException("Root save without wrapperKey requires JsonObject")
            }
        }

        val temporary = path.resolveSibling(path.fileName.toString() + ".tmp")
        Files.writeString(temporary, gson.toJson(rootObject))
        if (Files.exists(path) && Files.size(path) > 0) {
            Files.move(path, backupOf(path), StandardCopyOption.REPLACE_EXISTING)
        }
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    fun <T> load(
        path: Path,
        codec: Codec<T>,
        wrapperKey: String? = null
    ): T? {
        if (!Files.exists(path)) return null

        val jsonElement: JsonElement = readRoot(path) ?: return null

        val actual = if (wrapperKey != null) {
            jsonElement.asJsonObject.get(wrapperKey) ?: return null
        } else {
            jsonElement
        }

        return codec.parse(jsonOps, actual)
            .resultOrPartial { error ->
                throw IllegalStateException("Codec Decode error: $error")
            }
            .orElse(null)
    }

    /**
     * The file's json, or the backup's when the file will not parse: a file cut short is put aside
     * under a dated name and the backup takes its place, so a bad save costs one session at most.
     */
    private fun readRoot(path: Path): JsonObject? {
        parse(path)?.let { return it }

        val broken = path.resolveSibling(path.fileName.toString() + ".broken-" + LocalDateTime.now().format(STAMP))
        Common.LOGGER.error("$path is not valid json, moving it to $broken")
        runCatching { Files.move(path, broken, StandardCopyOption.REPLACE_EXISTING) }

        val backup = backupOf(path)
        val fromBackup = parse(backup) ?: return null
        Common.LOGGER.warn("Restored $path from $backup")
        runCatching { Files.copy(backup, path, StandardCopyOption.REPLACE_EXISTING) }
        return fromBackup
    }

    private fun parse(path: Path): JsonObject? {
        if (!Files.exists(path)) return null
        return runCatching { JsonParser.parseString(Files.readString(path)).asJsonObject }.getOrNull()
    }

    private fun backupOf(path: Path): Path = path.resolveSibling(path.fileName.toString() + ".bak")

    private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
}
