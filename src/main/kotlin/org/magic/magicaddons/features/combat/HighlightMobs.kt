package org.magic.magicaddons.features.combat

import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.config.data.EnumSetting
import org.magic.magicaddons.config.data.TextSetting
import org.magic.magicaddons.data.EntityInfo
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
                                        TextSetting(
                                            key = "EntityTypePlayerSkinHash",
                                            displayName = "Skin Hash Value",
                                            tooltip = "The skin hash value to detect",
                                            value = "f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b"
                                        )
                                    )

                                    EntityTypeDetection.Other -> listOf(
                                        TextSetting(
                                            key = "EntityTypeMobPathValue",
                                            displayName = "Mob Path",
                                            tooltip = "The mob path value to detect",
                                            value = "entity.minecraft.pig"
                                        )
                                    )
                                }
                            }
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

    var highlightedEntityList: MutableList<Entity> = mutableListOf()


    init {
        EventBus.register(this)
    }

    /*
    val minX = player.x - 0.5
                val minY = player.y
                val minZ = player.z - 0.5
                val maxX = player.x + 0.5
                val maxY = player.y + 2.0
                val maxZ = player.z + 0.5

                val box = Box(minX, minY, minZ, maxX, maxY, maxZ)
    GizmoDrawing.box(
                    box,
                    DrawStyle.stroked(0xFFFF0000.toInt(), 2f) // ARGB red
                ).ignoreOcclusion()

    */
    // change to only use WorldEntities instead of event

    // todo change to map with entity info boolean then just based on the boolean render or not

    @EventHandler
    fun onEntityAdded(event: OnEntityAdded) {
        event.addedEntityList.forEach {
            if (shouldHighlight(it))
                highlightedEntityList.add(it.entity)
        }
    }

    @EventHandler
    fun onEntityRemoved(event: OnEntityRemoved) {
        event.removedEntityList.forEach {
            highlightedEntityList.remove(it.entity)
        }
    }
    fun shouldHighlight(entity: EntityInfo): Boolean {

    }

    @EventHandler
    fun onWorldTick(onWorldTick: OnWorldTickEvent) {
        if (!baseSetting.value) return

        highlightedEntityList?.forEach { info ->
            val entity = info.entity
            if (entity !is PlayerEntity) return@forEach

            // littlefoot : f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b // do NOT delete
            val shouldHighlight = PlayerUtils.getSkinHash(entity) ==
                    "213cf0ca79a3611b8e05fe9e264fb2bf8d27e464dc12dc6e95dd0ae0c335a561"


            if (shouldHighlight) {
                WorldEntities.renderEntityBoundingBox(entity)
            }
        }
    }





}