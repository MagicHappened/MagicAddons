package org.magic.magicaddons.features

import org.magic.magicaddons.data.EntityInfo
import org.magic.magicaddons.util.EntityUtils

/**
 * A feature that contributes entity outlines.
 *
 * Subclasses only decide *what* is highlighted ([shouldHighlight]) and *how* it is colored
 * ([EntityUtils.HighlightSource.highlightColor]); keeping [EntityUtils] in sync with those answers
 * is owned here.
 *
 * Event wiring still lives in the subclass because [org.magic.magicaddons.events.EventBus] only
 * scans declared methods, so an inherited `@EventHandler` would never be found. Subclasses declare
 * the handlers and delegate to the `handleEntities*` functions below.
 */
abstract class HighlightFeature : Feature(), EntityUtils.HighlightSource {

    abstract fun shouldHighlight(info: EntityInfo): Boolean

    /** Drops every highlight owned by this feature and rebuilds it from the current entity list. */
    fun invalidateHighlights() {
        EntityUtils.removeAllForSource(this)

        if (!baseSetting.value) return

        EntityUtils.entityInfoList?.forEach { info ->
            if (shouldHighlight(info)) {
                EntityUtils.add(info.entity, this)
            }
        }
    }

    protected fun handleEntitiesAdded(entities: List<EntityInfo>) {
        if (!baseSetting.value) return

        entities.forEach { info ->
            if (shouldHighlight(info)) {
                EntityUtils.add(info.entity, this)
            }
        }
    }

    protected fun handleEntitiesRemoved(entities: List<EntityInfo>) {
        entities.forEach { info ->
            EntityUtils.remove(info.entity, this)
        }
    }

    protected fun handleEntitiesUpdated(entities: List<EntityInfo>) {
        if (!baseSetting.value) return

        entities.forEach { info ->
            val should = shouldHighlight(info)
            val has = EntityUtils.hasSource(info.entity, this)

            when {
                should && !has -> EntityUtils.add(info.entity, this)
                !should && has -> EntityUtils.remove(info.entity, this)
            }
        }
    }
}
