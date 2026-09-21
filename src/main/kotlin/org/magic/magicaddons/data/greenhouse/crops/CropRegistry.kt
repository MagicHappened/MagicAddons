package org.magic.magicaddons.data.greenhouse.crops

import net.minecraft.world.level.block.Block
import org.magic.magicaddons.data.greenhouse.crops.definitions.basecrops.*
import org.magic.magicaddons.data.greenhouse.crops.definitions.misc.DeadPlant
import org.magic.magicaddons.data.greenhouse.crops.definitions.misc.DevourerRoots
import org.magic.magicaddons.data.greenhouse.crops.definitions.misc.FireElement
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.common.*
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.epic.*
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.legendary.*
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.rare.*
import org.magic.magicaddons.data.greenhouse.crops.definitions.mutations.uncommon.*
import org.magic.magicaddons.data.greenhouse.crops.definitions.rarecrops.*

object CropRegistry {
    private val crops = mutableListOf<CropDefinition>()

    val all: List<CropDefinition> get() = crops

    private fun register(crop: CropDefinition) {
        crops.add(crop)
    }

    // built once, since lookups happen on every block update
    private val cropsByIdOrName: Map<String, CropDefinition> by lazy {
        buildMap {
            all.forEach { crop ->
                crop.skyblockId?.id?.let { putIfAbsent(it, crop) }
                crop.aliases?.forEach { putIfAbsent(it.id, crop) }
                putIfAbsent(crop.name, crop)
            }
        }
    }

    fun findByIdOrName(idOrName: String): CropDefinition? = cropsByIdOrName[idOrName]

    fun findByIdOrNameIgnoringCase(name: String): CropDefinition? =
        findByIdOrName(name) ?: all.find { it.name.equals(name, ignoreCase = true) }

    /** "do_not_eat_shroom" and "Do-not-eat-shroom" give the same key */
    private fun looseNameKey(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

    private val cropsByLooseName: Map<String, CropDefinition> by lazy {
        buildMap {
            all.forEach { crop ->
                putIfAbsent(looseNameKey(crop.name), crop)
                crop.skyblockId?.id?.substringAfter(':')?.let { putIfAbsent(looseNameKey(it), crop) }
            }
        }
    }

    fun findByLooseName(text: String): CropDefinition? = cropsByLooseName[looseNameKey(text)]

    val cropsBySoil: Map<Block, List<CropDefinition>> by lazy {
        all.flatMap { crop -> crop.requiredSoil.map { soil -> soil to crop } }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
    }

    init {
        registerEveryCrop()
    }

    private fun registerEveryCrop() {
        register(FireElement.definition)
        register(DeadPlant.definition)
        register(DevourerRoots.definition)

        register(Brownmushroom.definition)
        register(Cactus.definition)
        register(Carrot.definition)
        register(Cocoa.definition)
        register(Melon.definition)
        register(Moonflower.definition)
        register(Netherwart.definition)
        register(Potato.definition)
        register(Pumpkin.definition)
        register(Redmushroom.definition)
        register(Sugarcane.definition)
        register(Sunflower.definition)
        register(Wheat.definition)
        register(Wildrose.definition)

        // mutation - common
        register(Ashwreath.definition)
        register(Choconut.definition)
        register(Dustgrain.definition)
        register(Gloomgourd.definition)
        register(Lonelily.definition)
        register(Scourroot.definition)
        register(Shadevine.definition)
        register(Veilshroom.definition)
        register(Witherbloom.definition)

        // mutation - uncommon
        register(Chocoberry.definition)
        register(Cindershade.definition)
        register(Coalroot.definition)
        register(Creambloom.definition)
        register(Duskbloom.definition)
        register(Thornshade.definition)

        // mutation - rare
        register(Blastberry.definition)
        register(Cheesebite.definition)
        register(Chloronite.definition)
        register(DoNotEatShroom.definition)
        register(Fleshtrap.definition)
        register(MagicJellybean.definition)
        register(Noctilume.definition)
        register(Snoozling.definition)
        register(Soggybud.definition)

        // mutation - epic
        register(ChorusFruit.definition)
        register(PlantBoyAdvance.definition)
        register(Puffercloud.definition)
        register(Shellfruit.definition)
        register(Startlevine.definition)
        register(StoplightPetal.definition)
        register(Thunderling.definition)
        register(Turtlellini.definition)
        register(Zombud.definition)

        // mutation - legendary
        register(AllinAloe.definition)
        register(Devourer.definition)
        register(Glasscorn.definition)
        register(Godseed.definition)
        register(Jerryflower.definition)
        register(Phantomleaf.definition)
        register(Timestalk.definition)

        // rare crops
        register(Cropie.definition)
        register(Fermento.definition)
        register(Helianthus.definition)
        register(Squash.definition)
    }
}
