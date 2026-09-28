package org.magic.magicaddons.commands.debug

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.commands.formatPosition
import org.magic.magicaddons.data.greenhouse.crops.CropBlocks.toCode
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.WorldRotation
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.EntityUtils
import org.magic.magicaddons.util.PlayerUtils

object CropStageExporter {

    fun buildCropStageData(
        basePos: BlockPos,
        stageNum: Int? = null,
        foundDefinition: CropDefinition? = null,
        quiet: Boolean = false,
        knownStands: List<Entity> = emptyList()
    ): String? {
        val world = Minecraft.getInstance().level ?: return null
        val sb = StringBuilder(2048)

        val blockData = mutableListOf<CropBlockExport>()
        val standData = mutableListOf<ArmorStandExport>()

        val footprint = foundDefinition?.footprint
        val width = footprint?.width ?: 1
        val height = footprint?.height ?: 1

        for (dx in 0 until width) {
            for (dz in 0 until height) {

                var y = basePos.y + 1

                while (true) {
                    val checkPos = BlockPos(
                        basePos.x + dx,
                        y,
                        basePos.z + dz
                    )

                    val checkState = world.getBlockState(checkPos)

                    if (checkState.isAir) break

                    val offsetY = y - basePos.y

                    blockData.add(
                        CropBlockExport(
                            offset = BlockPos(dx,offsetY,dz),
                            blockState = checkState
                        )
                    )
                    y++
                }
            }
        }
        val box = AABB(
            basePos.x.toDouble(),
            basePos.y.toDouble() - 2,
            basePos.z.toDouble(),
            basePos.x + width.toDouble(),
            basePos.y.toDouble() + 14,
            basePos.z + height.toDouble()
        )

        val stands = world.getEntities(null, box).ifEmpty { knownStands }

        val worldStep = if (foundDefinition?.rotatesWithPlot == false) 0 else WorldRotation.quarterTurnsAt(basePos.x, basePos.z)
        val unturn = Math.floorMod(-worldStep, 4)

        val originVec = Vec3(
            basePos.x.toDouble() + width / 2.0,
            basePos.y.toDouble(),
            basePos.z.toDouble() + height / 2.0
        )

        val skipped = mutableListOf<String>()

        for (entity in stands) {
            if (entity !is ArmorStand) {
                if (entity !is Player) {
                    skipped += "${entity.type.description.string} at ${formatPosition(entity.position())}"
                }
                continue
            }

            val offset = entity.position().subtract(originVec)

            val hash = PlayerUtils.getSkullHash(entity)

            val headRotations = entity.headPose
            val customName = if (entity.hasCustomName()) {
                entity.name.string.replace("\"", "\\\"")
            } else null

            val held = if (hash == null) EntityUtils.heldItem(entity) else null
            val itemId = held?.second

            if (hash == null && customName == null && itemId == null) {
                skipped += "nameless empty-handed stand at ${formatPosition(entity.position())}"
                continue
            }

            standData.add(
                ArmorStandExport(
                    offset = WorldRotation.turned(offset, unturn).let {
                        Vec3(it.x + 0.0, it.y + 0.0, it.z + 0.0)
                    },
                    rotation = headRotations,
                    xRotation = entity.xRot,
                    yRotation = Mth.wrapDegrees(entity.yRot - 90f * worldStep) + 0.0f,
                    hash = hash,
                    customName = customName,
                    itemId = itemId,
                    itemSlot = held?.first,
                    isSmall = entity.isSmall
                )
            )
        }

        if (skipped.isNotEmpty() && !quiet) {
            ChatUtils.sendWithPrefix(
                Component.literal("${skipped.size} thing(s) near this crop were not exported")
                    .withStyle(ChatFormatting.YELLOW)
            )
            skipped.forEach { ChatUtils.send("  $it") }
        }

        sb.appendLine("CropStage(")

        var finalBlockString = ""
        if (blockData.isNotEmpty()){
            val grouped = blockData.groupBy {
                it.blockState
            }

            val singletons = grouped.values
                .filter { it.size == 1 }
                .map { it.first() }

            val patterns = grouped.values
                .filter { it.size > 1 }

            val parts = mutableListOf<String>()

            if (patterns.isNotEmpty()) {
                patterns.forEach {

                    val posList = it.joinToString(",\n") { b ->
                        "BlockPos(${b.offset.x}, ${b.offset.y}, ${b.offset.z})"
                    }

                    parts += """
            StageBlock.atPositions(
                positions = listOf(
                    $posList
                ),
                blockState = ${toCode(it.first().blockState)}
            )
        """.trimIndent()
                }
            }

            if (parts.isNotEmpty()){
                var appendedString = "    blocks = " + parts.removeFirst()
                parts.forEach {
                    appendedString += " + $it"
                }

                finalBlockString = appendedString
                parts.clear()
            }


            if (singletons.isNotEmpty()) {
                val singletonPart = singletons.joinToString(",\n") { block ->
                    """
    StageBlock(
        offset = BlockPos(${block.offset.x}, ${block.offset.y}, ${block.offset.z}),
        blockState = ${toCode(block.blockState)}
    )
    """.trimIndent()
                }

                parts += singletonPart
            }

            if (parts.isNotEmpty()){
                if (finalBlockString.isBlank()){
                    finalBlockString = "    blocks = listOf(\n" +
                            parts.joinToString(",\n") +
                            "\n)"

                } else {
                    val combined = finalBlockString +
                            " + listOf(\n" +
                            parts.joinToString(",\n") +
                            "\n)"

                    finalBlockString = combined
                }
            }

            if (finalBlockString.isNotBlank()) {
                sb.appendLine("$finalBlockString,")
            }
        }
        else {
            sb.appendLine("    blocks = listOf(),")
        }

        if (standData.isNotEmpty()) {

            val grouped = standData.groupBy {
                it.hash ?: it.itemId
            }

            val (uniform, mixed) = grouped.values.partition { group -> group.map { it.isSmall }.distinct().size == 1 }

            val singletons = uniform
                .filter { it.size == 1 }
                .map { it.first() } + mixed.flatten()

            val patterns = uniform
                .filter { it.size > 1 }

            val patternSections = mutableListOf<String>()
            val singletonSections = mutableListOf<String>()

            if (patterns.isNotEmpty()) {
                patterns.forEach { group ->

                    val offsets = group.joinToString(",\n") {
                        "    Vec3(${it.offset.x}, ${it.offset.y}, ${it.offset.z})"
                    }

                    val rotations = group.joinToString(",\n") {
                        "    Rotations(${it.rotation.x}f, ${it.rotation.y}f, ${it.rotation.z}f)"
                    }

                    val xRotations = group.joinToString(",\n") {
                        "    ${it.xRotation}f"
                    }

                    val yRotations = group.joinToString(",\n") {
                        "    ${it.yRotation}f"
                    }

                    val hash = group.first().hash
                    val name = group.first().customName
                    val itemId = group.first().itemId
                    val itemSlot = group.first().itemSlot

                    val fields = mutableListOf<String>()

                    fields.add("offsets = listOf(\n$offsets\n)")
                    fields.add("isSmall = ${group.first().isSmall}")

                    fields.add("rotations = listOf(\n$rotations\n)")

                    if (group.any { it.xRotation != 0f }) fields.add("xRotations = listOf(\n$xRotations\n)")
                    if (group.any { it.yRotation != 0f }) fields.add("yRotations = listOf(\n$yRotations\n)")

                    if (hash != null) fields.add("hashString = \"$hash\"")
                    if (name != null) fields.add("nameContains = \"$name\"")
                    if (itemId != null) fields.add("heldItemId = \"$itemId\"")
                    if (itemSlot != null && itemSlot != EquipmentSlot.HEAD) {
                        fields.add("itemSlot = EquipmentSlot.$itemSlot")
                    }

                    patternSections += "StageStand.atOffsets(\n" +
                            indent(fields.joinToString(",\n")) +
                            "\n)"
                }

            }


            if (singletons.isNotEmpty()) {

                val singletonText = singletons.joinToString(",\n") { stand ->
                    buildString {

                        val fields = mutableListOf<String>()
                        fields.add("offset = Vec3(${stand.offset.x}, ${stand.offset.y}, ${stand.offset.z})")
                        fields.add("isSmall = ${stand.isSmall}")
                        fields.add("headRotation = Rotations(${stand.rotation.x}f, ${stand.rotation.y}f, ${stand.rotation.z}f)")

                        if (stand.xRotation != 0f) fields.add("xRotation = ${stand.xRotation}f")
                        if (stand.yRotation != 0f) fields.add("yRotation = ${stand.yRotation}f")
                        if (stand.hash != null){
                            fields.add("hashString = \"${stand.hash}\"")
                        }
                        if (stand.customName != null){
                            fields.add("nameContains = \"${stand.customName}\"")
                        }
                        if (stand.itemId != null){
                            fields.add("heldItemId = \"${stand.itemId}\"")
                        }
                        if (stand.itemSlot != null && stand.itemSlot != EquipmentSlot.HEAD){
                            fields.add("itemSlot = EquipmentSlot.${stand.itemSlot}")
                        }

                        append("StageStand(\n")
                        append(indent(fields.joinToString(",\n")))
                        append("\n)")
                    }
                }

                singletonSections += singletonText

            }


            val singletonList = "listOf(\n" +
                    indent(singletonSections.joinToString(",\n")) +
                    "\n)"

            val final = when {
                patterns.isNotEmpty() && singletons.isNotEmpty() ->
                    patternSections.joinToString(" +\n") + " +\n" + singletonList

                patterns.isNotEmpty() -> patternSections.joinToString(" +\n")

                singletons.isNotEmpty() -> singletonList

                else -> "listOf()"
            }

            sb.appendLine("    armorStands = ${indent(final).trimStart()},")
        } else {
            sb.appendLine("    armorStands = listOf(),")
        }

        sb.appendLine("    ${stageNum ?: 1}..${stageNum ?: 1}")
        sb.appendLine(")")

        return sb.toString()
    }

    private fun indent(text: String, by: String = "    "): String =
        text.lineSequence().joinToString("\n") { if (it.isBlank()) it else by + it }

    data class ArmorStandExport(
        val offset: Vec3,
        val rotation: Rotations,
        val xRotation: Float,
        val yRotation: Float,
        val hash: String?,
        val customName: String?,
        val itemId: String?,
        val itemSlot: EquipmentSlot?,
        val isSmall: Boolean
    )

    data class CropBlockExport(
        val offset: BlockPos,
        val blockState: BlockState
    )
}
