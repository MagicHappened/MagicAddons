package org.magic.magicaddons.features

import net.minecraft.world.entity.Entity
import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.util.EntityUtils

/**
 * A feature that contributes entity outlines. Subclasses decide which entity a match should outline
 * and in what colour; keeping EntityUtils in step is owned here.
 *
 * Event wiring stays in the subclass, since EventBus only scans declared methods.
 */
abstract class HighlightFeature : Feature(), EntityUtils.HighlightSource {

    /**
     * The entity to outline for this match, or null when nothing here matched.
     *
     * Usually the entity itself, but not always: a rat is an invisible zombie whose skull is a
     * separate item display, so the match is made on the display and the display is what should be
     * drawn. Whatever is returned is also what [EntityUtils.HighlightSource.highlightColor] is asked
     * about.
     */
    abstract fun highlightTarget(info: EntityInfo): Entity?

    /** What each matched entity is currently outlining, so a match that moves can be cleaned up. */
    private val targets: MutableMap<Entity, Entity> = mutableMapOf()

    /** Drops every highlight owned by this feature and rebuilds it from the current entity list. */
    fun invalidateHighlights() {
        EntityUtils.removeAllForSource(this)
        targets.clear()

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
            EntityUtils.add(wanted, this)
        }
    }

    /** Takes the outline off an entity, unless another match of ours is still pointing at it. */
    private fun releaseIfUnused(target: Entity) {
        if (targets.containsValue(target)) return

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
