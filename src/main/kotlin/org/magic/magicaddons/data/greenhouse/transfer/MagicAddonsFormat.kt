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
import org.magic.magicaddons.data.greenhouse.plot.ExitCondition
import org.magic.magicaddons.data.greenhouse.plot.FlowStep
import org.magic.magicaddons.data.greenhouse.plot.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.plot.InputSpawns
import org.magic.magicaddons.data.greenhouse.plot.LayoutSlot
import org.magic.magicaddons.data.greenhouse.plot.PlotFlow
import org.magic.magicaddons.data.greenhouse.plot.PlotLayout
import org.magic.magicaddons.data.greenhouse.plot.StepExit
import org.magic.magicaddons.data.greenhouse.plot.StepPolicies

object MagicAddonsFormat : LayoutFormat {

    override val displayName: String = "MagicAddons"

    override val carriesSteps: Boolean = true

    private const val CODE_PREFIX: String = "MAGH"
    private const val CODE_VERSION: Int = 1
    private const val FIELD_SEPARATOR: Char = '|'
    private const val PAYLOAD_VERSION: Int = 2
    private const val FIRST_PAYLOAD_VERSION_WITH_STEPS: Int = 2
    private const val UNABLE_TO_IMPORT_MESSAGE: String = "Unable to import this layout."
    private const val NEWER_CODE_MESSAGE: String = "This layout comes from a newer MagicAddons; update the mod to import it."

    private const val MAX_DISTINCT_CROPS: Int = 255
    private const val MAX_DISTINCT_SOILS: Int = 62

    private const val SOIL_UNSET: Int = 0
    private const val SOIL_AIR: Int = 1
    private const val SOIL_FIRST: Int = 2

    private const val KIND_SPAWNED: Int = 1
    private const val KIND_STAGE: Int = 2
    private const val KIND_HARVESTED: Int = 3

    private const val POLICY_FULL_CLEAR: Int = 1
    private const val POLICY_KEEP_IDENTICAL: Int = 2
    private const val POLICY_HARVEST_INPUT_SPAWNS: Int = 4

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

    private class Tables(val crops: List<CropDefinition?>, val soils: List<Block?>)

    private fun readPayload(payload: ByteArray, layoutId: String, codeLabel: String?): LayoutTransferResult {
        val input = DataInputStream(ByteArrayInputStream(payload))
        val payloadVersion = input.readUnsignedByte()
        if (payloadVersion > PAYLOAD_VERSION) return LayoutTransferResult.Failure(NEWER_CODE_MESSAGE)
        if (payloadVersion < 1) return LayoutTransferResult.Failure(UNABLE_TO_IMPORT_MESSAGE)

        val storedName = input.readUTF().takeIf { it.isNotEmpty() }
        val presetName = codeLabel ?: storedName
        val gridSize = input.readUnsignedByte()

        val tables = Tables(
            crops = List(input.readUnsignedByte()) { input.readUTF() }.map { name ->
                CropRegistry.findByIdOrNameIgnoringCase(name)
            },
            soils = List(input.readUnsignedByte()) { input.readUTF() }.map { id ->
                runCatching { BuiltInRegistries.BLOCK.getOptional(Identifier.parse(id)).orElse(null) }.getOrNull()
            }
        )

        val plotCount = input.readUnsignedByte()
        val plots = List(plotCount) { index ->
            val plotName = input.readUTF().takeIf { it.isNotEmpty() }
            val layout = PlotLayout(id = GreenhouseLayout.plotId(layoutId, index), name = plotName)
            readCells(input, layout, gridSize, tables)
            if (payloadVersion >= FIRST_PAYLOAD_VERSION_WITH_STEPS) layout.flow = readFlow(input, layout, gridSize, tables)
            layout
        }.take(GreenhouseLayout.MAX_PLOTS)

        if (input.available() > 0) return LayoutTransferResult.Failure(UNABLE_TO_IMPORT_MESSAGE)

        if (plots.isEmpty()) return LayoutTransferResult.Failure("That MagicAddons layout has no plots.")
        return LayoutTransferResult.Imported(plots.first(), plots.drop(1), presetName)
    }

    private fun readCells(input: DataInputStream, layout: PlotLayout, gridSize: Int, tables: Tables) {
        for (cell in 0 until gridSize * gridSize) {
            val cropIndex = input.readUnsignedByte()
            val cellFlags = input.readUnsignedByte()
            val mergedAlternatives = if (cropIndex > 0) {
                List(input.readUnsignedByte()) { tables.crops.getOrNull(input.readUnsignedByte() - 1) }.filterNotNull()
            } else {
                emptyList()
            }
            val slot = layout.getSlot(cell % gridSize, cell / gridSize) ?: continue

            slot.mark = LayoutSlot.Marking.entries.getOrNull((cellFlags and 0b11) - 1)
            when (val soilCode = cellFlags shr 2) {
                SOIL_UNSET -> {}
                SOIL_AIR -> slot.soil = Blocks.AIR
                else -> tables.soils.getOrNull(soilCode - SOIL_FIRST)?.let { slot.soil = it }
            }
            if (cropIndex > 0) tables.crops.getOrNull(cropIndex - 1)?.let { addImportedPlant(layout, it, slot, mergedAlternatives) }
        }
    }

