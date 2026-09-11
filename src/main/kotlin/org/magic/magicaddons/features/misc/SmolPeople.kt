package org.magic.magicaddons.features.misc

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import org.magic.magicaddons.data.config.BooleanSetting
import org.magic.magicaddons.data.config.IntSetting
import org.magic.magicaddons.features.Feature
import org.magic.magicaddons.util.EntityUtils

/**
 * Draws real players as the old baby model: body and limbs at half size, the head at three
 * quarters, so the head is big for the body. Only the picture changes: hitboxes, reach and what
 * the server knows are untouched, and NPCs the server dresses up as players are left as they are.
 */
object SmolPeople : Feature() {

    override val id: String = "SmolPeople"
    override val displayName: String = "Smol People"
    override val description: String = "Draws other players small. Purely visual: hitboxes stay where they are."
    override val category: String = "misc"

    private val bodySetting = IntSetting(
        key = "BodySize",
        displayName = "Body Size",
        description = "How large the body, arms and legs are drawn, as a percentage of normal.",
        value = 50,
        range = 10..150,
        step = 5
    )

    private val headSetting = IntSetting(
        key = "HeadSize",
        displayName = "Head Size",
        description = "How large the head is drawn, as a percentage of normal.",
        value = 75,
        range = 10..150,
        step = 5
    )

    override val baseSetting: BooleanSetting = BooleanSetting(
        displayName = displayName,
        description = description,
        value = false,
        children = listOf(bodySetting, headSetting)
    )

    /** Whether this entity is drawn small: a real account, not an NPC wearing a player's shape. */
    @JvmStatic
    fun applies(entity: Entity): Boolean =
        baseSetting.value && entity is Player && EntityUtils.isRealPlayer(entity)

    @JvmStatic
    fun bodyFactor(): Float = bodySetting.value / 100f

    @JvmStatic
    fun headFactor(): Float = headSetting.value / 100f
}
