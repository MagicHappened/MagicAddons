package org.magic.magicaddons.features.combat

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.armadillo.Armadillo
import net.minecraft.world.entity.animal.bee.Bee
import net.minecraft.world.entity.animal.dolphin.Dolphin
import net.minecraft.world.entity.animal.goat.Goat
import net.minecraft.world.entity.animal.golem.IronGolem
import net.minecraft.world.entity.animal.turtle.Turtle
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.monster.Vex
import net.minecraft.world.entity.monster.creaking.Creaking
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
import org.magic.magicaddons.util.PlayerUtils
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import kotlin.math.abs
import kotlin.reflect.KClass

object SingleMobs {

    sealed interface ScaleMatch {
        data class Above(val scale: Float) : ScaleMatch
        data class Below(val scale: Float) : ScaleMatch
        data class Exactly(val scale: Float) : ScaleMatch
    }

    sealed interface Rule {
        data class Skull(val hash: String) : Rule
        data class Skin(val hash: String) : Rule
        data class Name(val contains: String) : Rule
        data class Type(val entityClass: KClass<out Entity>, val scaleMatch: ScaleMatch? = null) : Rule
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

    val hypixel: List<Mob> = listOf(
        Mob("Sanger", Rule.Skin("c60812527ebb2d72e6119effd0cce5f1f2966ad45edbb705ed04948aba6f1b74"), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Vanquisher", Rule.Type(WitherBoss::class), SkyBlockIsland.CRIMSON_ISLE),
        Mob("Matcho", Rule.Skin("ef2daabb78a1f7aa12d145d88c0ca46b9e856f5534e9286e555faf0c291f4fd5"), SkyBlockIsland.CRIMSON_ISLE),
        Mob("Ragnarok", Rule.Skin("a8e1fe214b71f6ea69c541a861c64bafda7bf9b85de5dd17ab2b6ccd1d32b039"), SkyBlockIsland.CRIMSON_ISLE),
        Mob("Lord Jawbus", Rule.Type(IronGolem::class), SkyBlockIsland.CRIMSON_ISLE),
        Mob("Rat", Rule.Skull("a8abb471db0ab78703011979dc8b40798a941f3a4dec3ec61cbeec2af8cffe8")),
        Mob("Lotum", Rule.FrogVariant(FrogVariants.TEMPERATE), SkyBlockIsland.LOTUS_ATOLL),
        Mob("Tewtil", Rule.Type(Turtle::class), SkyBlockIsland.LOTUS_ATOLL),
        Mob("Shellwise", Rule.Type(Turtle::class), SkyBlockIsland.GALATEA),
        Mob("Mossybit", Rule.FrogVariant(FrogVariants.COLD), SkyBlockIsland.GALATEA),
        Mob("Joydive", Rule.Type(Dolphin::class), SkyBlockIsland.GALATEA),
        Mob("Littlefoot", Rule.Skin("f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b")),
        Mob("Hideonleaf", Rule.ShulkerColor(DyeColor.GREEN), SkyBlockIsland.GALATEA),
        Mob("Coralot", Rule.AxolotlVariant(Axolotl.Variant.LUCY), SkyBlockIsland.GALATEA),
        Mob("Drybark", Rule.Type(Creaking::class), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Hideonsun", Rule.ShulkerColor(DyeColor.BROWN, DyeColor.YELLOW, DyeColor.ORANGE), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Beeheemoth", Rule.Type(Bee::class, ScaleMatch.Exactly(4f)), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Mountain Goat", Rule.Type(Goat::class), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Blue Jay", Rule.ParrotVariant(Parrot.Variant.BLUE), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Pangolin", Rule.Type(Armadillo::class), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Dustybit", Rule.FrogVariant(FrogVariants.TEMPERATE), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Grizzly Bear", Rule.Skin("5406108aa6bdda73df122454aa4250ec0cd457fd318a893d9d7c54d9c0761168"), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Puck", Rule.Type(Vex::class), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Timil", Rule.TropicalFishVariant(DyeColor.PINK, DyeColor.WHITE), SkyBlockIsland.TORRHUS_CANYON),
        Mob("Trinity", Rule.Skin("5841a16a5bd4a646cedb4b5437723226c7cf9f8669e558773fae0a9452c94d90"), SkyBlockIsland.THE_CATACOMBS),
    )

    val hypixelNames: List<String> = hypixel.map { it.name }

    fun iconFor(mob: Mob): ItemStack? = (mob.rule as? Rule.Skull)?.let { PlayerUtils.getItemFromHash(it.hash) }

    fun hypixelByName(name: String): Mob? = hypixel.firstOrNull { it.name == name }

    fun hypixelTarget(mob: Mob, info: EntityInfo): Entity? {
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
                rule.entityClass.isInstance(it) && matchesScale(it, rule.scaleMatch)
            }

            is Rule.Skull -> EntityUtils.skullCarrier(info, rule.hash)
        }
    }

    private const val SCALE_TOLERANCE: Float = 0.01f

    private fun matchesScale(entity: Entity, scaleMatch: ScaleMatch?): Boolean {
        if (scaleMatch == null) return true
        val scale = (entity as? LivingEntity)?.scale ?: return false

        return when (scaleMatch) {
            is ScaleMatch.Above -> scale > scaleMatch.scale
            is ScaleMatch.Below -> scale < scaleMatch.scale
            is ScaleMatch.Exactly -> abs(scale - scaleMatch.scale) < SCALE_TOLERANCE
        }
    }

    class VanillaMob(val name: String, val type: EntityType<*>) {
        override fun toString(): String = name
    }

    private val LIVING_MISC_IDS: Set<String> = setOf("player", "villager", "wandering_trader", "iron_golem", "snow_golem")

    val vanilla: List<VanillaMob> by lazy {
        BuiltInRegistries.ENTITY_TYPE
            .filter { it.category != MobCategory.MISC || BuiltInRegistries.ENTITY_TYPE.getKey(it).path in LIVING_MISC_IDS }
            .map { VanillaMob(it.description.string, it) }
            .sortedBy { it.name }
    }

    val vanillaNames: List<String> get() = vanilla.map { it.name }

    fun vanillaByName(name: String): VanillaMob? = vanilla.firstOrNull { it.name == name }

    fun vanillaTarget(mob: VanillaMob, info: EntityInfo): Entity? = info.entity.takeIf { it.type === mob.type }
}
