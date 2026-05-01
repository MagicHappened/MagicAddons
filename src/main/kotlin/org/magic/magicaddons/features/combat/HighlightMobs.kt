package org.magic.magicaddons.features.combat

import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
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
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.PlayerUtils
import org.magic.magicaddons.util.EntityUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI


object HighlightMobs : Feature(), EntityUtils.HighlightSource {
    override val highlightPriority: Int = 0
    override val highlightColor: Int = 0xFFFFFFFF.toInt()

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
//                    EnumSetting<EntityTypeDetection>(
//                        key = "EntityTypePlayerOtherEnum",
//                        displayName = "Entity Type", // just dont display this (only values)
//                        tooltip = "Which entity type detection to use",
//                        value = EntityTypeDetection.Player,
//                        children = listOf(
//                            entityTypePlayerSkinHashList,
//                            entityTypeMobPathValue
//                        ),
//                        childrenProvider = { entityTypeDetection ->
//                            when (entityTypeDetection) {
//                                EntityTypeDetection.Player -> listOf(
//                                    entityTypePlayerSkinHashList
//                                )
//                                EntityTypeDetection.Other -> listOf(
//                                    entityTypeMobPathValue
//                                )
//                            }
//                        }
//                    )
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
        EntityUtils.removeAllForSource(this)

        EntityUtils.entityInfoList?.forEach {
            if (shouldHighlight(it)) {
                EntityUtils.add(it.entity, this)
            }
        }
    }

    @EventHandler
    fun onEntityAdded(event: OnEntityAdded) {
        if (!baseSetting.value) return

        event.addedEntityList.forEach {
            if (shouldHighlight(it))
                EntityUtils.add(it.entity,this)
        }
    }

    @EventHandler
    fun onEntityRemoved(event: OnEntityRemoved) {
        if (!baseSetting.value) return

        event.removedEntityList.forEach {
            EntityUtils.remove(it.entity,this)
        }
    }

    @EventHandler
    fun onEntityUpdated(event: OnEntityUpdated) {
        if (!baseSetting.value) return

        event.updatedEntityList.forEach { info ->
            val should = shouldHighlight(info)
            val has = EntityUtils.hasSource(info.entity, this)

            when {
                should && !has -> EntityUtils.add(info.entity, this)
                !should && has -> EntityUtils.remove(info.entity, this)
            }
        }
    }



    fun shouldHighlight(info: EntityInfo): Boolean {
        val entity = info.entity

        if (!baseSetting.value) return false

        var matches = false
        var hasAnyFilter = false

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
                ?: return false

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

            if (entity !is LivingEntity) return false

            val expectedHash = entityEquipmentDetection
                .getChild<TextSetting>("EntityEquipmentHelmetSkullHash")?.value
                ?: return false

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
                        }
                    }
                }
            }





            matches = matches || hashResult
        }

        if (!hasAnyFilter) return false

        return matches
    }




}