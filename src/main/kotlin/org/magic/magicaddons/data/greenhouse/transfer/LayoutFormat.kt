package org.magic.magicaddons.data.greenhouse.transfer

import org.magic.magicaddons.data.greenhouse.GreenhouseLayout
import org.magic.magicaddons.data.greenhouse.MasterLayout

/**
 * One way of writing a layout down so it can leave the game and come back. Formats deal in text,
 * not the clipboard, so they can be exercised without a running client.
 */
interface LayoutFormat {

    /** What this format is called where the player picks it. */
    val displayName: String

    /** Whether [text] looks like this format, used to say so when the clipboard holds something else. */
    fun canImport(text: String): Boolean

    /** Reads [text] into a new plot filed under [layoutId], with any further plots the text carries. */
    fun import(text: String, layoutId: String): LayoutTransferResult

    /** Writes one plot out as the text a player can share. */
    fun export(layout: GreenhouseLayout): LayoutTransferResult

    /** Writes every plot of a preset; a format that holds one plot writes the first. */
    fun exportAll(master: MasterLayout): LayoutTransferResult = export(master.plots.first())
}

/** What came of a transfer. Notes carry whatever the player should know that did not stop it. */
sealed interface LayoutTransferResult {

    val notes: List<String>

    data class Imported(
        val layout: GreenhouseLayout,
        override val notes: List<String> = emptyList(),
        /** Plots after the first, when the text held more than one. */
        val extraPlots: List<GreenhouseLayout> = emptyList()
    ) : LayoutTransferResult {
        val plots: List<GreenhouseLayout> get() = listOf(layout) + extraPlots
    }

    data class Exported(
        val text: String,
        override val notes: List<String> = emptyList()
    ) : LayoutTransferResult

    /** The transfer could not happen at all, [reason] says why in words for the player. */
    data class Failure(
        val reason: String,
        override val notes: List<String> = emptyList()
    ) : LayoutTransferResult
}
