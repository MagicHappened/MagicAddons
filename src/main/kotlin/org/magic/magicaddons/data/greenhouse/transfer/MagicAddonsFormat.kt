package org.magic.magicaddons.data.greenhouse.transfer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.crops.CropDefinition
import org.magic.magicaddons.data.greenhouse.crops.CropRegistry
import org.magic.magicaddons.data.greenhouse.crops.Plant
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout

object MagicAddonsFormat : LayoutFormat {

    override val displayName: String = "MagicAddons"

    private const val CODE_PREFIX: String = "MAGH"
    private const val CODE_VERSION: Int = 1
    private const val FIELD_SEPARATOR: Char = '|'
    private const val PAYLOAD_VERSION: Int = 1
    private const val UNABLE_TO_IMPORT_MESSAGE: String = "Unable to import this layout."

    private const val MAX_DISTINCT_CROPS: Int = 255
    private const val MAX_DISTINCT_SOILS: Int = 62

    private const val SOIL_UNSET: Int = 0
    private const val SOIL_AIR: Int = 1
    private const val SOIL_FIRST: Int = 2

    override fun canImport(text: String): Boolean = text.trim().startsWith(CODE_PREFIX)

    override fun import(text: String, layoutId: String): LayoutTransferResult {
        val trimmed = text.trim()
        if (trimmed.substringBefore(FIELD_SEPARATOR) != "$CODE_PREFIX$CODE_VERSION") return LayoutTransferResult.Failure(UNABLE_TO_IMPORT_MESSAGE)

        val afterPrefix = trimmed.substringAfter(FIELD_SEPARATOR, "")
        val encodedPayload = afterPrefix.substringAfterLast(FIELD_SEPARATOR)
        val codeLabel = if (afterPrefix.contains(FIELD_SEPARATOR)) afterPrefix.substringBeforeLast(FIELD_SEPARATOR).trim().takeIf { it.isNotEmpty() } else null

        val payload = RawDeflate.decode(encodedPayload) ?: return LayoutTransferResult.Failure(UNABLE_TO_IMPORT_MESSAGE)

        return runCatching { readPayload(payload, layoutId, codeLabel) }.getOrElse { LayoutTransferResult.Failure(UNABLE_TO_IMPORT_MESSAGE) }
    }

    private fun readPayload(payload: ByteArray, layoutId: String, codeLabel: String?): LayoutTransferResult {
        val input = DataInputStream(ByteArrayInputStream(payload))
        val payloadVersion = input.readUnsignedByte()
        if (payloadVersion != PAYLOAD_VERSION) return LayoutTransferResult.Failure(UNABLE_TO_IMPORT_MESSAGE)

        val notes = mutableListOf<String>()
        val storedName = input.readUTF().takeIf { it.isNotEmpty() }
        val presetName = codeLabel ?: storedName
        val gridSize = input.readUnsignedByte()

        val cropTable = List(input.readUnsignedByte()) { input.readUTF() }.map { name ->
            CropRegistry.findByIdOrNameIgnoringCase(name)
                .also { if (it == null) notes.add("Unknown crop: $name") }
        }
        val soilTable = List(input.readUnsignedByte()) { input.readUTF() }.map { id ->
            runCatching { BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElse(null) }.getOrNull()
                .also { if (it == null) notes.add("Unknown soil: $id") }
        }

        val plotCount = input.readUnsignedByte()
        val plots = List(plotCount) { index ->
            val plotName = input.readUTF().takeIf { it.isNotEmpty() }
            val layout = PlotLayout(id = GreenhouseLayout.plotId(layoutId, index), name = plotName)

            for (cell in 0 until gridSize * gridSize) {
                val cropIndex = input.readUnsignedByte()
                val cellFlags = input.readUnsignedByte()
                val mergedAlternatives = if (cropIndex > 0) {
                    List(input.readUnsignedByte()) { cropTable.getOrNull(input.readUnsignedByte() - 1) }.filterNotNull()
                } else {
                    emptyList()
                }
                val slot = layout.getSlot(cell % gridSize, cell / gridSize) ?: continue

                slot.mark = LayoutSlot.Marking.entries.getOrNull((cellFlags and 0b11) - 1)
                when (val soilCode = cellFlags shr 2) {
                    SOIL_UNSET -> {}
                    SOIL_AIR -> slot.soil = Blocks.AIR
                    else -> soilTable.getOrNull(soilCode - SOIL_FIRST)?.let { slot.soil = it }
                }
                if (cropIndex > 0) cropTable.getOrNull(cropIndex - 1)?.let { addImportedPlant(layout, it, slot, notes, mergedAlternatives) }
            }
            layout
        }.take(GreenhouseLayout.MAX_PLOTS)

        if (input.available() > 0) return LayoutTransferResult.Failure(UNABLE_TO_IMPORT_MESSAGE)

        if (plots.isEmpty()) return LayoutTransferResult.Failure("That MagicAddons layout has no plots.")
        if (plotCount > GreenhouseLayout.MAX_PLOTS) notes.add("Only the first ${GreenhouseLayout.MAX_PLOTS} plots were taken.")
        return LayoutTransferResult.Imported(plots.first(), notes, plots.drop(1), presetName)
    }

