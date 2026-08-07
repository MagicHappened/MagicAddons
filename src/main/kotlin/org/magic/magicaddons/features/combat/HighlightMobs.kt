package org.magic.magicaddons.features.combat

import net.minecraft.advancements.criterion.PlayerHurtEntityTrigger
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.TextSetting
import org.magic.magicaddons.data.config.ToggleListSetting
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.data.ListEntry
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.events.world.OnEntityRemoved
import org.magic.magicaddons.events.world.OnEntityUpdated
import org.magic.magicaddons.events.world.OnWorldTickEvent
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.PlayerUtils
import org.magic.magicaddons.util.EntityUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland


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

    var safariZone: String = ""

    @EventHandler
    fun onWorldTick(event: OnWorldTickEvent){
        if (LocationAPI.island != SkyBlockIsland.SAFARI) return
        val playerPos = Minecraft.getInstance().player?.position() ?: return
        val isPositiveX = playerPos.x >= -47.0
        val isPositiveZ = playerPos.z >= 0.0
        val safariValue = when {
            !isPositiveZ && isPositiveX -> "haunted"
            !isPositiveZ && !isPositiveX -> "icy"
            isPositiveZ && !isPositiveX -> "cavern"
            else -> "forest"
        }
        if (safariValue != safariZone){
            safariZone = safariValue
            invalidateMobs()
        }
    }

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
                        value = false,
                        children = listOf(
                            BooleanSetting(
                                key = "ForagingTreasureSafariCondition",
                                displayName = "Safari Restriction",
                                tooltip = "Restricts the treasure highlight to the current safari zone.",
                                value = false
                            )
                        )
                    ),
                    BooleanSetting(
                        key = "SafariPreset",
                        displayName = "Safari Mobs",
                        tooltip = "Conditionally display safari mobs depending on the area.",
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
    fun invalidateMobs(){
        EntityUtils.removeAllForSource(this)

        EntityUtils.entityInfoList?.forEach {
            if (shouldHighlight(it)) {
                EntityUtils.add(it.entity, this)
            }
        }
    }

    @EventHandler
    fun onConfigChanged(event: ConfigChangedEvent) {
        invalidateMobs()
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

    fun shouldHighlightPreset(info: EntityInfo): Boolean {
        val presetsEnabled = baseSetting.getChild<BooleanSetting>("PresetsEnabled")
        presetsEnabled ?: return false
        if (!presetsEnabled.value) return false

        val dirtTreasurePresetEnabled = presetsEnabled.getChild<BooleanSetting>("PresetsForagingTreasure")
        if (dirtTreasurePresetEnabled?.value ?: false){
            if (info.entity is Display.ItemDisplay && info.entity.itemStack.item == Items.STRING){
                val safariConditionEnabled = dirtTreasurePresetEnabled.getChild<BooleanSetting>("ForagingTreasureSafariCondition")
                if (safariConditionEnabled?.value ?: false){
                    val mobPos = info.entity.position()
                    val isPositiveXMob = mobPos.x >= -47.0
                    val isPositiveZMob = mobPos .z >= 0.0
                    val mobRegion = when {
                        !isPositiveZMob && isPositiveXMob -> "haunted"
                        !isPositiveZMob && !isPositiveXMob -> "icy"
                        isPositiveZMob && !isPositiveXMob -> "cavern"
                        else -> "forest"
                    }
                    return mobRegion == safariZone
                }
                return true
            }
        }

        val safariPresetEnabled = presetsEnabled.getChild<BooleanSetting>("SafariPreset")
        if (safariPresetEnabled?.value ?: false){
            if (matchesSafariConditions(info)){
                return true
            }
        }

        return false
    }


    fun shouldHighlight(info: EntityInfo): Boolean {
        if (shouldHighlightPreset(info)) return true
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


    fun matchesSafariConditions(info: EntityInfo): Boolean {
        if (LocationAPI.island != SkyBlockIsland.SAFARI) return false
        val entity = info.entity
        val entityPath = entity.type.toString()

        return when (safariZone) {
            "haunted" -> {
                val hauntedMobs = listOf(
                    "entity.minecraft.silverfish",
                    "entity.minecraft.endermite",
                    "entity.minecraft.bat",
                    "entity.minecraft.phantom",
                    "entity.minecraft.cave_spider"
                )

                val playerHash = "3504f1f2327a5110e643bb8667082512815fa434a29ed37f4ca83bb16d2db533"
                (entity is Player && PlayerUtils.getSkinHash(entity) == playerHash) ||
                        (hauntedMobs.contains(entityPath))
            }
            "icy" -> {
                val icyMobs = listOf(
                    "entity.minecraft.dolphin",
                    "entity.minecraft.snow_golem",
                    "entity.minecraft.goat",
                    "entity.minecraft.polar_bear",
                    "entity.minecraft.tropical_fish",
                    "entity.minecraft.glow_squid",
                )

                // mantis shrimp
                val displayHash = "9924c105aa431dabd47952dc1dddd6f751f883423f4db1487d9bacc2cfe99c7a"
                (entity is Display.ItemDisplay && PlayerUtils.getSkinHash(entity.itemStack) == displayHash) ||
                        (icyMobs.contains(entityPath))
            }
            "cavern" -> {
                val cavernMobs = listOf(
                    "entity.minecraft.silverfish",
                    "entity.minecraft.tropical_fish",
                    "entity.minecraft.slime",
                    "entity.minecraft.sniffer"
                )
                val displayHashes = listOf(
                    "5dbaab74d1acd0abe9d04abe9928725de5d4495fcb63b647228caf6944c20800",
                    "a89a76deedd42b410344100df2fa79b6eeac7e6f287745d656179368340ffade"
                )

                val playerHash = "eacd215ccde2f677c7c144e2b698ff33ea06a87aaf468d05d1f0dc5ec2bdbfe8"
                (entity is Bat && info.informationEntities?.any { it is Display.ItemDisplay && displayHashes.contains(PlayerUtils.getSkinHash(it.itemStack)) } ?: false ) ||
                        (entity is Display.ItemDisplay && displayHashes.contains(PlayerUtils.getSkinHash(entity.itemStack))) ||
                        (entity is Player && PlayerUtils.getSkinHash(entity) == playerHash) ||
                        (cavernMobs.contains(entityPath))
            }
            "forest" -> {
                val forestMobs = listOf(
                    "entity.minecraft.fox",
                    "entity.minecraft.frog",
                    "entity.minecraft.panda",
                    "entity.minecraft.bee",
                    "entity.minecraft.creaking",
                    "entity.minecraft.shulker",
                    "entity.minecraft.parrot",
                    "entity.minecraft.silverfish"
                )
                forestMobs.contains(entityPath)
            }
            else -> false
        }

    }
    // -47 65 0
    // haunted negative z positive x
    // icy negative z negative x
    // cavern positive z negative x
    // forest positive z positive x

    // haunt silverfish, bat, phantom, cave spider, player (need hash)
    // icy dolphin, snowman, goat, polar bear, mantis shrimp (need info), fish?
    // cavern silverfish,tropical fish, slime, player (eacd215ccde2f677c7c144e2b698ff33ea06a87aaf468d05d1f0dc5ec2bdbfe8) item display (a89a76deedd42b410344100df2fa79b6eeac7e6f287745d656179368340ffade) sniffer
    // forest fox, frog, panda, bee, creaking






}