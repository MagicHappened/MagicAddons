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

object CodecStorage {

    private val jsonOps = JsonOps.INSTANCE

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /** One value of a root object: the key it is filed under, and the codec that writes it. */
    class Entry<T>(val key: String, val codec: Codec<T>, val value: T) {
        fun encode(): JsonElement = codec.encodeStart(jsonOps, value)
            .getOrThrow { IllegalStateException("Codec encode error: $it") }
    }

    /**
     * Writes every entry as one root object. The old file is copied to .bak first, the new one is
     * written to .tmp and moved over the old, and the .bak is deleted once that move succeeded.
     */
    fun save(path: Path, entries: List<Entry<*>>) {
        val root = JsonObject()
        entries.forEach { root.add(it.key, it.encode()) }

        val backup = backupOf(path)
        if (Files.exists(path) && Files.size(path) > 0) {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING)
        }

        val temporary = path.resolveSibling(path.fileName.toString() + ".tmp")
        Files.writeString(temporary, gson.toJson(root))
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)

        Files.deleteIfExists(backup)
    }

    fun <T> load(
        path: Path,
        codec: Codec<T>,
        wrapperKey: String? = null
    ): T? {
        if (!Files.exists(path)) return null

        val root = readRoot(path) ?: return null

        val actual: JsonElement = if (wrapperKey != null) {
            root.get(wrapperKey) ?: return null
        } else {
            root
        }

        return codec.parse(jsonOps, actual)
            .getOrThrow { IllegalStateException("Codec decode error: $it") }
    }

    /**
     * The file's json, or the backup's when the file will not parse and a backup does. The unreadable
     * file is kept as .broken, one copy overwritten each time, and the backup takes its place.
     */
    private fun readRoot(path: Path): JsonObject? {
        parse(path)?.let { return it }

        val backup = backupOf(path)
        val fromBackup = parse(backup)
        if (fromBackup == null) {
            Common.LOGGER.error("$path is not valid json and there is no backup to restore")
            return null
        }

        val broken = path.resolveSibling(path.fileName.toString() + ".broken")
        Common.LOGGER.error("$path is not valid json, keeping it as $broken and restoring $backup")
        runCatching { Files.copy(path, broken, StandardCopyOption.REPLACE_EXISTING) }
        runCatching { Files.copy(backup, path, StandardCopyOption.REPLACE_EXISTING) }
        return fromBackup
    }

    private fun parse(path: Path): JsonObject? {
        if (!Files.exists(path)) return null
        return runCatching { JsonParser.parseString(Files.readString(path)).asJsonObject }.getOrNull()
    }

    private fun backupOf(path: Path): Path = path.resolveSibling(path.fileName.toString() + ".bak")
}
