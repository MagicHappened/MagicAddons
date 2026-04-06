package org.magic.magicaddons.features.combat

import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.config.data.EnumSetting
import org.magic.magicaddons.config.data.TextSetting
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.events.world.OnEntityRemoved
import org.magic.magicaddons.events.world.OnWorldTickEvent
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.PlayerUtils
import org.magic.magicaddons.util.world.WorldEntities


object HighlightMobs : Feature() {

    enum class EntityTypeDetection {
        Player,
        Other
    }

    override val id: String = "HighlightMobs"
    override val displayName: String = "Mob Highlight"
    override val tooltipMessage: String = "Highlights specific mobs of your choosing"
    override val category: String = "combat"

    val entityTypePlayerSkinHash = TextSetting(
        key = "EntityTypePlayerSkinHash",
        displayName = "Skin Hash Value",
        tooltip = "The skin hash value to detect (get with mob hit debug)",
        value = "f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b"
    )

    val entityTypeMobPathValue = TextSetting(
        key = "EntityTypeMobPathValue",
        displayName = "Mob Path",
        tooltip = "The mob path value to detect (get with mob hit debug)",
        value = "entity.minecraft.pig"
    )


    override val baseSetting: BooleanSetting by lazy {
        BooleanSetting(
            key = "enabled",
            displayName = displayName,
            tooltip = tooltipMessage,
            value = false,
            children = listOf(
                BooleanSetting(
                    key = "EntityTypeEnabled",
                    displayName = "Entity Type",
                    tooltip = "If to use an entity type based filtering for mob highlighting",
                    value = false,
                    children = listOf(
                        EnumSetting<EntityTypeDetection>(
                            key = "EntityTypePlayerOtherEnum",
                            displayName = "Entity Type", // just dont display this (only values)
                            tooltip = "Which entity type detection to use",
                            value = EntityTypeDetection.Player,
                            childrenProvider = { entityTypeDetection ->
                                when (entityTypeDetection) {
                                    EntityTypeDetection.Player -> listOf(
                                        entityTypePlayerSkinHash
                                    )

                                    EntityTypeDetection.Other -> listOf(
                                        entityTypeMobPathValue
                                    ) // for children provider cant create new instances cuz no saving of old data
                                }
                            }
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
    }

    var highlightedEntityList: MutableList<Entity>? = null

    fun initializeHighlightedEntityList() {

        highlightedEntityList = mutableListOf()

        WorldEntities.entityInfoList?.forEach {
            if (shouldHighlight(it)){
                highlightedEntityList?.add(it.entity)
            }
        }
    }

    init {
        EventBus.register(this)
    }

    @EventHandler
    fun onConfigChanged(event: ConfigChangedEvent) {
        highlightedEntityList = null
    }

    @EventHandler
    fun onEntityAdded(event: OnEntityAdded) {
        if (!baseSetting.value) return

        highlightedEntityList ?: initializeHighlightedEntityList()

        event.addedEntityList.forEach {
            if (shouldHighlight(it))
                highlightedEntityList?.add(it.entity)
        }
    }

    @EventHandler
    fun onEntityRemoved(event: OnEntityRemoved) {
        if (!baseSetting.value) return

        highlightedEntityList ?: initializeHighlightedEntityList()

        event.removedEntityList.forEach {
            highlightedEntityList?.remove(it.entity)
        }
    }

    fun shouldHighlight(info: EntityInfo): Boolean {
        val entity = info.entity

        if (!baseSetting.value) return false

        var matches = true
        var hasAnyFilter = false

        val entityTypeSetting = baseSetting.getChild<BooleanSetting>("EntityTypeEnabled")
        if (entityTypeSetting?.value == true) {
            hasAnyFilter = true

            val enumSetting = entityTypeSetting
                .getChild<EnumSetting<EntityTypeDetection>>("EntityTypePlayerOtherEnum")

            val result = when (enumSetting?.value) {
                EntityTypeDetection.Player -> {
                    if (entity !is PlayerEntity) return false

                    val expectedHash = enumSetting
                        .getChild<TextSetting>("EntityTypePlayerSkinHash")?.value
                        ?: return false

                    if (expectedHash.isBlank()) return true

                    val actualHash = PlayerUtils.getSkinHash(entity)

                    actualHash == expectedHash
                }

                EntityTypeDetection.Other -> {
                    val expectedPath = enumSetting
                        .getChild<TextSetting>("EntityTypeMobPathValue")?.value
                        ?: return false

                    entity.type.toString().contains(expectedPath)
                }

                null -> false
            }

            matches = result
        }

        val mobInfoSetting = baseSetting.getChild<BooleanSetting>("MobInfoEnabled")
        if (mobInfoSetting?.value == true) {
            hasAnyFilter = true

            val filter = mobInfoSetting
                .getChild<TextSetting>("MobInfoContains")?.value
                ?: return false

            val matchesName =
                entity.customName?.string?.contains(filter, true) == true

            val matchesArmorStandTag =
                info.armorStandTags?.any { it.contains(filter, true) } == true

            matches = matches && (matchesName || matchesArmorStandTag)
        }

        val entityEquipmentDetection = baseSetting.getChild<BooleanSetting>("EntityEquipmentDetectionEnabled")
        if (entityEquipmentDetection?.value == true) {
            hasAnyFilter = true

            if (entity !is LivingEntity) return false

            val expectedHash = entityEquipmentDetection
                .getChild<TextSetting>("EntityEquipmentHelmetSkullHash")?.value
                ?: return false

            val headStack = entity.getEquippedStack(EquipmentSlot.HEAD)

            val actualHash = PlayerUtils.getSkinHash(headStack)

            val result = actualHash == expectedHash

            matches = matches && result
        }

        if (!hasAnyFilter) return false

        return matches
    }

    @EventHandler
    fun onWorldTick(onWorldTick: OnWorldTickEvent) {
        highlightedEntityList ?: initializeHighlightedEntityList()

        highlightedEntityList?.forEach {
            WorldEntities.renderEntityBoundingBox(it)
        }
    }





}