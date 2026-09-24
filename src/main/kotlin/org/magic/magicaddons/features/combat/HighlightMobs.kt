package org.magic.magicaddons.features.combat

import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.TextSetting
import org.magic.magicaddons.data.config.ToggleListSetting
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.SystemChatEvent
import org.magic.magicaddons.events.interact.InteractEntityEvent
import org.magic.magicaddons.events.world.EntityAddedEvent
import org.magic.magicaddons.events.world.EntityRemovedEvent
import org.magic.magicaddons.events.world.EntityUpdatedEvent
import org.magic.magicaddons.features.HighlightFeature
import org.magic.magicaddons.features.misc.HighlightMarkers
import org.magic.magicaddons.util.EntityUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.location.IslandChangeEvent
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland


object HighlightMobs : HighlightFeature() {
    override val highlightPriority: Int = 0

    /** A corpse is outlined in the colour of its own armour; everything else is outlined white. */
    override fun highlightColor(entity: Entity): Int =
        corpseColor(entity) ?: 0xFFFFFFFF.toInt()

    init {
        EventBus.register(this)
        SkyBlockAPI.eventBus.register(this)
    }

    override val id: String = "HighlightMobs"
    override val displayName: String = "Mob Highlight"
    override val description: String = "§fHighlights mobs of your choosing.\n" +
            "§fPresets, single mobs, or a name."
    override val category: String = "combat"

    val hypixelMobsList = ToggleListSetting(
        key = "HypixelMobs",
        displayName = "Hypixel Mobs",
        description = "",
        value = mutableListOf(),
        choices = { SingleMobs.hypixelNames }
    )

    val vanillaMobsList = ToggleListSetting(
        key = "VanillaMobs",
        displayName = "Vanilla Mobs",
        description = "",
        value = mutableListOf(),
        choices = { SingleMobs.vanillaNames }
    )

    private val throughWallsSetting = BooleanSetting(
        key = "ThroughWalls",
        displayName = "Through Walls",
        description = "§cShows the mobs through walls",
        value = false,
        needsExtensionPack = true,
        children = listOf(HighlightMarkers.linkSetting())
    )