    private fun readFlow(input: DataInputStream, plot: PlotLayout, gridSize: Int, tables: Tables): PlotFlow? {
        val loop = input.readBoolean()
        val stepCount = input.readUnsignedByte()
        if (stepCount == 0) return null

        val steps = List(stepCount) { index ->
            val label = input.readUTF().takeIf { it.isNotEmpty() }
            val exits = List(input.readUnsignedByte()) {
                StepExit(List(input.readUnsignedByte()) { readCondition(input, tables) }.filterNotNull())
            }
            val policyFlags = input.readUnsignedByte()
            val policies = StepPolicies(
                fullClear = policyFlags and POLICY_FULL_CLEAR != 0,
                keepIdentical = policyFlags and POLICY_KEEP_IDENTICAL != 0,
                inputSpawns = if (policyFlags and POLICY_HARVEST_INPUT_SPAWNS != 0) InputSpawns.Harvest else InputSpawns.Keep
            )
            val layout = if (index == 0) plot else PlotLayout(id = plot.stepId(index), name = label).also { readCells(input, it, gridSize, tables) }
            FlowStep(label, layout, exits, policies)
        }
        return if (steps.size < 2) null else PlotFlow(loop, steps.toMutableList())
    }

    private fun readCondition(input: DataInputStream, tables: Tables): ExitCondition? {
        val kind = input.readUnsignedByte()
        val crop = tables.crops.getOrNull(input.readUnsignedByte() - 1)
        val amount = input.readUnsignedShort()
        if (crop == null) return null
        return when (kind) {
            KIND_SPAWNED -> ExitCondition.MutationSpawned(crop, amount)
            KIND_STAGE -> ExitCondition.HighestStageAtLeast(crop, amount)
            KIND_HARVESTED -> ExitCondition.MutationHarvested(crop, amount)
            else -> null
        }
    }

    private fun addImportedPlant(
        layout: PlotLayout,
        definition: CropDefinition,
        slot: LayoutSlot,
        mergedAlternatives: List<CropDefinition>
    ) {
        val footprint = definition.footprint
        if (slot.x + footprint.width > layout.size || slot.y + footprint.height > layout.size) return
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
        val stepLayouts = plots.flatMap { plot -> plot.steps.map { it.layout } }
        val exitCrops = plots.flatMap { plot -> plot.steps.flatMap { step -> step.exits.flatMap { exit -> exit.conditions.map { it.crop } } } }
        val cropTable = (stepLayouts.flatMap { layout -> layout.plants.flatMap { it.acceptedCrops } } + exitCrops).distinct()
        val soilTable = stepLayouts.flatMap { layout -> layout.slots.mapNotNull { it.soil } }.filter { it != Blocks.AIR }.distinct()
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
            writeCells(out, plot, cropTable, soilTable)
            writeFlow(out, plot.flow, cropTable, soilTable)
        }

        val code = RawDeflate.encode(bytes.toByteArray())
        val label = (name ?: "").replace(FIELD_SEPARATOR, ' ').trim()
        return LayoutTransferResult.Exported("$CODE_PREFIX$CODE_VERSION$FIELD_SEPARATOR$label$FIELD_SEPARATOR$code")
    }

    private fun writeCells(out: DataOutputStream, plot: PlotLayout, cropTable: List<CropDefinition>, soilTable: List<Block>) {
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

    private fun writeFlow(out: DataOutputStream, flow: PlotFlow?, cropTable: List<CropDefinition>, soilTable: List<Block>) {
        out.writeBoolean(flow?.loop ?: false)
        out.writeByte(flow?.steps?.size ?: 0)
        flow ?: return

        flow.steps.forEachIndexed { index, step ->
            out.writeUTF(step.label ?: "")
            out.writeByte(step.exits.size)
            step.exits.forEach { exit ->
                out.writeByte(exit.conditions.size)
                exit.conditions.forEach { condition ->
                    val (kind, amount) = when (condition) {
                        is ExitCondition.MutationSpawned -> KIND_SPAWNED to condition.count
                        is ExitCondition.HighestStageAtLeast -> KIND_STAGE to condition.stage
                        is ExitCondition.MutationHarvested -> KIND_HARVESTED to condition.count
                    }
                    out.writeByte(kind)
                    out.writeByte(cropTable.indexOf(condition.crop) + 1)
                    out.writeShort(amount)
                }
            }
            var policyFlags = 0
            if (step.policies.fullClear) policyFlags = policyFlags or POLICY_FULL_CLEAR
            if (step.policies.keepIdentical) policyFlags = policyFlags or POLICY_KEEP_IDENTICAL
            if (step.policies.inputSpawns == InputSpawns.Harvest) policyFlags = policyFlags or POLICY_HARVEST_INPUT_SPAWNS
            out.writeByte(policyFlags)
            if (index > 0) writeCells(out, step.layout, cropTable, soilTable)
        }
    }

    private fun blockIdOf(block: Block): String = BuiltInRegistries.BLOCK.getKey(block).toString()
}
