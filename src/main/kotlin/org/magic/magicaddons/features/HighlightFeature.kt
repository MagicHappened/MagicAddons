package org.magic.magicaddons.features

import net.minecraft.world.entity.Entity
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.util.EntityUtils

abstract class HighlightFeature : Feature(), EntityUtils.HighlightSource {

    abstract fun highlightTarget(info: EntityInfo): Entity?

    private val targets: MutableMap<Entity, Entity> = mutableMapOf()

    private val marks: MutableMap<Entity, EntityUtils.HighlightMark> = mutableMapOf()

    open fun markOf(info: EntityInfo): EntityUtils.HighlightMark? = null

    final override fun highlightMark(entity: Entity): EntityUtils.HighlightMark? = marks[entity]

    fun invalidateHighlights() {
        EntityUtils.removeAllForSource(this)
        targets.clear()
        marks.clear()

        if (!baseSetting.value) return

        EntityUtils.entityInfoList?.forEach { info -> apply(info) }
    }

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
