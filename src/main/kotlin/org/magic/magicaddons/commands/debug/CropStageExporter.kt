package org.magic.magicaddons.commands.debug

import org.magic.magicaddons.data.greenhouse.WorldRotation
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropStates.toFunctionString
import org.magic.magicaddons.util.ChatUtils
import org.magic.magicaddons.util.PlayerUtils
import org.magic.magicaddons.util.isCardinalYaw

/**
 * Writes out the blocks and armor stands around a plant as the kotlin a [CropDefinition] is made
 * of, so a crop nobody has described yet can be added by standing next to one and pasting.
 *
 * A development tool rather than part of the greenhouse itself: it reads the world and produces
 * text, and nothing in the mod consumes what it writes.
 */
object CropStageExporter {

    fun copyCropStageData(
        basePos: BlockPos,
        stageNum: Int? = null,
        foundDefinition: CropDefinition? = null,
        discordFormat: Boolean = false
    ) {
        val world = Minecraft.getInstance().level ?: return
        val sb = StringBuilder(2048)

        val blockData = mutableListOf<CropBlockExport>()
        val standData = mutableListOf<ArmorStandExport>()

        val footprint = foundDefinition?.footprint
        val width = footprint?.width ?: 1
        val height = footprint?.height ?: 1

        if (discordFormat) {
            sb.appendLine("```")
        }

        for (dx in 0 until width) {
            for (dz in 0 until height) {

                var y = basePos.y + 1

                while (true) { //for multi height crops
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
        // capture maximum stands (false positives on players but thats fine)
        val box = AABB(
            basePos.x.toDouble(),
            basePos.y.toDouble() - 2,
            basePos.z.toDouble(),
            basePos.x + width.toDouble(),
            basePos.y.toDouble() + 14,
            basePos.z + height.toDouble()
        )

        val stands = world.getEntities(null, box)

        // the middle of the footprint on both axes, which is what the mirroring check measures
        // from. z takes the height, not the width: they only agree while every crop is square
        val originVec = Vec3(
            basePos.x.toDouble() + width / 2.0,
            basePos.y.toDouble(),
            basePos.z.toDouble() + height / 2.0
        )


        // what the export could not describe, so an empty armorStands list can be told apart from
        // a plant whose parts are not stands at all
        val skipped = mutableListOf<String>()

        for (entity in stands) {
            if (entity !is ArmorStand) {
                if (entity !is Player) {
                    skipped += "${entity.type.description.string} at ${fmt(entity.position())}"
                }
                continue
            }

            val offset = entity.position().subtract(originVec)

            val hash = PlayerUtils.getSkullHash(entity)
            val headRotations = entity.headPose
            val customName = if (entity.hasCustomName()) {
                entity.name.string.replace("\"", "\\\"")
            } else null

            // a stand with neither a head nor a name gives a definition nothing to match on, and
            // standing anywhere near a player puts their own nameplate stands inside the box
            if (hash == null && customName == null) {
                skipped += "nameless empty-handed stand at ${fmt(entity.position())}"
                continue
            }

            standData.add(
                ArmorStandExport(
                    offset = offset,
                    rotation = headRotations,
                    xRotation = entity.xRot,
                    yRotation = entity.yRot,
                    hash = hash,
                    customName = customName,
                    isSmall = entity.isSmall
                )
            )
        }



        if (skipped.isNotEmpty()) {
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
            CropBlockState.blockStatePattern(
                listOf(
                    $posList
                ),
                blockState = ${toFunctionString(it.first().blockState)}
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
    CropBlockState(
        offset = BlockPos(${block.offset.x}, ${block.offset.y}, ${block.offset.z}),
        blockState = ${toFunctionString(block.blockState)}
    )
    """.trimIndent()
                }

                parts += singletonPart
            }

            if (parts.isNotEmpty()){
                if (finalBlockString.isBlank()){ //no patterns only singletons
                    finalBlockString = "    blocks = listOf(\n" +
                            parts.joinToString(",\n") +
                            "\n)"

                } else { //patterns AND blocks
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
                it.hash
            }

            val singletons = grouped.values
                .filter { it.size == 1 }
                .map { it.first() }

            val patterns = grouped.values
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

                    val anyAbnormalRotations = group.any { it.rotation.x != 0f || it.rotation.y != 0f || it.rotation.z != 0f }
                    val anyAbnormalXRotations = group.any { !it.xRotation.isCardinalYaw() }
                    val anyAbnormalYRotations = group.any { !it.yRotation.isCardinalYaw() }


                    val hash = group.first().hash
                    val name = group.first().customName

                    val fields = mutableListOf<String>()

                    fields.add("offsets = listOf(\n$offsets\n)")

                    if (anyAbnormalRotations || anyAbnormalXRotations || anyAbnormalYRotations) {
                        fields.add("rotations = listOf(\n$rotations\n)")
                        fields.add("xRotations = listOf(\n$xRotations\n)")
                        fields.add("yRotations = listOf(\n$yRotations\n)")
                    }

                    // written only when there is one. A hash of null used to reach the file as the
                    // word "null", which is a hash no head will ever have
                    if (hash != null) fields.add("hashString = \"$hash\"")
                    if (name != null) fields.add("customName = \"$name\"")

                    patternSections += "CropArmorStand.matcherPattern(\n" +
                            indent(fields.joinToString(",\n")) +
                            "\n)"
                }

            }


            if (singletons.isNotEmpty()) {

                val singletonText = singletons.joinToString(",\n") { stand ->
                    buildString {

                        val fields = mutableListOf<String>()
                        fields.add("offset = Vec3(${stand.offset.x}, ${stand.offset.y}, ${stand.offset.z})")
                        if (stand.rotation.x != 0f || stand.rotation.y != 0f || stand.rotation.z != 0f) {
                            fields.add("headRotation = Rotations(${stand.rotation.x}f, ${stand.rotation.y}f, ${stand.rotation.z}f)")
                            fields.add("xRotation = ${stand.xRotation}f")
                            fields.add("yRotation = ${stand.yRotation}f")
                        }
                        if (stand.hash != null){
                            fields.add("hashString = \"${stand.hash}\"")
                        }
                        if (stand.customName != null){
                            fields.add("containsCustomName = \"${stand.customName}\"")
                        }
                        // written only when it differs, since a definition takes small as read
                        if (!stand.isSmall) {
                            fields.add("isSmall = false")
                        }

                        append("CropArmorStand(\n")
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

            // built at the left margin and moved into place in one go, so every level lines up with
            // the one above it however the pieces ended up being combined
            sb.appendLine("    armorStands = ${indent(final).trimStart()},")
        } else {
            sb.appendLine("    armorStands = listOf(),")
        }

        sb.appendLine("    ${stageNum ?: 1}..${stageNum ?: 1},")

        // the rotation this plant stood at, without which the offsets above are only canonical
        // for plants on the same diagonal of the grid
        sb.appendLine("    canonicalStep = ${WorldRotation.step(basePos.x, basePos.z)}")
        sb.appendLine(")")

        if (discordFormat) {
            sb.appendLine("```")
            sb.appendLine("Crop found: ${foundDefinition?.name} stageNum=$stageNum")
        }

        val result = sb.toString()
        Minecraft.getInstance().keyboardHandler.clipboard = result

        ChatUtils.sendWithPrefix("Copied crop stage to clipboard (${result.length} chars)")
    }

    /** A position short enough to read in chat. */
    private fun fmt(pos: Vec3): String = "%.4f %.4f %.4f".format(pos.x, pos.y, pos.z)

    /** Moves a block built at the left margin under whatever line it is being written into. */
    private fun indent(text: String, by: String = "    "): String =
        text.lineSequence().joinToString("\n") { if (it.isBlank()) it else by + it }

    //temp for exporting
    data class ArmorStandExport(
        val offset: Vec3,
        val rotation: Rotations,
        val xRotation: Float,
        val yRotation: Float,
        val hash: String?,
        val customName: String?,
        /** How the stand is built, which is what decides where the head it carries ends up. */
        val isSmall: Boolean
    )

    data class CropBlockExport(
        val offset: BlockPos,
        val blockState: BlockState
    )
}
