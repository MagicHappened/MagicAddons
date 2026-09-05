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
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.TextSetting
import org.magic.magicaddons.data.config.ToggleListSetting
import org.magic.magicaddons.events.ConfigChangedEvent
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.chat.OnSystemChatEvent
import org.magic.magicaddons.events.interact.OnInteractEntityEvent
import org.magic.magicaddons.events.world.OnEntityAdded
import org.magic.magicaddons.events.world.OnEntityRemoved
import org.magic.magicaddons.events.world.OnEntityUpdated
import org.magic.magicaddons.features.HighlightFeature
import org.magic.magicaddons.util.PlayerUtils
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
            "§fPresets, single mobs, a name, or advanced filters."
    override val category: String = "combat"

    val entityTypePlayerSkinHash = TextSetting(
        key = "EntityTypePlayerSkinHash",
        displayName = "Skin Hash Value",
        description = "§fThe skin hash to look for.\n§eGet it from the mob hit debug.",
        value = "f2b33640bfb71557e0e1d852287263ceafc9bec205301acf046b7c29fe8cb37b"
    )

    val entityTypeMobPathValue = TextSetting(
        key = "EntityTypeMobPathValue",
        displayName = "Mob Path",
        description = "§fThe entity type path to look for, such as entity.minecraft.pig.\n" +
                "§eGet it from the mob hit debug.",
        value = "entity.minecraft.pig"
    )

    val singleMobsList = ToggleListSetting(
        key = "SingleMobs",
        displayName = "Mobs",
        description = "",
        value = mutableListOf(),
        choices = { SingleMobs.names }
    )

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false,
        children = listOf(
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
                description = "§fHighlight specific mobs.\n" +
                        "§bIf a mob you want isn't added here, suggest it to a dev for implementation.",
                value = false,
                children = listOf(singleMobsList)
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
            ),
            BooleanSetting(
                key = "AdvancedHighlightEnabled",
                displayName = "Advanced Highlight",
                description = "§fFilters by entity type, skin hash or helmet skull,\n" +
                        "§ffor mobs no preset covers.\n" +
                        "§eValues come from the mob hit debug.",
                value = false,
                children = listOf(
                    BooleanSetting(
                        key = "EntityTypeEnabled",
                        displayName = "Entity Type",
                        description = "§fMatch on what the entity is.\n§fA player's skin hash, or a mob's type path.",
                        value = false,
                        children = listOf(
                            BooleanSetting(
                                key = "EntityTypePlayerEnabled",
                                displayName = "Player Entity",
                                description = "§fMatch players by skin hash.",
                                value = false,
                                children = listOf(entityTypePlayerSkinHash)
                            ),
                            BooleanSetting(
                                key = "EntityTypeOtherEnabled",
                                displayName = "Other Entities",
                                description = "§fMatch non-player entities by type path.",
                                value = false,
                                children = listOf(entityTypeMobPathValue)
                            )
                        )
                    ),
                    BooleanSetting(
                        key = "EntityEquipmentDetectionEnabled",
                        displayName = "Entity Helmet",
                        description = "§fMatch on the skull an entity wears,\n" +
                                "§for one carried by a stand or display standing in it.",
                        value = false,
                        children = listOf(
                            TextSetting(
                                key = "EntityEquipmentHelmetSkullHash",
                                displayName = "Helmet Skull Hash",
                                description = "§fThe skull hash to look for.\n§eGet it from the mob hit debug.",
                                value = "a8abb471db0ab78703011979dc8b40798a941f3a4dec3ec61cbeec2af8cffe8" //default rat helmet skin
                            )
                        )
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

    /** Corpses looted this visit, by entity id. Cleared when the mineshaft is left. */
    private val lootedCorpses: MutableSet<Int> = mutableSetOf()

    /** The corpse just right clicked, waiting for chat to say whether the loot went through. */
    private var pendingCorpse: Entity? = null
    private var pendingSince: Long = 0

    /** How long a right click waits for its loot message before it is forgotten. */
    private const val LOOT_WINDOW_MS: Long = 3000

    /** "  LAPIS CORPSE LOOT!", sent only to the player who opened it. */
    private val CORPSE_LOOT_MESSAGE = Regex("\\s*\\w+ CORPSE LOOT!\\s*")

    @EventHandler
    fun onInteractEntity(event: OnInteractEntityEvent) {
        if (!hideLootedEnabled()) return
        if (corpseColor(event.target) == null) return

        pendingCorpse = event.target
        pendingSince = System.currentTimeMillis()
    }

    /** The loot message names the type but not which corpse, so it settles the one just clicked. */
    @EventHandler
    fun onSystemChat(event: OnSystemChatEvent) {
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

    /** The entity a preset wants outlined, or null when no preset matched. */
    private fun presetTarget(info: EntityInfo): Entity? {
        val presets = baseSetting.getChild<BooleanSetting>("PresetsEnabled") ?: return null
        if (!presets.value) return null

        if (presets.getChild<BooleanSetting>("PresetsForagingTreasure")?.value == true) {
            if (info.entity is Display.ItemDisplay && info.entity.itemStack.item == Items.STRING) {
                return info.entity
            }
        }

        // only inside a mineshaft: the three colours are ordinary dyes, and anything else wearing
        // one of them elsewhere in the game is not a corpse
        if (presets.getChild<BooleanSetting>("PresetsShaftCorpses")?.value == true &&
            LocationAPI.island == SkyBlockIsland.MINESHAFT &&
            corpseColor(info.entity) != null &&
            info.entity.id !in lootedCorpses
        ) {
            return info.entity
        }

        return null
    }

    /** The entity one of the picked single mobs wants outlined, or null. */
    private fun singleMobTarget(info: EntityInfo): Entity? {
        if (baseSetting.getChild<BooleanSetting>("SingleMobsEnabled")?.value != true) return null

        return singleMobsList.value
            .asSequence()
            .filter { it.enabled }
            .mapNotNull { SingleMobs.byName(it.value) }
            .mapNotNull { SingleMobs.target(it, info) }
            .firstOrNull()
    }

    /** The mob, when its own name or a tag beside it contains the filter text. */
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

    /** The advanced filters: entity type, skin hash and helmet skull. */
    private fun advancedTarget(info: EntityInfo): Entity? {
        val advanced = baseSetting.getChild<BooleanSetting>("AdvancedHighlightEnabled") ?: return null
        if (!advanced.value) return null

        val entity = info.entity

        val entityType = advanced.getChild<BooleanSetting>("EntityTypeEnabled")
        if (entityType?.value == true) {
            val playerEnabled = entityType.getChild<BooleanSetting>("EntityTypePlayerEnabled")?.value == true
            if (playerEnabled && entity is Player) {
                val expected = entityTypePlayerSkinHash.value
                if (expected.isNotBlank() && PlayerUtils.getSkinHash(entity) == expected) return entity
            }

            val otherEnabled = entityType.getChild<BooleanSetting>("EntityTypeOtherEnabled")?.value == true
            if (otherEnabled && entity !is LocalPlayer) {
                val path = entityTypeMobPathValue.value
                if (path.isNotBlank() && entity.type.toString().contains(path)) return entity
            }
        }

        val helmet = advanced.getChild<BooleanSetting>("EntityEquipmentDetectionEnabled")
        if (helmet?.value == true && entity is LivingEntity) {
            val expected = helmet.getChild<TextSetting>("EntityEquipmentHelmetSkullHash")?.value
                ?: return null

            if (PlayerUtils.getSkinHash(entity.getItemBySlot(EquipmentSlot.HEAD)) == expected) return entity

            // a skull on something standing in the mob: a rat is an invisible zombie whose skull is
            // its own item display, and that display is what should be drawn
            val carrier = info.informationEntities?.firstOrNull { other ->
                val stack = when (other) {
                    is ArmorStand -> other.getItemBySlot(EquipmentSlot.HEAD)
                    is Display.ItemDisplay -> other.itemStack
                    else -> ItemStack.EMPTY
                }
                !stack.isEmpty && PlayerUtils.getSkinHash(stack) == expected
            }
            if (carrier != null) return if (entity.isInvisible) carrier else entity
        }

        return null
    }

    override fun highlightTarget(info: EntityInfo): Entity? {
        if (!baseSetting.value) return null

        return presetTarget(info)
            ?: singleMobTarget(info)
            ?: nameTarget(info)
            ?: advancedTarget(info)
    }
}
