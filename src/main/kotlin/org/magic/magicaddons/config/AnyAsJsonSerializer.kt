package org.magic.magicaddons.config

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

object AnyAsJsonSerializer : KSerializer<Any> {

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("AnyAsJson", SerialKind.CONTEXTUAL)

    override fun serialize(encoder: Encoder, value: Any) {
        require(encoder is JsonEncoder) {
            "AnyAsJsonSerializer can only be used with JSON"
        }

        val jsonElement = when (value) {
            is String -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            else -> error("Unsupported type: ${value::class}")
        }

        encoder.encodeJsonElement(jsonElement)
    }

    override fun deserialize(decoder: Decoder): Any {
        require(decoder is JsonDecoder) {
            "AnyAsJsonSerializer can only be used with JSON"
        }

        val element = decoder.decodeJsonElement()

        if (element !is JsonPrimitive) {
            error("Only primitive types are supported")
        }

        element.booleanOrNull?.let { return it }
        element.intOrNull?.let { return it }
        element.doubleOrNull?.let { return it }
        element.contentOrNull?.let { return it }

        error("Unsupported JSON value: $element")
    }
}