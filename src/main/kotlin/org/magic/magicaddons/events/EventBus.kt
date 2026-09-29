package org.magic.magicaddons.events

import org.magic.magicaddons.util.ErrorReporter
import org.magic.magicaddons.util.SBLocation
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object EventBus {

    private val listeners = ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<Listener>>()

    fun register(instance: Any) {
        instance::class.java.declaredMethods.forEach { method ->
            if (method.isAnnotationPresent(EventHandler::class.java)) {

                if (method.parameterCount != 1) throw IllegalArgumentException("Method ${method.name} must have 1 parameter")

                val eventType = method.parameterTypes[0]
                val handler = method.getAnnotation(EventHandler::class.java)

                val eventListeners = listeners.computeIfAbsent(eventType) { CopyOnWriteArrayList() }
                eventListeners.add(Listener(instance, method, handler.priority, handler.onlyIn.toList()))
                eventListeners.sortWith(compareByDescending { it.priority })

                method.isAccessible = true
            }
        }
    }

    @JvmStatic
    fun post(event: Any) {
        val eventListeners = listeners[event::class.java] ?: return

        for (listener in eventListeners) {
            if (listener.onlyIn.isNotEmpty() && listener.onlyIn.none { it.inside() }) continue

            try {
                listener.method.invoke(listener.owner, event)
            } catch (error: Throwable) {
                ErrorReporter.report("${listener.owner::class.simpleName}.${listener.method.name}", error)
            }

            if (event is Cancellable && event.canceled) {
                return
            }
        }
    }

    private data class Listener(
        val owner: Any,
        val method: Method,
        val priority: Int,
        val onlyIn: List<SBLocation>
    )
}