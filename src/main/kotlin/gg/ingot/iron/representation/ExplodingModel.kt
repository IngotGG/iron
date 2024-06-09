package gg.ingot.iron.representation

import gg.ingot.iron.sql.SqlParameters
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * A representation of a class that can be exploded into an array of its fields. This is useful for
 * building SQL queries and other operations that require the fields of a class to be exploded.
 * @author DebitCardz
 * @since 1.3
 */
interface ExplodingModel {
    /**
     * Explodes the model into an array of its fields.
     * @return An array of the fields of the model.
     */
    fun explode(): Array<Any> {
        val fields = getFields()

        return Array(fields.size) {
            fields[it].javaField
                ?.get(this)
                ?: error("Field ${fields[it].name} has no backing field.")
        }
    }

    /**
     * Converts the model into a map of its fields.
     * @return A map of the fields of the model.
     */
    fun toSqlParams(): SqlParameters {
        val fields = getFields()

        return fields.associate {
            it.name to (it.javaField?.get(this)
                ?: error("Field ${it.name} has no backing field."))
        }
    }

    private fun getFields(): List<KProperty<*>> {
        val kClass = this::class

        return cachedFields.getOrPut(kClass) {
            kClass.declaredMemberProperties
                .filterNot { it.javaField?.isSynthetic == true }
                .onEach { it.isAccessible = true }
                .map { it }
        }
    }

    companion object {
        /** A cache of fields for each model. */
        internal val cachedFields = HashMap<KClass<*>, List<KProperty<*>>>()
    }
}