package org.magic.magicaddons.data.greenhouse

import org.magic.magicaddons.data.greenhouse.elements.basecrop.*
import org.magic.magicaddons.data.greenhouse.elements.mutation.common.*
import org.magic.magicaddons.data.greenhouse.elements.mutation.uncommon.*
import org.magic.magicaddons.data.greenhouse.elements.mutation.rare.*
import org.magic.magicaddons.data.greenhouse.elements.mutation.epic.*
import org.magic.magicaddons.data.greenhouse.elements.mutation.legendary.*
import org.magic.magicaddons.data.greenhouse.elements.rarecrop.*

object GreenhouseElementFactory {
    private val factories = mutableMapOf<String,() -> CropDefinitionProvider>()

    init {
        // basecrop
        this.register("BROWN_MUSHROOM") { Brownmushroom() }
        this.register("CACTUS") { Cactus() }
        this.register("CARROT_ITEM") { Carrot() }
        this.register("INK_SACK-3") { Cocoa() }
        this.register("MELON") { Melon() }
        this.register("MOONFLOWER") { Moonflower() }
        this.register("NETHER_STALK") { Netherwart() }
        this.register("POTATO_ITEM") { Potato() }
        this.register("PUMPKIN") { Pumpkin() }
        this.register("RED_MUSHROOM") { Redmushroom() }
        this.register("SUGAR_CANE") { Sugarcane() }
        this.register("DOUBLE_PLANT") { Sunflower() }
        this.register("WHEAT") { Wheat() }
        this.register("WILD_ROSE") { Wildrose() }

        //mutations
        this.register("ASHWREATH") { Ashwreath() }
        this.register("CHOCONUT") { Choconut() }
        this.register("DUSTGRAIN") { Dustgrain() }
        this.register("GLOOMGOURD") { Gloomgourd() }
        this.register("LONELILY") { Lonelily() }
        this.register("SCOURROOT") { Scourroot() }
        this.register("SHADEVINE") { Shadevine() }
        this.register("VEILSHROOM") { Veilshroom() }
        this.register("WITHERBLOOM") { Witherbloom() }

        this.register("CHOCOBERRY") { Chocoberry() }
        this.register("CINDERSHADE") { Cindershade() }
        this.register("COALROOT") { Coalroot() }
        this.register("CREAMBLOOM") { Creambloom() }
        this.register("DUSKBLOOM") { Duskbloom() }
        this.register("THORNSHADE") { Thornshade() }

        this.register("BLASTBERRY") { Blastberry() }
        this.register("CHEESEBITE") { Cheesebite() }
        this.register("CHLORONITE") { Chloronite() }
        this.register("DO_NOT_EAT_SHROOM") { DoNotEatShroom() }
        this.register("FLESHTRAP") { Fleshtrap() }
        this.register("MAGIC_JELLYBEAN") { MagicJellybean() }
        this.register("NOCTILUME") { Noctilume() }
        this.register("SNOOZLING") { Snoozling() }
        this.register("SOGGYBUD") { Soggybud() }

        this.register("CHORUS_FRUIT") { ChorusFruit() }
        this.register("PLANTBOY_ADVANCE") { PlantBoyAdvance() }
        this.register("PUFFERCLOUD") { Puffercloud() }
        this.register("SHELLFRUIT") { Shellfruit() }
        this.register("STARTLEVINE") { Startlevine() }
        this.register("STOPLIGHT_PETAL") { StoplightPetal() }
        this.register("THUNDERLING") { Thunderling() }
        this.register("TURTLELLINI") { Turtlellini() }
        this.register("ZOMBUD") { Zombud() }

        this.register("ALL_IN_ALOE") { AllinAloe() }
        this.register("DEVOURER") { Devourer() }
        this.register("GLASSCORN") { Glasscorn() }
        this.register("GODSEED") { Godseed() }
        this.register("JERRYFLOWER") { Jerryflower() }
        this.register("PHANTOMLEAF") { Phantomleaf() }
        this.register("TIMESTALK") { Timestalk() }

        //rare crops
        this.register("CROPIE") { Cropie() }
        this.register("FERMENTO") { Fermento() }
        this.register("HELIANTHUS") { Helianthus() }
        this.register("SQUASH") { Squash() }
    }

    fun register(id: String, factory: () -> CropDefinitionProvider) {
        factories[id] = factory
    }

    fun create(idOrName: String): CropDefinitionProvider {
        return factories[idOrName]?.invoke()
            ?: error("Unknown greenhouse element id: $idOrName")
    }

    fun getAllFactories(): Collection<() -> CropDefinitionProvider> {
        return factories.values
    }
}