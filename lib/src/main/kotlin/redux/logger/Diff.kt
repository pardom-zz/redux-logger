package redux.logger

import redux.logger.Diff.Change.Addition
import redux.logger.Diff.Change.Deletion
import redux.logger.Diff.Change.Modification
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object Diff {

    private val COMPARABLE_PACKAGES = listOf(
        "java.lang",
        "java.util"
    )

    private val METHOD_CACHE = mutableMapOf<Class<*>, List<Method>>()

    interface Change {

        data class Modification(val name: String, val oldValue: Any?, val newValue: Any?) : Change {
            override fun toString() = "∆ $name: $oldValue → $newValue"
        }

        data class Addition(val name: String, val value: Any?) : Change {
            override fun toString() = "+ $name: $value"
        }

        data class Deletion(val name: String, val value: Any?) : Change {
            override fun toString() = "− $name: $value"
        }

    }

    fun calculate(old: Any?, new: Any?): List<Change> {
        return compare(
            inspect(old),
            inspect(new)
        )
    }

    private fun compare(
        old: Map<String, Any?>?,
        new: Map<String, Any?>?): List<Change> {

        val oldKeys = old?.keys.orEmpty()
        val newKeys = new?.keys.orEmpty()

        val additions = newKeys
            .subtract(oldKeys)
            .map { Addition(it, new?.get(it)) }

        val deletions = oldKeys
            .subtract(newKeys)
            .map { Deletion(it, old?.get(it)) }

        val modifications = oldKeys
            .intersect(newKeys)
            .filter { old?.get(it) != new?.get(it) }
            .map {
                val oldValue = old?.get(it)
                val newValue = new?.get(it)
                val comparable = oldValue != null && newValue != null
                    && Map::class.java.isAssignableFrom(oldValue.javaClass)
                    && Map::class.java.isAssignableFrom(newValue.javaClass)

                @Suppress("UNCHECKED_CAST")
                when {
                    comparable -> compare(
                        oldValue as Map<String, Any?>,
                        newValue as Map<String, Any?>
                    )
                    else -> listOf(Modification(it, old?.get(it), new?.get(it)))
                }
            }
            .flatten()

        return additions + deletions + modifications
    }

    private fun inspect(obj: Any?, parentName: String = ""): Map<String, Any?>? {
        return getProperties(obj)
            ?.map {
                val name = parentName + it.name
                    .removePrefix("is")
                    .removePrefix("get")
                    .toLowerCase()

                val value = it.invoke(obj)
                val comparable = if (value != null) {
                    val type = value.javaClass
                    val packageName = type.`package`.name
                    type.isPrimitive || packageName in COMPARABLE_PACKAGES
                }
                else true

                name to if (comparable) value else inspect(value, "$name.")
            }
            ?.toMap()
    }

    private fun getProperties(obj: Any?): List<Method>? {
        if (obj == null) return null

        return METHOD_CACHE[obj.javaClass] ?: obj
            .javaClass
            .declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }
            .filter { it.parameterTypes.isEmpty() }
            .filter { it.returnType != Void.TYPE }
            .filter { it.name.startsWith("is") || it.name.startsWith("get") }
            .filter { it.name != "getClass" }
            .apply { METHOD_CACHE.put(obj.javaClass, this) }
    }

}
