package org.magic.magicaddons.ui.hud

import com.google.gson.GsonBuilder
import org.magic.magicaddons.Common
import org.magic.magicaddons.data.handlers.DataHandler

enum class Positioning { ABSOLUTE, RELATIVE }

enum class Stacking { VERTICAL, HORIZONTAL }

/** A place given by another thing's top left corner plus an offset. */
class Tie(var target: String, var dx: Int, var dy: Int)

/** Something with a place on screen: an element, a group of them, or an anchor. */
abstract class Placed {
    var positioning: Positioning = Positioning.ABSOLUTE

    var x: Int = 0
    var y: Int = 0

    /** Where along the free room of each axis it sits, zero at one edge and one at the other. */
    var fx: Float = 0f
    var fy: Float = 0f

    var tie: Tie? = null

    /** Records a rectangle in every form, so switching how it is positioned keeps it where it is. */
    fun placeAt(left: Int, top: Int, width: Int, height: Int, screenWidth: Int, screenHeight: Int) {
        x = left
        y = top
        fx = fraction(left, screenWidth - width)
        fy = fraction(top, screenHeight - height)
    }

    private fun fraction(at: Int, room: Int): Float = if (room <= 0) 0f else (at.toFloat() / room).coerceIn(0f, 1f)
}

class ElementState : Placed() {
    /** Whether the box follows its content; off, [width] and [height] hold what it was dragged to. */
    var dynamic: Boolean = true

    var width: Int? = null
    var height: Int? = null
    var scale: Float = 1f
    var alpha: Float = 1f
}

class AnchorState(val id: String) : Placed() {
    /** How solid the anchor's box is drawn in the editor; it is never drawn in the game. */
    var alpha: Float = 0.6f
}

class GroupState(val id: String, val members: MutableList<String>, var stacking: Stacking) : Placed() {
    /** Whether the box follows its members' content; off, the sizes dragged to are kept. */
    var dynamic: Boolean = true

    var width: Int? = null
    var height: Int? = null
    var alpha: Float = 1f
}

/** Every element's state, the anchors, and the groups elements were merged into. */
class HudLayout {
    val elements: MutableMap<String, ElementState> = mutableMapOf()
    val anchors: MutableList<AnchorState> = mutableListOf()
    val groups: MutableList<GroupState> = mutableListOf()

    /** Elements the editor leaves out per situation, by the situation's name. */
    val hidden: MutableMap<String, MutableSet<String>> = mutableMapOf()

    fun isHidden(situation: HudSituation, elementId: String): Boolean = hidden[situation.name]?.contains(elementId) == true

    fun setHidden(situation: HudSituation, elementId: String, hide: Boolean) {
        val set = hidden.getOrPut(situation.name) { mutableSetOf() }
        if (hide) set.add(elementId) else set.remove(elementId)
        if (set.isEmpty()) hidden.remove(situation.name)
    }

    fun stateOf(element: HudElement): ElementState = elements.getOrPut(element.id) { defaults(element) }

    fun defaults(element: HudElement): ElementState = ElementState().also {
        it.x = element.defaultX
        it.y = element.defaultY
        it.alpha = element.defaultAlpha
    }

    fun groupOf(elementId: String): GroupState? = groups.firstOrNull { elementId in it.members }

    fun anchor(id: String): AnchorState? = anchors.firstOrNull { it.id == id }

    fun group(id: String): GroupState? = groups.firstOrNull { it.id == id }

    /** Whatever [id] names, an element, a group or an anchor. */
    fun placed(id: String): Placed? = elements[id] ?: group(id) ?: anchor(id)

    fun newAnchor(): AnchorState {
        val id = nextId("anchor", anchors.map { it.id })
        return AnchorState(id).also { anchors.add(it) }
    }

    fun newGroup(members: List<String>, stacking: Stacking): GroupState {
        val id = nextId("group", groups.map { it.id })
        return GroupState(id, members.toMutableList(), stacking).also { groups.add(it) }
    }

    private fun nextId(prefix: String, taken: List<String>): String {
        var n = 1
        while ("$prefix-$n" in taken) n++
        return "$prefix-$n"
    }

    /** Whether tying [id] to [target] would make something depend on itself. */
    fun wouldLoop(id: String, target: String): Boolean {
        var at: String? = target
        var steps = 0
        while (at != null && steps++ < 16) {
            if (at == id) return true
            at = placed(at)?.tie?.target
        }
        return false
    }

    /** Takes every tie to [id] off, leaving the tied things where they are. */
    fun untieFrom(id: String, positions: Map<String, IntArray>, screenWidth: Int, screenHeight: Int) {
        (elements.values + groups + anchors).forEach { placed ->
            if (placed.tie?.target != id) return@forEach
            val key = when (placed) {
                is AnchorState -> placed.id
                is GroupState -> placed.id
                else -> elements.entries.first { it.value === placed }.key
            }
            positions[key]?.let { placed.placeAt(it[0], it[1], it[2], it[3], screenWidth, screenHeight) }
            placed.tie = null
        }
    }
}

/** The layout on disk, magicaddons/hud.json under the config folder, read once and written after every edit. */
object HudLayoutStore {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file = DataHandler.modDir.resolve("hud.json").toFile()

    val layout: HudLayout by lazy { load() }

    private fun load(): HudLayout {
        if (!file.exists()) return HudLayout()
        return runCatching { gson.fromJson(file.readText(), HudLayout::class.java) ?: HudLayout() }
            .onFailure { Common.LOGGER.warn("Could not read the hud layout, starting over", it) }
            .getOrDefault(HudLayout())
    }

    fun save() {
        runCatching {
            file.parentFile.mkdirs()
            file.writeText(gson.toJson(layout))
        }.onFailure { Common.LOGGER.warn("Could not save the hud layout", it) }
    }
}
