package org.magic.magicaddons.commands.debug

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.commands.AbstractCommand
import org.magic.magicaddons.events.EventBus
import org.magic.magicaddons.events.EventHandler
import org.magic.magicaddons.events.interact.OnStartDestroyBlockEvent
import org.magic.magicaddons.util.BlockUtils.getId
import org.magic.magicaddons.util.BlockUtils.getIntProperty
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils

object DebugFarming : AbstractCommand() {
    override val argument: String = "farming"
    override val description: String = "farming debug"

    init {
        EventBus.register(this)
    }
    var debugEnabled = false

    override fun build(): LiteralArgumentBuilder<FabricClientCommandSource> {
        return LiteralArgumentBuilder.literal<FabricClientCommandSource>(argument).executes {
            debugEnabled = !debugEnabled
            val onString = if (debugEnabled) "on" else "off"
            ChatUtils.sendWithPrefix("turned $onString the crop debug")
            return@executes 1
        }
    }

    @EventHandler
    fun onStartBlockBreak(event: OnStartDestroyBlockEvent) {
        if (!debugEnabled) return
        val world = Minecraft.getInstance().level ?: return
        val pos = event.blockPos
        val state = world.getBlockState(pos)

        ChatUtils.sendWithPrefix(
            "BLOCK BREAK DEBUG -> pos=$pos block=${state.block}\nstate=$state"
        )

        val sb = StringBuilder(2048)


        val blockLines = mutableListOf<String>()

        var y = pos.y + 1
        var stageIndex = 0

        while (true) {
            val checkPos = BlockPos(pos.x, y, pos.z)
            val checkState = world.getBlockState(checkPos)

            if (checkState.isAir) break

            val offsetY = y - pos.y
            val blockId = checkState.getId()

            val hasAge = checkState.getIntProperty("age") != null

            val matcherLine = if (hasAge) {
                """
                it.isBlock("$blockId") &&
                        it.getIntProperty("age") == ${checkState.getIntProperty("age")}
            """.trimIndent()
            } else {
                """
                it.isBlock("$blockId")
            """.trimIndent()
            }

            blockLines.add(
                """
        CropBlockState(
            offset = BlockPos(0,$offsetY,0),
            matcher = {
$matcherLine
            }
        )
            """.trimIndent()
            )

            stageIndex++
            y++
        }


        val box = AABB(
            pos.x.toDouble(),
            pos.y.toDouble() - 2,
            pos.z.toDouble(),
            pos.x + 1.0,
            pos.y.toDouble() + 4,
            pos.z + 1.0
        )

        val stands = world.getEntities(null, box)

        val standLines = mutableListOf<String>()

        for (entity in stands) {
            if (entity !is ArmorStand) continue
            val center = Vec3(
                pos.x + 0.5,
                pos.y.toDouble(),
                pos.z + 0.5
            )

            val offset = entity.position().subtract(center)

            val head = entity.getItemBySlot(EquipmentSlot.HEAD)
            val hash = PlayerUtils.getSkinHash(head)
            ChatUtils.sendWithPrefix("stand pos: ${entity.position()} hash=$hash")

            standLines.add(
                """
        CropArmorStand(
            offset = Vec3(${offset.x}, ${offset.y}, ${offset.z}),
            matcher = {
                ${if (!hash.isNullOrBlank()) "it == \"$hash\"" else "true"}
            }
        )
    """.trimIndent()
            )
        }


        sb.appendLine("CropStage(")

        // blocks
        sb.appendLine("    blocks = listOf(")
        if (blockLines.isNotEmpty()) {
            sb.appendLine(blockLines.joinToString(",\n"))
        }
        sb.appendLine("    ),")

        // armor stands
        if (standLines.isNotEmpty()) {
            sb.appendLine("    armorStands = listOf(")
            sb.appendLine(
                standLines.joinToString(",\n") { "        $it" }
            )
            sb.appendLine("    ),")
        } else {
            sb.appendLine("    armorStands = null,")
        }

        sb.appendLine("    1..1")
        sb.appendLine(")")

        val result = sb.toString()

        Minecraft.getInstance().keyboardHandler.clipboard = result

        ChatUtils.sendWithPrefix("Copied crop stage to clipboard (${result.length} chars)")
        event.canceled = true
    }
}