package org.magic.magicaddons.commands.debug

import net.minecraft.util.Mth
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
 * Writes the blocks and stands around a plant as the kotlin a CropDefinition is made of, so a new
 * crop can be described by standing next to one and pasting. A development tool; nothing reads its output.
 */
object CropStageExporter {

    /** The skulls the plot's own marker stands carry, which belong to no crop. */
    val PLOT_MARKER_SKINS: Set<String> = setOf(
        "4099589796de185787ab92c3066d0d0af832ffad7153a42bb2e2d23598e7ea60",
        "df03ad96092f3f789902436709cdf69de6b727c121b3c2daef9ffa1ccaed186c"
    )

    fun copyCropStageData(
        basePos: BlockPos,
        stageNum: Int? = null,
        foundDefinition: CropDefinition? = null,
        discordFormat: Boolean = false
    ) {
        val result = buildCropStageData(basePos, stageNum, foundDefinition, discordFormat)
            ?: return

        Minecraft.getInstance().keyboardHandler.clipboard = result

        ChatUtils.sendWithPrefix("Copied crop stage to clipboard (${result.length} chars)")
    }

    /** The stage as kotlin, or null without a world. Quiet keeps the skipped-entity report out of chat. */
    fun buildCropStageData(
        basePos: BlockPos,
        stageNum: Int? = null,
        foundDefinition: CropDefinition? = null,
        discordFormat: Boolean = false,
        quiet: Boolean = false
    ): String? {
        val world = Minecraft.getInstance().level ?: return null
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

        // how far the world has turned this plant, undone so the stage exports identically wherever
        // it stands. The head pose rides on the body and needs nothing
        val worldStep = WorldRotation.step(basePos.x, basePos.z)
        val unturn = Math.floorMod(-worldStep, 4)

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

            // every greenhouse carries these, one per plot, and one standing high above a crop was
            // being written into that crop's stage as a stand nine blocks in the air
            if (hash in PLOT_MARKER_SKINS) continue
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
                    // negative zero is zero wearing the sign the un-rotation left on it, and it
                    // made byte-identical stages read as two different ones
                    offset = WorldRotation.rotate(offset, unturn).let {
                        Vec3(it.x + 0.0, it.y + 0.0, it.z + 0.0)
                    },
                    rotation = headRotations,
                    xRotation = entity.xRot,
                    yRotation = Mth.wrapDegrees(entity.yRot - 90f * worldStep) + 0.0f,
                    hash = hash,
                    customName = customName,
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

                    val hash = group.first().hash
                    val name = group.first().customName

                    val fields = mutableListOf<String>()

                    fields.add("offsets = listOf(\n$offsets\n)")

                    // always written, zeros included, same as the single stands
                    fields.add("rotations = listOf(\n$rotations\n)")
                    fields.add("xRotations = listOf(\n$xRotations\n)")
                    fields.add("yRotations = listOf(\n$yRotations\n)")

                    // only written when the stand has one, so no export contains the string "null"
                    // as a hash
                    if (hash != null) fields.add("hashString = \"$hash\"")
                    if (name != null) fields.add("customName = \"$name\"")

                    // a fact the singleton branch always kept and this one silently dropped, so
                    // full-size plants exported as small ones whenever their stands grouped
                    if (group.any { !it.isSmall }) fields.add("isSmall = false")

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
                        // always written, zeros included: leaving a pose of nothing out kept default
                        // posed stands reading as uncollected forever
                        fields.add("headRotation = Rotations(${stand.rotation.x}f, ${stand.rotation.y}f, ${stand.rotation.z}f)")
                        fields.add("xRotation = ${stand.xRotation}f")
                        fields.add("yRotation = ${stand.yRotation}f")
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

            // indented once at the end rather than per piece, so every level lines up whatever the
            // pieces were built from
            sb.appendLine("    armorStands = ${indent(final).trimStart()},")
        } else {
            sb.appendLine("    armorStands = listOf(),")
        }

        sb.appendLine("    ${stageNum ?: 1}..${stageNum ?: 1}")
        sb.appendLine(")")

        if (discordFormat) {
            sb.appendLine("```")
            sb.appendLine("Crop found: ${foundDefinition?.name} stageNum=$stageNum")
        }

        return sb.toString()
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
