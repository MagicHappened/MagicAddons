package org.magic.magicaddons.data.greenhouse.transfer

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import org.magic.magicaddons.data.greenhouse.CropDefinition
import org.magic.magicaddons.data.greenhouse.CropRegistry
import org.magic.magicaddons.data.greenhouse.GreenhouseElementInstance
import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.LayoutSlot
import org.magic.magicaddons.data.greenhouse.MasterLayout
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * This mod's own way of sharing a preset: one short line for chat, `MAGH1|name|data`. The name is
 * a label the importer takes as the preset's name. Importing also still reads the json this mod
 * wrote before. The data is the bytes below, deflated (raw, no zlib header) and base64url encoded
 * without padding:
 *
 * ```
 * u8    payload version (1)
 * utf   preset name, empty for none            (java modified utf, u16 length first)
 * u8    grid size
 * u8    crop count, then that many utf crop names
 * u8    soil count, then that many utf block ids such as minecraft:sand
 * u8    plot count, then per plot:
 *   utf plot name, empty for none
 *   size*size cells, row by row, two bytes each:
 *     u8 crop: 0 for none, else the crop's index plus one, on the plant's top left cell only
 *     u8 flags: bits 0-1 the mark (0 none, 1 target, 2 ingredient), bits 2-7 the soil
 *              (0 unset, 1 air required, else the soil's index plus two)
 * ```
 */
object ShareCodeFormat : LayoutFormat {

    override val displayName: String = "MagicAddons"

    private const val PREFIX: String = "MAGH"
    private const val WRAPPER_VERSION: Int = 1
    private const val SEPARATOR: Char = '|'
    private const val PAYLOAD_VERSION: Int = 1

    private const val SOIL_UNSET: Int = 0
    private const val SOIL_AIR: Int = 1
    private const val SOIL_FIRST: Int = 2

    override fun canImport(text: String): Boolean = text.trim().startsWith(PREFIX) || MagicAddonsFormat.canImport(text)

    override fun import(text: String, layoutId: String): LayoutTransferResult {
        val trimmed = text.trim()
        // the json of earlier versions still comes in
        if (!trimmed.startsWith(PREFIX)) return MagicAddonsFormat.import(text, layoutId)

        val head = trimmed.substringBefore(SEPARATOR)
        val wrapper = head.removePrefix(PREFIX).toIntOrNull() ?: return LayoutTransferResult.Failure("That share code has no version.")
        if (wrapper > WRAPPER_VERSION) return LayoutTransferResult.Failure("That share code was written by a newer version of the mod.")

        val rest = trimmed.substringAfter(SEPARATOR, "")
        val data = rest.substringAfterLast(SEPARATOR)
        val label = if (rest.contains(SEPARATOR)) rest.substringBeforeLast(SEPARATOR).trim().takeIf { it.isNotEmpty() } else null

        val bytes = runCatching { inflate(Base64.getUrlDecoder().decode(data)) }.getOrNull()
            ?: return LayoutTransferResult.Failure("That share code is damaged.")

        return runCatching { read(bytes, layoutId, label) }.getOrElse { LayoutTransferResult.Failure("That share code is damaged.") }
    }

    private fun read(bytes: ByteArray, layoutId: String, label: String?): LayoutTransferResult {
        val input = DataInputStream(ByteArrayInputStream(bytes))
        val version = input.readUnsignedByte()
        if (version > PAYLOAD_VERSION) return LayoutTransferResult.Failure("That share code was written by a newer version of the mod.")

        val notes = mutableListOf<String>()
        val storedName = input.readUTF().takeIf { it.isNotEmpty() }
        val presetName = label ?: storedName
        val size = input.readUnsignedByte()

        val crops = List(input.readUnsignedByte()) { input.readUTF() }.map { name ->
            (CropRegistry.get(name) ?: CropRegistry.all.find { it.name.equals(name, ignoreCase = true) })
                .also { if (it == null) notes.add("Unknown crop: $name") }
        }
        val soils = List(input.readUnsignedByte()) { input.readUTF() }.map { id ->
            runCatching { BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElse(null) }.getOrNull()
                .also { if (it == null) notes.add("Unknown soil: $id") }
        }

        val plotCount = input.readUnsignedByte()
        val plots = List(plotCount) { index ->
            val plotName = input.readUTF().takeIf { it.isNotEmpty() }
            val id = if (index == 0) layoutId else "${layoutId}_p${index + 1}"
            val layout = GreenhouseLayout(id = id, name = plotName)

            for (cell in 0 until size * size) {
                val crop = input.readUnsignedByte()
                val flags = input.readUnsignedByte()
                val x = cell % size
                val y = cell / size
                val slot = layout.getSlot(x, y) ?: continue

                slot.slotMark = LayoutSlot.Marking.entries.getOrNull((flags and 0b11) - 1)
                when (val soil = flags shr 2) {
                    SOIL_UNSET -> {}
                    SOIL_AIR -> slot.placedBlock = Blocks.AIR.defaultBlockState()
                    else -> soils.getOrNull(soil - SOIL_FIRST)?.let { slot.placedBlock = it.defaultBlockState() }
                }
                if (crop > 0) crops.getOrNull(crop - 1)?.let { plant(layout, it, slot, notes) }
            }
            layout
        }.take(MasterLayout.MAX_PLOTS)

        if (plots.isEmpty()) return LayoutTransferResult.Failure("That share code holds no plots.")
        if (plotCount > MasterLayout.MAX_PLOTS) notes.add("Only the first ${MasterLayout.MAX_PLOTS} plots were taken.")
        return LayoutTransferResult.Imported(plots.first(), notes, plots.drop(1), presetName)
    }

