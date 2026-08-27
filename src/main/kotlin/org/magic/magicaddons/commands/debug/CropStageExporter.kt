package org.magic.magicaddons.commands.debug

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
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

        val originVec = Vec3(
            basePos.x.toDouble() + width / 2.0, //get center of footprint
            basePos.y.toDouble(),               // has to be center for mirroring to work properly.
            basePos.z.toDouble() + width / 2.0
        )


        for (entity in stands) {
            if (entity !is ArmorStand) continue

            val offset = entity.position().subtract(originVec)

            val head = entity.getItemBySlot(EquipmentSlot.HEAD)
            val hash = PlayerUtils.getSkinHash(head)
            val headRotations = entity.headPose
            val customName = if (entity.hasCustomName()) {
                entity.name.string.replace("\"", "\\\"")
            } else null

            standData.add(
                ArmorStandExport(
                    offset = offset,
                    rotation = headRotations,
                    xRotation = entity.xRot,
                    yRotation = entity.yRot,
                    hash = hash,
                    customName = customName
                )
            )
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
                        "        Vec3(${it.offset.x}, ${it.offset.y}, ${it.offset.z})"
                    }

                    val rotations = group.joinToString(",\n") {
                        "        Rotations(${it.rotation.x}f, ${it.rotation.y}f, ${it.rotation.z}f)"
                    }

                    val xRotations = group.joinToString(",\n") {
                        "        ${it.xRotation}f"
                    }

                    val yRotations = group.joinToString(",\n") {
                        "        ${it.yRotation}f"
                    }

                    val anyAbnormalRotations = group.any { it.rotation.x != 0f || it.rotation.y != 0f || it.rotation.z != 0f }
                    val anyAbnormalXRotations = group.any { !it.xRotation.isCardinalYaw() }
                    val anyAbnormalYRotations = group.any { !it.yRotation.isCardinalYaw() }


                    val hash = group.first().hash
                    val name = group.first().customName

                    val rotationsSection = """
    rotations = listOf(
$rotations
    ),
    xRotations = listOf(
$xRotations
    ),
    yRotations = listOf(
$yRotations
    ),
""".trimIndent()

                    patternSections += """
CropArmorStand.matcherPattern(
    offsets = listOf(
$offsets
    ),
    ${if (anyAbnormalRotations || anyAbnormalXRotations || anyAbnormalYRotations) rotationsSection else ""}
    hashString = "$hash"${
                        name?.let {
                            ",\n    customName = \"$it\""
                        } ?: ""
                    }
)
        """.trimIndent()
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

                        append(
                            """
CropArmorStand(
    ${fields.joinToString(",\n" )}
)
""".trimIndent()
                        )
                    }
                }

                singletonSections += singletonText

            }


            val final = when {
                patterns.isNotEmpty() && singletons.isNotEmpty() ->
                    patternSections.joinToString(" + ") +
                            " + listOf(" +
                            singletonSections.joinToString(",\n") +
                            "\n)"

                patterns.isNotEmpty() ->
                    patternSections.joinToString(" + ")

                singletons.isNotEmpty() ->
                    "listOf(\n" +
                            singletonSections.joinToString(",\n") +
                            "\n)"

                else -> " listOf()"
            }
            sb.appendLine("    armorStands = $final,")
        } else {
            sb.appendLine("    armorStands = listOf(),")
        }

        sb.appendLine("    ${stageNum ?: 1}..${stageNum ?: 1}")
        sb.appendLine(")")

        if (discordFormat) {
            sb.appendLine("```")
            sb.appendLine("Crop found: ${foundDefinition?.name} stageNum=$stageNum")
        }

        val result = sb.toString()
        Minecraft.getInstance().keyboardHandler.clipboard = result

        ChatUtils.sendWithPrefix("Copied crop stage to clipboard (${result.length} chars)")
    }

    //temp for exporting
    data class ArmorStandExport(
        val offset: Vec3,
        val rotation: Rotations,
        val xRotation: Float,
        val yRotation: Float,
        val hash: String?,
        val customName: String?
    )

    data class CropBlockExport(
        val offset: BlockPos,
        val blockState: BlockState
    )
}
