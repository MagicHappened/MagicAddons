package org.magic.magicaddons.features.foraging.safarihelper

import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.animal.parrot.Parrot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.util.PlayerUtils

/**
 * One unique mob of a safari zone.
 *
 * [matches] gets the whole [EntityInfo] because the visual of a mob is often an item display or an
 * armor stand: when the mob has a real entity of its own the visual sits in its information
 * entities, when it does not the visual is the entity itself.
 */
class SafariMob(
    val displayName: String,
    private val matcher: (EntityInfo) -> Boolean
) {
    fun matches(info: EntityInfo): Boolean = matcher(info)
}

/**
 * The safari island is split into four quadrants around x = -47 / z = 0, each with its own set of
 * uniques that have to be caught once each to complete the zone.
 *
 *  haunted: negative z, positive x
 *  icy:     negative z, negative x
 *  cavern:  positive z, negative x
 *  forest:  positive z, positive x
 */
enum class SafariZone(val displayName: String, val uniqueMobs: List<SafariMob>) {
    FOREST(
        "Forest",
        listOf(
            SafariMob("Bluebird") { isParrot(it, Parrot.Variant.BLUE) },
            SafariMob("Fluffling") { isType(it, "panda") },
            SafariMob("Foxtrot") { isType(it, "fox") },
            SafariMob("Hideonfloor") { isType(it, "shulker") },
            SafariMob("Honeybug") { isType(it, "bee") },
            SafariMob("Macaw") { isParrot(it, Parrot.Variant.RED_BLUE) },
            SafariMob("Parakeet") { isParrot(it, Parrot.Variant.GREEN) },
            SafariMob("Treefrog") { isType(it, "frog") },
            SafariMob("Woodchucker") { isType(it, "creaking") }
        )
    ),
    ICE(
        "Ice",
        listOf(
            SafariMob("Billygoat") { isType(it, "goat") },
            SafariMob("Mantis Shrimp") { hasSkull(it, "9924c105aa431dabd47952dc1dddd6f751f883423f4db1487d9bacc2cfe99c7a") },
            SafariMob("Nozzlenose") { isType(it, "dolphin") },
            SafariMob("Polaris") { isType(it, "polar_bear") },
            SafariMob("Shuddersquid") { isType(it, "glow_squid") },
            SafariMob("Strongarm") { isType(it, "snow_golem") },
            SafariMob("Tepid") { isType(it, "tropical_fish") },
            SafariMob("Troodon") { hasSkull(it, "53de4135a3b19a2187029c86a0020e58c907c7bdd4e37b7643f120e16a0aa9ab") },
            SafariMob("Wumpa") { isType(it, "ravager") }
        )
    ),
    HAUNTED(
        "Haunted",
        listOf(
            SafariMob("Areita") { isType(it, "cave_spider") },
            SafariMob("Bloodbat") { isType(it, "bat") },
            SafariMob("Doomspiral") { isType(it, "warden") },
            SafariMob("Duplico") { isBlockDisplay(it) },
            SafariMob("Gazer") { hasSkull(it, "407b3c3d2c3fe259d69207a14ca5cd99713c7096ba122bb40326f3489e5d0d6c") },
            SafariMob("Gimmiegold") { hasSkull(it, "8b329e108ac28b0bec8d47b7cdce253df1db80b46052b5915d963e1bcbab0db4") },
            SafariMob("Hideonwall") { isType(it, "silverfish") },
            SafariMob("Hideyho") { hasPlayerSkin(it, "3504f1f2327a5110e643bb8667082512815fa434a29ed37f4ca83bb16d2db533") },
            SafariMob("Litterbug") { isType(it, "endermite") },
            SafariMob("Solsnatcher") { isType(it, "phantom") }
        )
    ),
    CAVE(
        "Cave",
        listOf(
            // a tropical fish anywhere else in the cave is a different mob that is not identified yet
            SafariMob("Cavernfish") { isType(it, "tropical_fish") && CAVERNFISH_AREA.contains(it.entity.position()) },
            SafariMob("Chuckwalla") { hasSkull(it, "fc63cd0d480971a7beae5fd503e5d51658cd906330843cbad92018f5b98b4fe5") },
            SafariMob("Driftling") { hasSkull(it, "f4c4f8e5fce1ec2d299cb8a395792ecddc497a1d8af86faaa5e20373016c7225") },
            SafariMob("Flitter") { hasSkull(it, "a89a76deedd42b410344100df2fa79b6eeac7e6f287745d656179368340ffade") },
            SafariMob("Gemzie") { isType(it, "vex") },
            // an item display while it is still in stage one, a silverfish once it hatches
            SafariMob("Rockmite") {
                hasSkull(it, "5dbaab74d1acd0abe9d04abe9928725de5d4495fcb63b647228caf6944c20800") || isType(it, "silverfish")
            },
            SafariMob("Scrappy") { isType(it, "armadillo") },
            SafariMob("Shyworm") { isType(it, "slime") },
            SafariMob("Snoozle") { isType(it, "sniffer") }
        )
    );

    /** The unique this entity belongs to, or null when it is not one of the zone's uniques. */
    fun mobMatching(info: EntityInfo): SafariMob? = uniqueMobs.firstOrNull { it.matches(info) }

    companion object {
        fun at(pos: Vec3): SafariZone {
            val isPositiveX = pos.x >= -47.0
            val isPositiveZ = pos.z >= 0.0

            return when {
                !isPositiveZ && isPositiveX -> HAUNTED
                !isPositiveZ && !isPositiveX -> ICE
                isPositiveZ && !isPositiveX -> CAVE
                else -> FOREST
            }
        }
    }
}

/** The only part of the cave where a tropical fish counts as a cavernfish. */
private val CAVERNFISH_AREA = AABB(-105.0, 55.0, 68.0, -75.0, 72.0, 105.0)

private fun isType(info: EntityInfo, path: String): Boolean =
    info.entity.type.toString() == "entity.minecraft.$path"

private fun isParrot(info: EntityInfo, variant: Parrot.Variant): Boolean {
    val entity = info.entity
    return entity is Parrot && entity.variant == variant
}

private fun hasPlayerSkin(info: EntityInfo, hash: String): Boolean {
    val entity = info.entity
    return entity is Player && PlayerUtils.getSkinHash(entity) == hash
}

private fun hasSkull(info: EntityInfo, vararg hashes: String): Boolean =
    visualsOf(info).any { entity ->
        val hash = when (entity) {
            is Display.ItemDisplay -> PlayerUtils.getSkinHash(entity.itemStack)
            is ArmorStand -> PlayerUtils.getSkinHash(entity.getItemBySlot(EquipmentSlot.HEAD))
            else -> null
        }

        hash != null && hash in hashes
    }

/** Mobs that disguise themselves show up as an item display holding a plain full block. */
private fun isBlockDisplay(info: EntityInfo): Boolean =
    visualsOf(info).any { entity ->
        entity is Display.ItemDisplay &&
                (entity.itemStack.item as? BlockItem)?.block?.defaultBlockState()?.canOcclude() == true
    }

private fun visualsOf(info: EntityInfo): List<Entity> =
    listOf(info.entity) + (info.informationEntities ?: emptyList())