    private fun addImportedPlant(
        layout: PlotLayout,
        definition: CropDefinition,
        slot: LayoutSlot,
        notes: MutableList<String>,
        mergedAlternatives: List<CropDefinition>
    ) {
        val footprint = definition.footprint
        if (slot.x + footprint.width > layout.size || slot.y + footprint.height > layout.size) {
            notes.add("${definition.name} at ${slot.x},${slot.y} does not fit the grid")
            return
        }
        for (dx in 0 until footprint.width) {
            for (dy in 0 until footprint.height) {
                val covered = layout.getSlot(slot.x + dx, slot.y + dy) ?: continue
                if (covered.soil == null) covered.soil = definition.requiredSoil.firstOrNull()
            }
        }
        layout.plants.add(
            Plant(definition.elementId, slot, cropDef = definition, presetAlternatives = mergedAlternatives.toMutableList())
        )
    }

    override fun export(layout: PlotLayout): LayoutTransferResult = writeCode(layout.name, listOf(layout))

    override fun exportAll(master: GreenhouseLayout): LayoutTransferResult = writeCode(master.name, master.plots)

    private fun writeCode(name: String?, plots: List<PlotLayout>): LayoutTransferResult {
        val cropTable = plots.flatMap { plot -> plot.plants.flatMap { it.acceptedCrops } }.distinct()
        val soilTable = plots.flatMap { plot -> plot.slots.mapNotNull { it.soil } }.filter { it != Blocks.AIR }.distinct()
        if (cropTable.size > MAX_DISTINCT_CROPS || soilTable.size > MAX_DISTINCT_SOILS) return LayoutTransferResult.Failure("Too many different crops or soils for a MagicAddons layout.")

        val bytes = ByteArrayOutputStream()
        val out = DataOutputStream(bytes)
        out.writeByte(PAYLOAD_VERSION)
        out.writeUTF(name ?: "")
        out.writeByte(plots.first().size)
        out.writeByte(cropTable.size)
        cropTable.forEach { out.writeUTF(it.name) }
        out.writeByte(soilTable.size)
        soilTable.forEach { out.writeUTF(blockIdOf(it)) }
        out.writeByte(plots.size)

        plots.forEach { plot ->
            out.writeUTF(plot.name ?: "")
            val plantByOrigin = plot.plants.associateBy { it.slot.x to it.slot.y }
            for (cell in 0 until plot.size * plot.size) {
                val x = cell % plot.size
                val y = cell / plot.size
                val slot = plot.getSlot(x, y)
                val plant = plantByOrigin[x to y]
                out.writeByte(plant?.let { cropTable.indexOf(it.cropDef) + 1 } ?: 0)

                val mark = slot?.mark?.let { it.ordinal + 1 } ?: 0
                val soilBlock = slot?.soil
                val soilCode = when {
                    soilBlock == null -> SOIL_UNSET
                    soilBlock == Blocks.AIR -> SOIL_AIR
                    else -> soilTable.indexOf(soilBlock) + SOIL_FIRST
                }
                out.writeByte(mark or (soilCode shl 2))

                if (plant != null) {
                    out.writeByte(plant.presetAlternatives.size)
                    plant.presetAlternatives.forEach { out.writeByte(cropTable.indexOf(it) + 1) }
                }
            }
        }

        val code = RawDeflate.encode(bytes.toByteArray())
        val label = (name ?: "").replace(FIELD_SEPARATOR, ' ').trim()
        return LayoutTransferResult.Exported("$CODE_PREFIX$CODE_VERSION$FIELD_SEPARATOR$label$FIELD_SEPARATOR$code")
    }

    private fun blockIdOf(block: Block): String = BuiltInRegistries.BLOCK.getKey(block).toString()
}
