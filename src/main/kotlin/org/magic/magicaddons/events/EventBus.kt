package org.magic.magicaddons.events

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

object EventBus {

    private val listeners = ConcurrentHashMap<Class<*>, MutableList<Listener>>()

    fun register(instance: Any) {
        instance::class.java.declaredMethods.forEach { method ->
            if (method.isAnnotationPresent(EventHandler::class.java)) {

                if (method.parameterCount != 1) throw IllegalArgumentException("Method ${method.name} must have 1 parameter")

                val eventType = method.parameterTypes[0]

                listeners
                    .computeIfAbsent(eventType) { mutableListOf() }
                    .add(Listener(instance, method))

                method.isAccessible = true
            }
        }
    }

    @JvmStatic
    fun post(event: Any) {
        val eventListeners = listeners[event::class.java] ?: return

        for (listener in eventListeners) {
            listener.method.invoke(listener.owner, event)

            if (event is Cancellable && event.canceled) {
                return
            }
        }
    }

    private data class Listener(
        val owner: Any,
        val method: Method
    )
}