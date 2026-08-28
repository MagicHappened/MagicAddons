package org.magic.magicaddons.data.greenhouse.transfer

import org.magic.magicaddons.data.greenhouse.GreenhouseLayout

/**
 * One way of writing a greenhouse layout down so it can leave the game and come back.
 *
 * The formats deal in text rather than in the clipboard, so what carries a layout is the caller's
 * business and a format can be exercised without a running client.
 */
interface LayoutFormat {

    /** What this format is called where the player picks it. */
    val displayName: String

    /** Whether [text] looks like this format, used to say so when the clipboard holds something else. */
    fun canImport(text: String): Boolean

    /** Reads [text] into a new layout filed under [layoutId]. */
    fun import(text: String, layoutId: String): LayoutTransferResult

    /** Writes [layout] out as the text a player can share. */
    fun export(layout: GreenhouseLayout): LayoutTransferResult
}

/**
 * What came of a transfer. [notes] carries whatever the player should know that did not stop the
 * transfer, such as a plant the other side has no name for.
 */
sealed interface LayoutTransferResult {

    val notes: List<String>

    data class Imported(
        val layout: GreenhouseLayout,
        override val notes: List<String> = emptyList()
    ) : LayoutTransferResult

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
