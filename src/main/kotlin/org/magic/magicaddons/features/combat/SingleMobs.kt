package org.magic.magicaddons.features.combat

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
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

/**
 * The mobs a player can pick by name instead of by hash. Each carries the rule that finds it, in
 * the same terms the advanced filters use, so adding one is one line here.
 */
object SingleMobs {

    /**
     * How a mob is recognised: by the skull it or something beside it wears, its skin, its name, its
     * type, or the dye a shulker is painted in.
     */
    sealed interface Rule {
        data class Skull(val hash: String) : Rule
        data class Skin(val hash: String) : Rule
        data class Name(val contains: String) : Rule
        /** [minScale] separates a mob the server blew up from the ordinary one of its type. */
        data class Type(val path: String, val minScale: Float = 0f) : Rule
        data class ParrotVariant(val variant: Parrot.Variant) : Rule
        data class ShulkerColor(val colors: Set<DyeColor>) : Rule {
            constructor(vararg colors: DyeColor) : this(colors.toSet())
        }
    }

    /** [island] is the only island the mob lives on, or null when it can turn up anywhere. */
    data class Mob(val name: String, val rule: Rule, val island: SkyBlockIsland? = null) {
        override fun toString(): String = name
    }

    val all: List<Mob> = listOf(
        Mob("Rat", Rule.Skull("a8abb471db0ab78703011979dc8b40798a941f3a4dec3ec61cbeec2af8cffe8")),
        Mob("Littlefoot", Rule.Skin("f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b")),
        Mob("Hideonleaf", Rule.ShulkerColor(DyeColor.GREEN), SkyBlockIsland.GALATEA),
        Mob("Hideonsun", Rule.ShulkerColor(DyeColor.BROWN, DyeColor.YELLOW), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Beeheemoth", Rule.Type("bee", minScale = 4f), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Mountain Goat", Rule.Type("goat"), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Blue Jay", Rule.ParrotVariant(Parrot.Variant.BLUE), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Pangolin", Rule.Type("armadillo"), SkyBlockIsland.TORRHUS_CANYON),
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
