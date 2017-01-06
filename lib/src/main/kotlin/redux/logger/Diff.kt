package redux.logger

import redux.logger.Diff.Change.Addition
import redux.logger.Diff.Change.Deletion
import redux.logger.Diff.Change.Modification
import java.lang.reflect.Modifier
import java.util.Date

object Diff {

    private val MAP_CLASS = Map::class.java

    private val BASIC_TYPES = listOf(
        Any::class.java,
        Byte::class.java,
        Short::class.java,
        Int::class.java,
        Long::class.java,
        Float::class.java,
        Double::class.java,
        Boolean::class.java,
        Char::class.java,
        String::class.java,
        Date::class.java,
        Void::class.java
    )

    sealed class Change {

        class Modification(val name: String, val oldValue: Any?, val newValue: Any?) : Change() {

            override fun equals(other: Any?) =
                other is Modification
                    && name == other.name
                    && oldValue == other.oldValue
                    && newValue == other.newValue

            override fun toString() = "∆ $name: $oldValue → $newValue"

        }

        class Addition(val name: String, val value: Any?) : Change() {

            override fun equals(other: Any?) =
                other is Addition
                    && name == other.name
                    && value == other.value

            override fun toString() = "+ $name: $value"

        }

        class Deletion(val name: String, val value: Any?) : Change() {

            override fun equals(other: Any?) =
                other is Deletion
                    && name == other.name
                    && value == other.value

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

                when {
                    comparable -> compare(oldValue as Map<String, Any?>, newValue as Map<String, Any?>)
                    else -> listOf(Modification(it, old?.get(it), new?.get(it)))
                }
            }
            .flatten()

        return additions + deletions + modifications
    }

    private fun inspect(obj: Any?, parentName: String = ""): Map<String, Any?>? {
        return obj
            ?.javaClass
            ?.declaredMethods
            ?.filter { Modifier.isPublic(it.modifiers) }
            ?.filter { it.parameterTypes.isEmpty() }
            ?.filter { it.returnType != Void.TYPE }
            ?.filter { it.name.startsWith("is") || it.name.startsWith("get") }
            ?.filter { it.name != "getClass" }
            ?.map {
                val name = parentName + it.name
                    .removePrefix("is")
                    .removePrefix("get")
                    .toLowerCase()

                val returnType = it.returnType
                val comparable = returnType.isPrimitive || returnType in BASIC_TYPES

                name to if (comparable) it.invoke(obj) else inspect(it.invoke(obj), "$name.")
            }
            ?.toMap()
    }

}
