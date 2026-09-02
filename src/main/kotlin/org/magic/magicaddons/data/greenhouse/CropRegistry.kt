package org.magic.magicaddons.data.greenhouse

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
    val all: MutableList<CropDefinition> = mutableListOf()

    /** Where each crop sits in the dex ordering, taken from the package its provider lives in. */
    val tierOf: MutableMap<CropDefinition, Int> = mutableMapOf()

    private fun register(provider: CropDefinitionProvider) {
        all.add(provider.definition)
        tierOf[provider.definition] = tierFromPackage(provider.javaClass.name)
    }

    private fun tierFromPackage(name: String): Int = when {
        ".basecrop." in name -> 0
        ".mutation.common." in name -> 1
        ".mutation.uncommon." in name -> 2
        ".mutation.rare." in name -> 3
        ".mutation.epic." in name -> 4
        ".mutation.legendary." in name -> 5
        ".rarecrop." in name -> 6
        else -> 7
    }

    /** Every name a definition answers to, built once: lookups happen on every block update. */
    private val byKey: Map<String, CropDefinition> by lazy {
        buildMap {
            all.forEach { definition ->
                definition.skyblockId?.id?.let { putIfAbsent(it, definition) }
                definition.aliases?.forEach { putIfAbsent(it.id, definition) }
                putIfAbsent(definition.name, definition)
            }
        }
    }

    fun get(idOrName: String): CropDefinition? = byKey[idOrName]


    init {
        loadCrops()
    }

    private fun loadCrops(){
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