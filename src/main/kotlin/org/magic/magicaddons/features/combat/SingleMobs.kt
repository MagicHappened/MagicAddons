package org.magic.magicaddons.features.combat

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.animal.frog.Frog
import net.minecraft.world.entity.animal.frog.FrogVariants
import net.minecraft.world.entity.animal.frog.FrogVariant as McFrogVariant
import net.minecraft.world.entity.animal.fish.TropicalFish as McTropicalFish
import net.minecraft.world.entity.animal.axolotl.Axolotl
import net.minecraft.world.entity.animal.parrot.Parrot
import net.minecraft.world.entity.monster.Shulker
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.EntityUtils.typeId
import org.magic.magicaddons.util.PlayerUtils
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

object SingleMobs {


    sealed interface Rule {
        data class Skull(val hash: String) : Rule
        data class Skin(val hash: String) : Rule
        data class Name(val contains: String) : Rule
        data class Type(val path: String, val minScale: Float = 0f) : Rule
        data class ParrotVariant(val variant: Parrot.Variant) : Rule
        data class AxolotlVariant(val variant: Axolotl.Variant) : Rule
        data class FrogVariant(val variant: ResourceKey<McFrogVariant>) : Rule
        data class TropicalFishVariant(
            val baseColor: DyeColor,
            val patternColor: DyeColor,
            val pattern: McTropicalFish.Pattern? = null
        ) : Rule
        data class ShulkerColor(val colors: Set<DyeColor>) : Rule {
            constructor(vararg colors: DyeColor) : this(colors.toSet())
        }
    }


    data class Mob(val name: String, val rule: Rule, val island: SkyBlockIsland? = null) {
        override fun toString(): String = name
    }

    val all: List<Mob> = listOf(
        Mob("Vanquisher", Rule.Type("wither"), SkyBlockIsland.CRIMSON_ISLE),
        Mob("Matcho", Rule.Skin("ef2daabb78a1f7aa12d145d88c0ca46b9e856f5534e9286e555faf0c291f4fd5"), SkyBlockIsland.CRIMSON_ISLE),
        Mob("Rat", Rule.Skull("a8abb471db0ab78703011979dc8b40798a941f3a4dec3ec61cbeec2af8cffe8")),
        Mob("Lotum", Rule.FrogVariant(FrogVariants.TEMPERATE), SkyBlockIsland.LOTUS_ATOLL),
        Mob("Tewtil", Rule.Type("turtle"), SkyBlockIsland.LOTUS_ATOLL),
        Mob("Shellwise", Rule.Type("turtle"), SkyBlockIsland.GALATEA),
        Mob("Mossybit", Rule.FrogVariant(FrogVariants.COLD), SkyBlockIsland.GALATEA),
        Mob("Joydive", Rule.Type("dolphin"), SkyBlockIsland.GALATEA),
        Mob("Littlefoot", Rule.Skin("f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b")),
        Mob("Hideonleaf", Rule.ShulkerColor(DyeColor.GREEN), SkyBlockIsland.GALATEA),
        Mob("Coralot", Rule.AxolotlVariant(Axolotl.Variant.LUCY), SkyBlockIsland.GALATEA),
        Mob("Hideonsun", Rule.ShulkerColor(DyeColor.BROWN, DyeColor.YELLOW, DyeColor.ORANGE), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Beeheemoth", Rule.Type("bee", minScale = 4f), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Mountain Goat", Rule.Type("goat"), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Blue Jay", Rule.ParrotVariant(Parrot.Variant.BLUE), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Pangolin", Rule.Type("armadillo"), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Dustybit", Rule.FrogVariant(FrogVariants.TEMPERATE), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Grizzly Bear", Rule.Skin("5406108aa6bdda73df122454aa4250ec0cd457fd318a893d9d7c54d9c0761168"), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Puck", Rule.Type("vex"), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Timil", Rule.TropicalFishVariant(DyeColor.PINK, DyeColor.WHITE), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Trinity", Rule.Skin("5841a16a5bd4a646cedb4b5437723226c7cf9f8669e558773fae0a9452c94d90"), SkyBlockIsland.THE_CATACOMBS),
    )

    val names: List<String> = all.map { it.name }

    /**
     * The item a marker draws for this mob, or null to draw the mob itself. Only a skull mob has
     * one: the skull is the part the player sees, while the entity wearing it is invisible.
     */
    fun iconFor(mob: Mob): ItemStack? = (mob.rule as? Rule.Skull)?.let { PlayerUtils.getItemFromHash(it.hash) }

    fun byName(name: String): Mob? = all.firstOrNull { it.name == name }

    /**
     * The entity to outline if this mob is what the info describes, or null. A skull found on
     * something standing in the mob is the visible part, so that is what comes back.
     */
    fun target(mob: Mob, info: EntityInfo): Entity? {
        if (mob.island != null && LocationAPI.island != mob.island) return null

        val entity = info.entity

        return when (val rule = mob.rule) {
            is Rule.ParrotVariant -> entity.takeIf { it is Parrot && it.variant == rule.variant }

            is Rule.AxolotlVariant -> entity.takeIf { it is Axolotl && it.variant == rule.variant }

            is Rule.FrogVariant -> entity.takeIf { it is Frog && it.variant.`is`(rule.variant) }

            is Rule.TropicalFishVariant -> entity.takeIf {
                it is McTropicalFish &&
                        it.baseColor == rule.baseColor &&
                        it.patternColor == rule.patternColor &&
                        (rule.pattern == null || it.pattern == rule.pattern)
            }

            is Rule.ShulkerColor -> entity.takeIf { it is Shulker && it.color in rule.colors }

            is Rule.Skin -> entity.takeIf { it is Player && PlayerUtils.getSkinHash(it) == rule.hash }

            is Rule.Name -> entity.takeIf {
                it.customName?.string?.contains(rule.contains, ignoreCase = true) == true ||
                        info.informationEntities?.any { tag ->
                            tag.customName?.string?.contains(rule.contains, ignoreCase = true) == true
                        } == true
            }

            is Rule.Type -> entity.takeIf {
                it.typeId().contains(rule.path) && ((it as? LivingEntity)?.scale ?: 1f) >= rule.minScale
            }

            is Rule.Skull -> EntityUtils.skullCarrier(info, rule.hash)
        }
    }
}
