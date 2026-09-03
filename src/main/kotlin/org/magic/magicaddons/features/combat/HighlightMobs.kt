package org.magic.magicaddons.features.combat

import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.data.ListEntry
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.EnumSetting
import org.magic.magicaddons.data.config.TextSetting
import org.magic.magicaddons.data.config.ToggleListSetting
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.events.world.OnEntityRemoved
import org.magic.magicaddons.events.world.OnEntityUpdated
import org.magic.magicaddons.features.HighlightFeature
import org.magic.magicaddons.util.PlayerUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
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

    enum class EntityTypeDetection {
        Player,
        Other
    }

    override val id: String = "HighlightMobs"
    override val displayName: String = "Mob Highlight"
    override val tooltipMessage: String = "Highlights specific mobs of your choosing"
    override val category: String = "combat"

    val entityTypePlayerSkinHashList = ToggleListSetting(
        key = "EntityTypePlayerSkinHash",
        displayName = "Skin Hash Value",
        tooltip = "The skin hash value to detect (get with mob hit debug)",
        value = mutableListOf(
            ListEntry(name = "Littlefoot", "f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b", enabled = true)
        )
    )

    val entityTypeMobPathValue = TextSetting(
        key = "EntityTypeMobPathValue",
        displayName = "Mob Path",
        tooltip = "The mob path value to detect (get with mob hit debug)",
        value = "entity.minecraft.pig"
    )

    override val baseSetting: BooleanSetting = BooleanSetting(
        key = "enabled",
        displayName = displayName,
        tooltip = tooltipMessage,
        value = false,
        children = listOf(
            BooleanSetting(
                key = "PresetsEnabled",
                displayName = "Mob Presets",
                tooltip = "Preselected Mobs to add to the highlight list.",
                value = false,
                children = listOf(
                    BooleanSetting(
                        key = "PresetsForagingTreasure",
                        displayName = "Foraging Treasure",
                        tooltip = "Preset to highlight the grass containing treasure (or shards) inside foraging islands",
                        value = false
                    ),
                    BooleanSetting(
                        key = "PresetsShaftCorpses",
                        displayName = "Shaft Corpses",
                        tooltip = "Preset to highlight the lapis, umber and tungsten corpses inside " +
                                "mineshafts, each outlined in its own colour",
                        value = false
                    )
                )
            ),
            BooleanSetting(
                key = "EntityTypeEnabled",
                displayName = "Entity Type",
                tooltip = "If to use an entity type based filtering for mob highlighting",
                value = false,
                children = listOf(
                    BooleanSetting(
                        key = "EntityTypePlayerEnabled",
                        displayName = "Player Entity",
                        tooltip = "Enables searching based on player skin hashes.",
                        value = false,
                        children = listOf(
                            entityTypePlayerSkinHashList
                        )
                    ),
                    BooleanSetting(
                        key = "EntityTypeOtherEnabled",
                        displayName = "Other Entities",
                        tooltip = "Enables searching based on entity paths",
                        value = false,
                        children = listOf(
                            entityTypeMobPathValue
                        )
                    )
                )
            ),
            BooleanSetting(
                key = "EntityEquipmentDetectionEnabled",
                displayName = "Entity Helmet",
                tooltip = "Highlight based on the entity helmet filtering",
                value = false,
                children = listOf(
                    TextSetting(
                        key = "EntityEquipmentHelmetSkullHash",
                        displayName = "Entity Helmet",
                        tooltip = "The skull hash to look for on the entity (get with mob hit debug)",
                        value = "a8abb471db0ab78703011979dc8b40798a941f3a4dec3ec61cbeec2af8cffe8" //default rat helmet skin
                    )
                )
            ),
            BooleanSetting(
                key = "MobInfoEnabled",
                displayName = "Mob Info",
                tooltip = "If to use a mob info based filtering for mob highlighting",
                value = false,
                children = listOf(
                    TextSetting(
                        key = "MobInfoContains",
                        displayName = "Mob Name Contains",
                        tooltip = "The string which to filter mobs in",
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
    fun onEntityAdded(event: OnEntityAdded) {
        handleEntitiesAdded(event.addedEntityList)
    }

    @EventHandler
    fun onEntityRemoved(event: OnEntityRemoved) {
        handleEntitiesRemoved(event.removedEntityList)
    }

    @EventHandler
    fun onEntityUpdated(event: OnEntityUpdated) {
        handleEntitiesUpdated(event.updatedEntityList)
    }

    /**
     * The armour colour each mineshaft corpse wears, which is also what it is outlined in. Read off
     * the dyed leather with the mob hit debug: lapis, umber and tungsten.
     */
    private val CORPSE_COLORS: Set<Int> = setOf(0x0000FF, 0xC83200, 0xCCE5FF)

    /**
     * The corpse colour this entity wears, or null when it is not a corpse. Taken from the dyed
     * chestplate, which all three wear; their heads differ, a sea lantern, a helmet and a skull.
     */
    private fun corpseColor(entity: Entity): Int? {
        if (entity !is LivingEntity) return null

        val dyed = entity.getItemBySlot(EquipmentSlot.CHEST)
            .get(DataComponents.DYED_COLOR)
            ?.rgb
            ?: return null

        val rgb = dyed and 0xFFFFFF

        return if (rgb in CORPSE_COLORS) 0xFF000000.toInt() or rgb else null
    }

    /** The entity a preset wants outlined, or null when no preset matched. */
    fun presetTarget(info: EntityInfo): Entity? {
        val presetsEnabled = baseSetting.getChild<BooleanSetting>("PresetsEnabled")
        presetsEnabled ?: return null
        if (!presetsEnabled.value) return null

        val dirtTreasurePresetEnabled = presetsEnabled.getChild<BooleanSetting>("PresetsForagingTreasure")
        if (dirtTreasurePresetEnabled?.value ?: false){
            if (info.entity is Display.ItemDisplay && info.entity.itemStack.item == Items.STRING){
                return info.entity
            }
        }

        // only inside a mineshaft: the three colours are ordinary dyes, and anything else wearing
        // one of them elsewhere in the game is not a corpse
        val shaftCorpsesEnabled = presetsEnabled.getChild<BooleanSetting>("PresetsShaftCorpses")
        if (shaftCorpsesEnabled?.value == true &&
            LocationAPI.island == SkyBlockIsland.MINESHAFT &&
            corpseColor(info.entity) != null
        ) {
            return info.entity
        }

        return null
    }


    override fun highlightTarget(info: EntityInfo): Entity? {
        if (!baseSetting.value) return null

        presetTarget(info)?.let { return it }
        val entity = info.entity

        var matches = false
        var hasAnyFilter = false

        // what to outline when a filter matched something standing beside the mob rather than the
        // mob itself: a rat is an invisible zombie whose skull is its own item display
        var visual: Entity? = null

        val entityTypeSetting = baseSetting.getChild<BooleanSetting>("EntityTypeEnabled")
        if (entityTypeSetting?.value == true) {
            hasAnyFilter = true

            val entityTypePlayerEnabled = entityTypeSetting
                .getChild<BooleanSetting>("EntityTypePlayerEnabled")

            val typePlayerResult =
                if (entity !is Player) {
                    false
                } else {
                    val skinHashEntryList = entityTypePlayerEnabled
                        ?.getChild<ToggleListSetting>("EntityTypePlayerSkinHash")?.value
                        ?: emptyList()

                    val actualHash = PlayerUtils.getSkinHash(entity)
                    val hashList = skinHashEntryList
                        .filter { it.enabled }
                        .map { it.value }

                    actualHash in hashList
                }

            val entityTypeOtherEnabled = entityTypeSetting
                .getChild<BooleanSetting>("EntityTypeOtherEnabled")

            val typeOtherResult =
                if (entity is LocalPlayer) {
                    false
                } else {
                    val expectedPath = entityTypeOtherEnabled
                        ?.getChild<TextSetting>("EntityTypeMobPathValue")?.value
                        ?: ""

                    if (expectedPath.isEmpty()){
                        false
                    }
                    else {
                        entity.type.toString().contains(expectedPath)
                    }
                }
            matches = typePlayerResult || typeOtherResult
        }

        val mobInfoSetting = baseSetting.getChild<BooleanSetting>("MobInfoEnabled")
        if (mobInfoSetting?.value == true) {
            hasAnyFilter = true

            val filter = mobInfoSetting
                .getChild<TextSetting>("MobInfoContains")?.value
                ?: return null

            val matchesName =
                entity.customName?.string?.contains(filter, true) == true
            val matchesArmorStandTag =
                info.informationEntities?.any {
                    it.customName?.string?.contains(filter, true) ?: false
                } == true


            matches = matches || (matchesName || matchesArmorStandTag)
        }

        val entityEquipmentDetection = baseSetting.getChild<BooleanSetting>("EntityEquipmentDetectionEnabled")
        if (entityEquipmentDetection?.value == true) {
            hasAnyFilter = true

            if (entity !is LivingEntity) return null

            val expectedHash = entityEquipmentDetection
                .getChild<TextSetting>("EntityEquipmentHelmetSkullHash")?.value
                ?: return null

            var hashResult = false

            val entityHeadStack = entity.getItemBySlot(EquipmentSlot.HEAD)
            var actualHash = PlayerUtils.getSkinHash(entityHeadStack)

            if (actualHash == expectedHash) {
                hashResult = true
            }

            if (!hashResult) {
                info.informationEntities?.forEach { infoEntity ->
                    if (hashResult) return@forEach

                    val stack = when (infoEntity) {
                        is ArmorStand -> infoEntity.getItemBySlot(EquipmentSlot.HEAD)
                        is Display.ItemDisplay -> infoEntity.itemStack
                        else -> ItemStack.EMPTY
                    }

                    if (!stack.isEmpty) {
                        val actualHash = PlayerUtils.getSkinHash(stack)
                        if (actualHash == expectedHash) {
                            hashResult = true
                            visual = infoEntity
                        }
                    }
                }
            }





            matches = matches || hashResult
        }

        if (!hasAnyFilter) return null
        if (!matches) return null

        // the mob is what matched, but an invisible one is drawn by something else standing in it
        return if (entity.isInvisible) visual ?: entity else entity
    }
}