    /** Puts [definition] down with its top left on [slot], its own soil under any cell not given one. */
    private fun plant(layout: GreenhouseLayout, definition: CropDefinition, slot: LayoutSlot, notes: MutableList<String>) {
        val footprint = definition.footprint
        if (slot.x + footprint.width > layout.size || slot.y + footprint.height > layout.size) {
            notes.add("${definition.name} at ${slot.x},${slot.y} does not fit the grid")
            return
        }
        for (dx in 0 until footprint.width) {
            for (dy in 0 until footprint.height) {
                val covered = layout.getSlot(slot.x + dx, slot.y + dy) ?: continue
                if (covered.placedBlock == null) covered.placedBlock = definition.requiredSoil.firstOrNull()?.defaultBlockState()
            }
        }
        layout.elementInstances.add(GreenhouseElementInstance(definition.skyblockId?.id ?: definition.name, slot, cropDef = definition))
    }

    override fun export(layout: GreenhouseLayout): LayoutTransferResult = write(layout.name, listOf(layout))

    override fun exportAll(master: MasterLayout): LayoutTransferResult = write(master.name, master.plots)

    private fun write(name: String?, plots: List<GreenhouseLayout>): LayoutTransferResult {
        val crops = plots.flatMap { plot -> plot.elementInstances.map { it.cropDef } }.distinct()
        val soils = plots.flatMap { plot -> plot.slots.mapNotNull { it.placedBlock?.block } }.filter { it != Blocks.AIR }.distinct()
        if (crops.size > 255 || soils.size > 250) return LayoutTransferResult.Failure("Too many different crops or soils for a share code.")

        val bytes = ByteArrayOutputStream()
        val out = DataOutputStream(bytes)
        out.writeByte(PAYLOAD_VERSION)
        out.writeUTF(name ?: "")
        out.writeByte(plots.first().size)
        out.writeByte(crops.size)
        crops.forEach { out.writeUTF(it.name) }
        out.writeByte(soils.size)
        soils.forEach { out.writeUTF(idOf(it)) }
        out.writeByte(plots.size)

        plots.forEach { plot ->
            out.writeUTF(plot.name ?: "")
            val origins = plot.elementInstances.associateBy { it.slot.x to it.slot.y }
            for (cell in 0 until plot.size * plot.size) {
                val x = cell % plot.size
                val y = cell / plot.size
                val slot = plot.getSlot(x, y)
                val plant = origins[x to y]
                out.writeByte(plant?.let { crops.indexOf(it.cropDef) + 1 } ?: 0)

                val mark = slot?.slotMark?.let { it.ordinal + 1 } ?: 0
                val block = slot?.placedBlock?.block
                val soil = when {
                    block == null -> SOIL_UNSET
                    block == Blocks.AIR -> SOIL_AIR
                    else -> soils.indexOf(block) + SOIL_FIRST
                }
                out.writeByte(mark or (soil shl 2))
            }
        }

        val code = Base64.getUrlEncoder().withoutPadding().encodeToString(deflate(bytes.toByteArray()))
        val label = (name ?: "").replace(SEPARATOR, ' ').trim()
        return LayoutTransferResult.Exported("$PREFIX$WRAPPER_VERSION$SEPARATOR$label$SEPARATOR$code")
    }

    private fun idOf(block: Block): String = BuiltInRegistries.BLOCK.getKey(block).toString()

    private fun deflate(bytes: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(bytes)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
        deflater.end()
        return out.toByteArray()
    }

    private fun inflate(bytes: ByteArray): ByteArray {
        val inflater = Inflater(true)
        inflater.setInput(bytes)
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0 && inflater.needsInput()) break
            out.write(buffer, 0, count)
        }
        inflater.end()
        return out.toByteArray()
    }
}
