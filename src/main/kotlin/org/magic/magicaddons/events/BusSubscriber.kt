package org.magic.magicaddons.events

abstract class BusSubscriber {
    init {
        EventBus.register(this)
    }
}