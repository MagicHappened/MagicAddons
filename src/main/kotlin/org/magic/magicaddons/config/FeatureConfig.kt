package org.magic.magicaddons.config

import com.google.gson.JsonSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Serializable
data class FeatureConfig (
    val enabled: Boolean = false,
    val extra: MutableMap<String,@Serializable(with = AnyAsJsonSerializer::class) Any>? = mutableMapOf()
)
