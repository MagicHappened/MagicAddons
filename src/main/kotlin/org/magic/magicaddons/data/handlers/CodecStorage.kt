package org.magic.magicaddons.data.handlers

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import java.nio.file.Path

object CodecStorage {

    private val jsonOps = JsonOps.INSTANCE

    private val prettyGson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    class RootEntry<T>(val key: String, val codec: Codec<T>, val value: T) {
        fun encode(): JsonElement = codec.encodeStart(jsonOps, value)
            .getOrThrow { IllegalStateException("Codec encode error: $it") }
    }

    fun save(path: Path, entries: List<RootEntry<*>>, decodeRoot: (JsonObject) -> Any?) {
        val root = JsonObject()
        entries.forEach { root.add(it.key, it.encode()) }

        ModFiles.saveTextWithBackup(path, prettyGson.toJson(root)) { decodeRoot(parseJsonObject(it)) }
    }

    fun <T> load(path: Path, decodeRoot: (JsonObject) -> T): ModFiles.LoadResult<T> =
        ModFiles.loadTextWithBackup(path) { decodeRoot(parseJsonObject(it)) }

    fun <T> decodeEntry(root: JsonObject, codec: Codec<T>, rootKey: String): T? =
        root.get(rootKey)?.let { entryJson ->
            codec.parse(jsonOps, entryJson).getOrThrow { IllegalStateException("Codec decode error in $rootKey: $it") }
        }

    private fun parseJsonObject(text: String): JsonObject = JsonParser.parseString(text).asJsonObject
}
