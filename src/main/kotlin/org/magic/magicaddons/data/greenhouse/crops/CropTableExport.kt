package org.magic.magicaddons.data.greenhouse.crops

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.SharedConstants
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.Bootstrap
import java.nio.file.Files
import java.nio.file.Path

object CropTableExport {

    const val TABLE_VERSION: Int = 1

    @JvmStatic
    fun main(args: Array<String>) {
        SharedConstants.tryDetectVersion()
        Bootstrap.bootStrap()

        val output = Path.of(args.firstOrNull() ?: "crops.json")
        val table = JsonObject().apply {
            addProperty("version", TABLE_VERSION)
            add("crops", JsonArray().apply { CropRegistry.allCrops.forEach { add(cropJsonOf(it)) } })
        }

        Files.createDirectories(output.toAbsolutePath().parent)
        Files.writeString(output, GsonBuilder().setPrettyPrinting().create().toJson(table) + "\n")
        println("Wrote ${CropRegistry.allCrops.size} crops to ${output.toAbsolutePath()}")
    }

    private fun cropJsonOf(crop: CropDefinition): JsonObject = JsonObject().apply {
        addProperty("name", crop.name)
        addProperty("tier", crop.tier.name)
        addProperty("maxStage", crop.maxStage)
        addProperty("isBaseCrop", crop.isBaseCrop)
        addProperty("isMutation", crop.isMutation)
        addProperty("decayTimeMs", crop.decayTimeMs)
        addProperty("needsWater", crop.needsWater)
        add("footprint", JsonObject().apply {
            addProperty("width", crop.footprint.width)
            addProperty("height", crop.footprint.height)
        })
        add("effects", JsonArray().apply { crop.effects.forEach { add(it.name) } })
        add("requiredSoil", JsonArray().apply { crop.requiredSoil.forEach { add(BuiltInRegistries.BLOCK.getKey(it).path) } })
        addProperty("drainsNeighbours", crop.drainsNeighbours)
        addProperty("resetsToFirstStage", crop.resetsToFirstStage)
        add("resetPercentByStage", JsonObject().apply { crop.resetPercentByStage.forEach { (stage, percent) -> addProperty(stage.toString(), percent) } })
        add("sleepStages", JsonArray().apply { crop.sleepStages.sorted().forEach { add(it) } })
        crop.chargeRule?.let { rule ->
            add("chargeRule", JsonObject().apply {
                addProperty("perStage", rule.perStage)
                addProperty("limit", rule.limit)
            })
        }
        addProperty("hasHungerBar", crop.hasHungerBar)
    }
}
