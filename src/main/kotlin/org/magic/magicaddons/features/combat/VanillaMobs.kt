package org.magic.magicaddons.features.combat

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import org.magic.magicaddons.data.EntityInfo

object VanillaMobs {

    class Mob(val name: String, val type: EntityType<*>) {
        override fun toString(): String = name
    }

    private val LIVING_MISC_IDS: Set<String> = setOf("player", "villager", "wandering_trader", "iron_golem", "snow_golem")

    // read once the registry is full, which is before any screen can ask
    val all: List<Mob> by lazy {
        BuiltInRegistries.ENTITY_TYPE
            .filter { it.category != MobCategory.MISC || BuiltInRegistries.ENTITY_TYPE.getKey(it)?.path in LIVING_MISC_IDS }
            .map { Mob(it.description.string, it) }
            .sortedBy { it.name }
    }

    val names: List<String> get() = all.map { it.name }

    fun byName(name: String): Mob? = all.firstOrNull { it.name == name }

    fun target(mob: Mob, info: EntityInfo): Entity? = info.entity.takeIf { it.type === mob.type }
}
