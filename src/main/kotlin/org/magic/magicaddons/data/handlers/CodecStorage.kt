package org.magic.magicaddons.data.handlers

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import java.nio.file.Files
import java.nio.file.Path

object CodecStorage {

    private val jsonOps = JsonOps.INSTANCE

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    fun <T> save(
        path: Path,
        codec: Codec<T>,
        value: T,
        wrapperKey: String? = null
    ) {
        val encoded = codec.encodeStart(jsonOps, value)
            .resultOrPartial { error ->
                throw IllegalStateException("Codec encode error: $error")
            }.orElseThrow()

        val finalJson = if (wrapperKey != null) {
            JsonObject().apply {
                add(wrapperKey, encoded)
            }
        } else {
            encoded
        }

        DataHandler.createFile(path)
        Files.writeString(path, gson.toJson(finalJson))
    }

    fun <T> load(
        path: Path,
        codec: Codec<T>,
        wrapperKey: String? = null
    ): T? {
        if (!Files.exists(path)) return null

        val text = Files.readString(path)
        val jsonElement = JsonParser.parseString(text)

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
}