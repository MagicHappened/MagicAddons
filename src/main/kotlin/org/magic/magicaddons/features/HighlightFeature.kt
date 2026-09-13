package org.magic.magicaddons.features

import net.minecraft.world.entity.Entity
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.util.EntityUtils

/** a feature that uses highlights */
abstract class HighlightFeature : Feature(), EntityUtils.HighlightSource {

    /** returns the entity to outline for the entity info bundle*/
    abstract fun highlightTarget(info: EntityInfo): Entity?

    private val targets: MutableMap<Entity, Entity> = mutableMapOf()

    /** what mark to draw if the entity is far away for each entity */
    private val marks: MutableMap<Entity, EntityUtils.HighlightMark> = mutableMapOf()

    /** what should the marker draw for this entity default null */
    open fun markOf(info: EntityInfo): EntityUtils.HighlightMark? = null

    final override fun highlightMark(entity: Entity): EntityUtils.HighlightMark? = marks[entity]

    fun invalidateHighlights() {
        EntityUtils.removeAllForSource(this)
        targets.clear()
        marks.clear()

        if (!baseSetting.value) return

        EntityUtils.entityInfoList?.forEach { info -> apply(info) }
    }

    /** Points this entity's highlight at whatever the feature now wants outlined, or at nothing. */
    private fun apply(info: EntityInfo) {
        val wanted = highlightTarget(info)
        val current = targets[info.entity]

        if (current === wanted) return

        if (current != null) {
            targets.remove(info.entity)
            releaseIfUnused(current)
        }

        if (wanted != null) {
            targets[info.entity] = wanted
            markOf(info)?.let { marks[wanted] = it }
            EntityUtils.add(wanted, this)
        }
    }

    /** Takes the outline off an entity, unless another match of ours is still pointing at it. */
    private fun releaseIfUnused(target: Entity) {
        if (targets.containsValue(target)) return

        marks.remove(target)
        EntityUtils.remove(target, this)
    }

    protected fun handleEntitiesAdded(entities: List<EntityInfo>) {
        if (!baseSetting.value) return

        entities.forEach { info -> apply(info) }
    }

    protected fun handleEntitiesRemoved(entities: List<EntityInfo>) {
        entities.forEach { info ->
            val target = targets.remove(info.entity) ?: return@forEach

            releaseIfUnused(target)
        }
    }

    protected fun handleEntitiesUpdated(entities: List<EntityInfo>) {
        if (!baseSetting.value) return

        entities.forEach { info -> apply(info) }
    }
}
