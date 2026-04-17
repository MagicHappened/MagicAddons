package org.magic.magicaddons.data.handlers

import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import java.nio.file.Files
import java.nio.file.Path

object CodecStorage {

    private val jsonOps = JsonOps.INSTANCE

    fun <T> save(path: Path, codec: Codec<T>, value: T) {
        val result = codec.encodeStart(jsonOps, value)

        val json = result.resultOrPartial { error ->
            throw IllegalStateException("Codec encode error: $error")
        }.orElseThrow()

        DataHandler.createFile(path)

        Files.writeString(path, json.toString())
    }

    fun <T> load(path: Path, codec: Codec<T>): T? {
        if (!Files.exists(path)) return null

        val text = Files.readString(path)
        val jsonElement = JsonParser.parseString(text)

        return codec.parse(jsonOps, jsonElement)
            .resultOrPartial { error ->
                throw IllegalStateException("Codec Decode error: $error")
            }
            .orElse(null)
    }
}