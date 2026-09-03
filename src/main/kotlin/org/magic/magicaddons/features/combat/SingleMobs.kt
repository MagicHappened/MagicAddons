package org.magic.magicaddons.features.combat

import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.util.PlayerUtils

/**
 * The mobs a player can pick by name instead of by hash. Each carries the rule that finds it, in
 * the same terms the advanced filters use, so adding one is one line here.
 */
object SingleMobs {

    /** How a mob is recognised: by the skull it or something beside it wears, its skin, its name, or its type. */
    sealed interface Rule {
        data class Skull(val hash: String) : Rule
        data class Skin(val hash: String) : Rule
        data class Name(val contains: String) : Rule
        data class Type(val path: String) : Rule
    }

    data class Mob(val name: String, val rule: Rule) {
        override fun toString(): String = name
    }

    val all: List<Mob> = listOf(
        Mob("Rat", Rule.Skull("a8abb471db0ab78703011979dc8b40798a941f3a4dec3ec61cbeec2af8cffe8")),
        Mob("Littlefoot", Rule.Skin("f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b")),
    )

    val names: List<String> = all.map { it.name }

    fun byName(name: String): Mob? = all.firstOrNull { it.name == name }

    /**
     * The entity to outline if this mob is what the info describes, or null. A skull found on
     * something standing in the mob is the visible part, so that is what comes back.
     */
    fun target(mob: Mob, info: EntityInfo): Entity? {
        val entity = info.entity

        return when (val rule = mob.rule) {
            is Rule.Skin -> entity.takeIf { it is Player && PlayerUtils.getSkinHash(it) == rule.hash }

            is Rule.Name -> entity.takeIf {
                it.customName?.string?.contains(rule.contains, ignoreCase = true) == true ||
                        info.informationEntities?.any { tag ->
                            tag.customName?.string?.contains(rule.contains, ignoreCase = true) == true
                        } == true
            }

            is Rule.Type -> entity.takeIf { it.type.toString().contains(rule.path) }

            is Rule.Skull -> skullCarrier(info, rule.hash)
        }
    }

    /** Whoever wears the skull: the mob itself, or otherwise the stand or display beside it. */
    private fun skullCarrier(info: EntityInfo, hash: String): Entity? {
        val entity = info.entity

        if (entity is LivingEntity && PlayerUtils.getSkinHash(entity.getItemBySlot(EquipmentSlot.HEAD)) == hash) {
            return entity
        }

        val carrier = info.informationEntities?.firstOrNull { other ->
            val stack = when (other) {
                is ArmorStand -> other.getItemBySlot(EquipmentSlot.HEAD)
                is Display.ItemDisplay -> other.itemStack
                else -> ItemStack.EMPTY
            }

            !stack.isEmpty && PlayerUtils.getSkinHash(stack) == hash
        } ?: return null

        // the mob is what matched, but an invisible one is drawn by whatever carries its skull
        return if (entity.isInvisible) carrier else entity
    }
}
