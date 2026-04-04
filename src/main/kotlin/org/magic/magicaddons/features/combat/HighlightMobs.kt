package org.magic.magicaddons.features.combat

import net.minecraft.client.render.DrawStyle
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box
import net.minecraft.world.debug.gizmo.GizmoDrawing
import org.magic.magicaddons.config.data.BooleanSetting
import org.magic.magicaddons.config.data.EnumSetting
import org.magic.magicaddons.config.data.TextSetting
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.world.OnWorldTickEvent
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.PlayerUtils
import org.magic.magicaddons.util.WorldEntities


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

    init {
        EventBus.register(this)
    }

    // todo change logic based on settings
    @EventHandler
    fun onWorldTick(event: OnWorldTickEvent) {
        if (!baseSetting.value) return

        WorldEntities.entityList.forEach { info ->
            val player = info.entity
            if (player !is PlayerEntity) return@forEach

            // littlefoot skin hash: f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b
            val shouldHighlight = PlayerUtils.getSkinHash(player).equals("f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b")

            if (shouldHighlight) {
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
            }


        }
    }


}