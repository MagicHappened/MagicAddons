package org.magic.magicaddons.data.greenhouse

import net.minecraft.world.level.block.Block
import org.magic.magicaddons.data.greenhouse.elements.DeadPlant
import org.magic.magicaddons.data.greenhouse.elements.DevourerRoots
import org.magic.magicaddons.data.greenhouse.elements.FireElement
import org.magic.magicaddons.data.greenhouse.elements.basecrop.*
import org.magic.magicaddons.data.greenhouse.elements.mutation.common.*
import org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon.*
import org.magic.magicaddons.data.greenhouse.elements.mutation.rare.*
import org.magic.magicaddons.data.greenhouse.elements.mutation.epic.*
import org.magic.magicaddons.data.greenhouse.elements.mutation.legendary.*
import org.magic.magicaddons.data.greenhouse.elements.rarecrop.*

object CropRegistry {
    private val crops = mutableListOf<CropDefinition>()
    private val tierByCrop = mutableMapOf<CropDefinition, CropTier>()

    val all: List<CropDefinition> get() = crops

    fun tierOf(crop: CropDefinition): CropTier = tierByCrop[crop] ?: CropTier.Other

    private fun register(provider: CropDefinitionProvider) {
        crops.add(provider.definition)
        tierByCrop[provider.definition] = tierFromPackage(provider.javaClass.name)
    }

    private fun tierFromPackage(className: String): CropTier = when {
        ".basecrop." in className -> CropTier.BaseCrop
        ".mutation.common." in className -> CropTier.Common
        ".mutation.uncommon." in className -> CropTier.Uncommon
        ".mutation.rare." in className -> CropTier.Rare
        ".mutation.epic." in className -> CropTier.Epic
        ".mutation.legendary." in className -> CropTier.Legendary
        ".rarecrop." in className -> CropTier.RareCrop
        else -> CropTier.Other
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
        register(FireElement)
        register(DeadPlant)
        register(DevourerRoots)

        register(Brownmushroom)
        register(Cactus)
        register(Carrot)
        register(Cocoa)
        register(Melon)
        register(Moonflower)
        register(Netherwart)
        register(Potato)
        register(Pumpkin)
        register(Redmushroom)
        register(Sugarcane)
        register(Sunflower)
        register(Wheat)
        register(Wildrose)

        // mutation - common
        register(Ashwreath)
        register(Choconut)
        register(Dustgrain)
        register(Gloomgourd)
        register(Lonelily)
        register(Scourroot)
        register(Shadevine)
        register(Veilshroom)
        register(Witherbloom)

        // mutation - uncommon
        register(Chocoberry)
        register(Cindershade)
        register(Coalroot)
        register(Creambloom)
        register(Duskbloom)
        register(Thornshade)

        // mutation - rare
        register(Blastberry)
        register(Cheesebite)
        register(Chloronite)
        register(DoNotEatShroom)
        register(Fleshtrap)
        register(MagicJellybean)
        register(Noctilume)
        register(Snoozling)
        register(Soggybud)

        // mutation - epic
        register(ChorusFruit)
        register(PlantBoyAdvance)
        register(Puffercloud)
        register(Shellfruit)
        register(Startlevine)
        register(StoplightPetal)
        register(Thunderling)
        register(Turtlellini)
        register(Zombud)

        // mutation - legendary
        register(AllinAloe)
        register(Devourer)
        register(Glasscorn)
        register(Godseed)
        register(Jerryflower)
        register(Phantomleaf)
        register(Timestalk)

        // rare crops
        register(Cropie)
        register(Fermento)
        register(Helianthus)
        register(Squash)
    }
}
