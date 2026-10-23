package org.magic.magicaddons.features.farming.fortune

import net.minecraft.world.item.ItemStack
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.SystemChatEvent
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.screen.ContainerInitializedEvent
import tech.thatgravyboat.skyblockapi.api.profile.PetsAPI
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore

object ActivePet {

    class Pet(val name: String, val level: Int, val rarity: SkyBlockRarity?)

    private val PETS_TITLE: Regex = Regex("""^(?:\(\d+/\d+\) )?Pets$""")
    private val PET_NAME: Regex = Regex("""\[Lvl (\d+)] (.+)""")
    private const val ACTIVE_PET_LINE: String = "Click to despawn!"
    private val AUTOPET_MESSAGE: Regex = Regex("""Autopet equipped your \[Lvl (\d+)] (.+?)! VIEW RULE""")
    private val SUMMON_MESSAGE: Regex = Regex("""^You summoned your (.+?)!$""")
    private val DESPAWN_MESSAGE: Regex = Regex("""^You despawned your (.+?)!$""")

    private val knownPets: MutableMap<String, Pet> = mutableMapOf()
    private var tracked: Pet? = null

    val current: Pet?
        get() = PetsAPI.pet?.takeIf { it.isNotBlank() }?.let { Pet(it, PetsAPI.level, PetsAPI.rarity) } ?: tracked

    @Subscription
    fun onContainer(event: ContainerInitializedEvent) {
        if (!PETS_TITLE.matches(event.title)) return
        event.containerItems.forEach { item ->
            val pet = petOf(item) ?: return@forEach
            knownPets[pet.name] = pet
            if (item.getLore().any { it.string.contains(ACTIVE_PET_LINE) }) tracked = pet
        }
    }

    private fun petOf(item: ItemStack): Pet? {
        val match = PET_NAME.find(item.hoverName.string) ?: return null
        val name = match.groupValues[2].trim()
        return Pet(name, match.groupValues[1].toInt(), item.getData(DataTypes.RARITY))
    }

    @EventHandler
    fun onChat(event: SystemChatEvent) {
        val text = event.text.trim()
        AUTOPET_MESSAGE.find(text)?.let { match ->
            val name = match.groupValues[2].trim()
            tracked = Pet(name, match.groupValues[1].toInt(), knownPets[name]?.rarity)
            return
        }
        SUMMON_MESSAGE.find(text)?.let { match ->
            val name = match.groupValues[1].trim()
            tracked = knownPets[name] ?: Pet(name, 0, null)
            return
        }
        if (DESPAWN_MESSAGE.containsMatchIn(text)) tracked = null
    }
}
