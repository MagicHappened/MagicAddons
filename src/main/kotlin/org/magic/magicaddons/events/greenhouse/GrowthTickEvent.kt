package org.magic.magicaddons.events.greenhouse

/**
 * The garden's growth tick has come round, carrying how many passed since the last look and how
 * long one takes. Posted after the plants have been moved on, so a listener reads the world as it is.
 */
class GrowthTickEvent(val ticks: Int, val tickMs: Long)
