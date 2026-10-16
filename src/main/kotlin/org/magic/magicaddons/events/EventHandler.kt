package org.magic.magicaddons.events

import org.magic.magicaddons.util.SBLocation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventHandler(
    val priority: Int = 0,
    val onlyIn: Array<SBLocation> = []
)
