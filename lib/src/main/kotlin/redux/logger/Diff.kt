package redux.logger

import redux.logger.Diff.Change.Addition
import redux.logger.Diff.Change.Deletion
import redux.logger.Diff.Change.Modification
import java.util.Date

object Diff {

    private val BASIC_TYPES = listOf(
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

    fun calculate(
        old: Any?,
        new: Any?,
        filter: List<Class<*>> = emptyList()): List<Change> {

        return compare(
            inspect(old, filter),
            inspect(new, filter)
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
            .map { Modification(it, old?.get(it), new?.get(it)) }

        return additions + deletions + modifications
    }

    private fun inspect(obj: Any?, filter: List<Class<*>>): Map<String, Any?>? {
        return obj
            ?.javaClass
            ?.declaredFields
            ?.filter { it.type !in filter }
            ?.map {
                it.isAccessible = true
                it.name to if (it.type.isPrimitive || (BASIC_TYPES).contains(it.type)) {
                    it.get(obj)
                }
                else {
                    inspect(it.get(obj), filter)
                }
            }
            ?.toMap()
    }

}
