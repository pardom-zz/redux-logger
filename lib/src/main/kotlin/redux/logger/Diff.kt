package redux.logger

import redux.logger.Diff.Change.Addition
import redux.logger.Diff.Change.Deletion
import redux.logger.Diff.Change.Modification
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Arrays

object Diff {

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Private constants

    private val COMPARABLE_PACKAGES = listOf(
        "java.lang",
        "java.util"
    )

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Private members

    private val methodCache = mutableMapOf<Class<*>, List<Method>>()

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Public functions

    fun calculate(old: Any?, new: Any?): List<Change> {
        return compare(
            createMap(old),
            createMap(new)
        )
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Private functions

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

    private fun createMap(obj: Any?, parentName: String = ""): Map<String, Any?>? {
        return getProperties(obj)
            ?.map {
                val name = parentName + it.name
                    .replace(Regex("^(get|is)"), "")  // Kotlin property prefix
                    .decapitalize()

                val value = it.invoke(obj)
                val comparable = if (value != null) {
                    val type = value.javaClass
                    type.isPrimitive
                        || type.isArray
                        || type.isEnum
                        || type.`package`.name in COMPARABLE_PACKAGES
                }
                else true

                name to if (comparable) value else createMap(value, "$name.")
            }
            ?.toMap()
    }

    private fun getProperties(obj: Any?): List<Method>? {
        if (obj == null) return null

        return methodCache[obj.javaClass] ?: obj
            .javaClass
            .declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }
            .filter { it.parameterTypes.isEmpty() }
            .filter { it.returnType != Void.TYPE }
            .filter { it.name.matches(Regex("^(get|is).*+$")) } // Kotlin properties
            .filter { it.name != "getClass" }
            .apply { methodCache.put(obj.javaClass, this) }
    }

    private fun Any?.isEqualTo(obj: Any?): Boolean {
        return when {
            this is Array<*> && obj is Array<*> -> Arrays.deepEquals(this, obj)
            else -> this == obj
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Classes

    sealed class Change {

        class Modification(val name: String, val oldValue: Any?, val newValue: Any?) : Change() {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other?.javaClass != javaClass) return false

                other as Modification

                if (name != other.name) return false
                if (!oldValue.isEqualTo(other.oldValue)) return false
                if (!newValue.isEqualTo(other.newValue)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = name.hashCode()
                result = 31 * result + (oldValue?.hashCode() ?: 0)
                result = 31 * result + (newValue?.hashCode() ?: 0)
                return result
            }

            override fun toString(): String {
                val sb = StringBuilder()
                sb.append("∆ $name: ")
                sb.append(if (oldValue is Array<*>) Arrays.toString(oldValue) else "$oldValue")
                sb.append(" → ")
                sb.append(if (newValue is Array<*>) Arrays.toString(newValue) else "$newValue")
                return sb.toString()
            }

        }

        class Addition(val name: String, val value: Any?) : Change() {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other?.javaClass != javaClass) return false

                other as Addition

                if (name != other.name) return false
                if (!value.isEqualTo(other.value)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = name.hashCode()
                result = 31 * result + (value?.hashCode() ?: 0)
                return result
            }

            override fun toString(): String {
                val sb = StringBuilder()
                sb.append("+ $name: ")
                sb.append(if (value is Array<*>) Arrays.toString(value) else "$value")
                return sb.toString()
            }

        }

        class Deletion(val name: String, val value: Any?) : Change() {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other?.javaClass != javaClass) return false

                other as Deletion

                if (name != other.name) return false
                if (!value.isEqualTo(other.value)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = name.hashCode()
                result = 31 * result + (value?.hashCode() ?: 0)
                return result
            }

            override fun toString(): String {
                val sb = StringBuilder()
                sb.append("- $name: ")
                sb.append(if (value is Array<*>) Arrays.toString(value) else "$value")
                return sb.toString()
            }

        }

    }

}