    override val throughWalls: Boolean get() = throughWallsSetting.value

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false,
        children = listOf(
            throughWallsSetting,
            BooleanSetting(
                key = "PresetsEnabled",
                displayName = "Mob Presets",
                description = "§fPreselect highlight options for different areas of the game.",
                value = false,
                children = listOf(
                    BooleanSetting(
                        key = "PresetsForagingTreasure",
                        displayName = "Foraging Treasure",
                        description = "§fHighlights the grass hiding treasure or shards\n§fon the foraging islands.",
                        value = false
                    ),
                    BooleanSetting(
                        key = "PresetsShaftCorpses",
                        displayName = "Shaft Corpses",
                        // each corpse named in the colour it is outlined in, as near as chat colours get
                        description = "§fHighlights the §9lapis§f, §6umber§f and §btungsten§f corpses in mineshafts.\n" +
                                "§fEach is outlined in its own colour.",
                        value = false,
                        children = listOf(
                            BooleanSetting(
                                key = "HideLootedCorpses",
                                displayName = "Hide Looted",
                                description = "§fStops highlighting a corpse once you have looted it.",
                                value = false
                            )
                        )
                    )
                )
            ),
            BooleanSetting(
                key = "SingleMobsEnabled",
                displayName = "Single Mobs",
                description = "§fHighlight specific mobs.",
                value = false,
                children = listOf(
                    BooleanSetting(
                        key = "HypixelMobsEnabled",
                        displayName = "Hypixel Mobs",
                        description = "§fMobs specifically for hypixel\n" +
                                "§bIf a mob you want isn't added here, suggest it to a dev for implementation.",
                        value = false,
                        children = listOf(hypixelMobsList)
                    ),
                    BooleanSetting(
                        key = "VanillaMobsEnabled",
                        displayName = "Vanilla Mobs",
                        description = "§fEvery kind of living thing in the game itself, players included.",
                        value = false,
                        children = listOf(vanillaMobsList)
                    )
                )
            ),
            BooleanSetting(
                key = "MobInfoEnabled",
                displayName = "Mob Name",
                description = "§fHighlights mobs whose name contains this text.\n" +
                        "\n" +
                        "§cNames usually sit on a separate armor stand above the mob,\n" +
                        "§cso the highlight is often shorter range than with the\n" +
                        "§cother highlight options.",
                value = false,
                children = listOf(
                    TextSetting(
                        key = "MobInfoContains",
                        displayName = "Mob Name Contains",
                        description = "§fThe text to look for in a mob's name.",
                        value = "Littlefoot"
                    )
                )
            )
        )
    )

    @EventHandler
    fun onConfigChanged(event: ConfigChangedEvent) {
        invalidateHighlights()
    }

    @EventHandler
    fun onEntityAdded(event: EntityAddedEvent) {
        handleEntitiesAdded(event.addedEntityList)
    }

    @EventHandler
    fun onEntityRemoved(event: EntityRemovedEvent) {
        handleEntitiesRemoved(event.removedEntityList)
    }

    @EventHandler
    fun onEntityUpdated(event: EntityUpdatedEvent) {
        handleEntitiesUpdated(event.updatedEntityList)
    }

    private val CORPSE_COLORS: Set<Int> = setOf(0x0000FF, 0xC83200, 0xCCE5FF)

    private fun corpseColor(entity: Entity): Int? {
        if (entity !is LivingEntity) return null

        val dyedItems = entity.getItemBySlot(EquipmentSlot.CHEST)
            .get(DataComponents.DYED_COLOR)
            ?.rgb
            ?: return null

        val rgb = dyedItems and 0xFFFFFF

        return if (rgb in CORPSE_COLORS) 0xFF000000.toInt() or rgb else null
    }

    private val lootedCorpses: MutableSet<Int> = mutableSetOf()

    private var pendingCorpse: Entity? = null
    private var pendingSince: Long = 0
    private const val LOOT_WINDOW_MS: Long = 3000

    private val CORPSE_LOOT_MESSAGE = Regex("\\s*\\w+ CORPSE LOOT!\\s*")

    @EventHandler
    fun onInteractEntity(event: InteractEntityEvent) {
        if (!hideLootedEnabled()) return
        if (corpseColor(event.target) == null) return

        pendingCorpse = event.target
        pendingSince = System.currentTimeMillis()
    }

    @EventHandler
    fun onSystemChat(event: SystemChatEvent) {
        if (event.overlay) return
        if (!CORPSE_LOOT_MESSAGE.matches(event.text)) return

        val corpse = pendingCorpse ?: return
        pendingCorpse = null

        if (System.currentTimeMillis() - pendingSince > LOOT_WINDOW_MS) return

        lootedCorpses.add(corpse.id)
        invalidateHighlights()
    }

    @Subscription
    fun onIslandChange(event: IslandChangeEvent) {
        lootedCorpses.clear()
        pendingCorpse = null
    }

    private fun hideLootedEnabled(): Boolean =
        baseSetting.getChild<BooleanSetting>("PresetsEnabled")
            ?.getChild<BooleanSetting>("PresetsShaftCorpses")
            ?.getChild<BooleanSetting>("HideLootedCorpses")
            ?.value == true


    private fun presetTarget(info: EntityInfo): Entity? {
        val presets = baseSetting.getChild<BooleanSetting>("PresetsEnabled") ?: return null
        if (!presets.value) return null

        if (presets.getChild<BooleanSetting>("PresetsForagingTreasure")?.value == true) {
            if (info.entity is Display.ItemDisplay && info.entity.itemStack.item == Items.STRING) {
                return info.entity
            }
        }

        if (presets.getChild<BooleanSetting>("PresetsShaftCorpses")?.value == true &&
            LocationAPI.island == SkyBlockIsland.MINESHAFT &&
            corpseColor(info.entity) != null &&
            info.entity.id !in lootedCorpses
        ) {
            return info.entity
        }

        return null
    }

    private class SingleMobsMatch(val name: String, val icon: ItemStack?, val target: Entity)

    private fun singleMobTarget(info: EntityInfo): Entity? = singleMobMatch(info)?.target

    private fun singleMobMatch(info: EntityInfo): SingleMobsMatch? {
        val singleMobs = baseSetting.getChild<BooleanSetting>("SingleMobsEnabled") ?: return null
        if (!singleMobs.value) return null

        return hypixelMobMatch(singleMobs, info) ?: vanillaMobMatch(singleMobs, info)
    }

    private fun hypixelMobMatch(singleMobs: BooleanSetting, info: EntityInfo): SingleMobsMatch? {
        if (singleMobs.getChild<BooleanSetting>("HypixelMobsEnabled")?.value != true) return null

        return hypixelMobsList.value
            .asSequence()
            .filter { it.enabled }
            .mapNotNull { SingleMobs.hypixelByName(it.value) }
            .mapNotNull { mob -> SingleMobs.hypixelTarget(mob, info)?.let { SingleMobsMatch(mob.name, SingleMobs.iconFor(mob), it) } }
            .firstOrNull()
    }

    private fun vanillaMobMatch(singleMobs: BooleanSetting, info: EntityInfo): SingleMobsMatch? {
        if (singleMobs.getChild<BooleanSetting>("VanillaMobsEnabled")?.value != true) return null

        return vanillaMobsList.value
            .asSequence()
            .filter { it.enabled }
            .mapNotNull { SingleMobs.vanillaByName(it.value) }
            .mapNotNull { mob -> SingleMobs.vanillaTarget(mob, info)?.let { SingleMobsMatch(mob.name, null, it) } }
            .firstOrNull()
    }

    // for names above the mobs
    private fun nameTarget(info: EntityInfo): Entity? {
        val nameSetting = baseSetting.getChild<BooleanSetting>("MobInfoEnabled") ?: return null
        if (!nameSetting.value) return null

        val filter = nameSetting.getChild<TextSetting>("MobInfoContains")?.value ?: return null
        if (filter.isBlank()) return null

        val entity = info.entity
        val matches = entity.customName?.string?.contains(filter, true) == true ||
                info.informationEntities?.any {
                    it.customName?.string?.contains(filter, true) == true
                } == true

        return entity.takeIf { matches }
    }

    override fun highlightTarget(info: EntityInfo): Entity? {
        if (!baseSetting.value) return null

        return presetTarget(info)
            ?: singleMobTarget(info)
            ?: nameTarget(info)
    }

    override fun markOf(info: EntityInfo): EntityUtils.HighlightMark? {
        val picked = singleMobMatch(info) ?: return null

        return EntityUtils.HighlightMark(picked.name, picked.icon)
    }
}
