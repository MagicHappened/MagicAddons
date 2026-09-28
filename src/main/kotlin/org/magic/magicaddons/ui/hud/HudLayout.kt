package org.magic.magicaddons.ui.hud

import com.google.gson.GsonBuilder
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.handlers.ModFiles
import org.magic.magicaddons.data.handlers.ModFiles.LoadResult
import org.magic.magicaddons.ui.ScreenRect

enum class Positioning { ABSOLUTE, RELATIVE }

enum class Stacking { VERTICAL, HORIZONTAL }

class PlacementTie(var target: String = "", var offsetX: Int = 0, var offsetY: Int = 0)

abstract class HudPlacement {
    var positioning: Positioning = Positioning.ABSOLUTE

    var x: Int = 0
    var y: Int = 0

    var fractionX: Float = 0f
    var fractionY: Float = 0f

    var tie: PlacementTie? = null

    var alpha: Float = 1f

    fun placeRect(rect: ScreenRect, screenWidth: Int, screenHeight: Int) {
        x = rect.x
        y = rect.y
        fractionX = fractionOfAvailableRoom(rect.x, screenWidth - rect.width)
        fractionY = fractionOfAvailableRoom(rect.y, screenHeight - rect.height)
    }

    private fun fractionOfAvailableRoom(position: Int, availableRoom: Int): Float =
        if (availableRoom <= 0) 0f else (position.toFloat() / availableRoom).coerceIn(0f, 1f)
}

abstract class SizedPlacement : HudPlacement() {
    var isSizedByContent: Boolean = true

    var width: Int? = null
    var height: Int? = null
}

class ElementState : SizedPlacement() {
    var scale: Float = 1f
}

class AnchorState(val id: String = "") : HudPlacement() {
    init {
        alpha = 0.6f
    }
}

class GroupState(
    val id: String = "",
    val members: MutableList<String> = mutableListOf(),
    var stacking: Stacking = Stacking.VERTICAL
) : SizedPlacement()

class HudLayout {
    val elements: MutableMap<String, ElementState> = mutableMapOf()
    val anchors: MutableList<AnchorState> = mutableListOf()
    val groups: MutableList<GroupState> = mutableListOf()

    val hidden: MutableMap<String, MutableSet<String>> = mutableMapOf()

    fun isHidden(situation: HudSituation, elementId: String): Boolean = hidden[situation.name]?.contains(elementId) == true

    fun setHidden(situation: HudSituation, elementId: String, isHidden: Boolean) {
        val hiddenElementIds = hidden.getOrPut(situation.name) { mutableSetOf() }
        if (isHidden) hiddenElementIds.add(elementId) else hiddenElementIds.remove(elementId)
        if (hiddenElementIds.isEmpty()) hidden.remove(situation.name)
    }

    fun elementStateOf(element: HudElement): ElementState = elements.getOrPut(element.id) { defaultStateOf(element) }

    fun defaultStateOf(element: HudElement): ElementState = ElementState().also {
        it.x = element.defaultX
        it.y = element.defaultY
        it.alpha = element.defaultAlpha
    }

    fun groupContaining(elementId: String): GroupState? = groups.firstOrNull { elementId in it.members }

    fun anchorById(id: String): AnchorState? = anchors.firstOrNull { it.id == id }

    fun groupById(id: String): GroupState? = groups.firstOrNull { it.id == id }

    fun placementById(id: String): HudPlacement? = elements[id] ?: groupById(id) ?: anchorById(id)

    fun addAnchor(): AnchorState {
        val id = nextFreeId("anchor", anchors.map { it.id })
        return AnchorState(id).also { anchors.add(it) }
    }

    fun addGroup(members: List<String>, stacking: Stacking): GroupState {
        val id = nextFreeId("group", groups.map { it.id })
        return GroupState(id, members.toMutableList(), stacking).also { groups.add(it) }
    }

    private fun nextFreeId(prefix: String, takenIds: List<String>): String {
        var number = 1
        while ("$prefix-$number" in takenIds) number++
        return "$prefix-$number"
    }

    fun wouldTieLoop(id: String, target: String): Boolean {
        var currentId: String? = target
        var steps = 0
        while (currentId != null && steps++ < MAX_TIE_CHAIN) {
            if (currentId == id) return true
            currentId = placementById(currentId)?.tie?.target
        }
        return false
    }

    fun untieEverythingFrom(id: String, rectById: Map<String, ScreenRect>, screenWidth: Int, screenHeight: Int) {
        (elements.values + groups + anchors).forEach { placement ->
            if (placement.tie?.target != id) return@forEach
            val placementId = when (placement) {
                is AnchorState -> placement.id
                is GroupState -> placement.id
                else -> elements.entries.first { it.value === placement }.key
            }
            rectById[placementId]?.let { placement.placeRect(it, screenWidth, screenHeight) }
            placement.tie = null
        }
    }

    companion object {
        const val MAX_TIE_CHAIN: Int = 16
    }
}

object HudLayoutFile {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val layoutPath = ModFiles.modDir.resolve("hud.json")

    val layout: HudLayout by lazy { load() }

    private fun load(): HudLayout = when (val result = ModFiles.loadTextWithBackup(layoutPath) { readLayout(it) }) {
        is LoadResult.NoFile -> HudLayout()
        is LoadResult.Loaded -> {
            if (result.restoredFromBackup) Common.LOGGER.warn("The hud layout could not be read, restored it from its backup")
            result.value
        }
        is LoadResult.Unreadable -> {
            Common.LOGGER.warn("Could not read the hud layout, starting over", result.cause)
            HudLayout()
        }
    }

    private fun readLayout(text: String): HudLayout = gson.fromJson(text, HudLayout::class.java) ?: HudLayout()

    fun save() {
        runCatching { ModFiles.saveTextWithBackup(layoutPath, gson.toJson(layout)) { readLayout(it) } }
            .onFailure { Common.LOGGER.warn("Could not save the hud layout", it) }
    }
}
