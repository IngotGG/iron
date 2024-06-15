package gg.ingot.iron.representation

import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.retrieveSerializer
import gg.ingot.iron.serialization.EmptySerializer
import gg.ingot.iron.sql.SqlParameters
import gg.ingot.iron.sql.jsonField
import gg.ingot.iron.sql.serializedField
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
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

        return Array(fields.size) { getFieldValue(fields[it]) }
    }

    /**
     * Converts the model into a map of its fields.
     * @return A map of the fields of the model.
     */
    fun toSqlParams(): SqlParameters {
        val fields = getFields()

        return fields.associate {
            it.name to getFieldValue(it)
        }
    }

    /**
     * Retrieve the value of a field.
     * @param field The field to retrieve the value of.
     * @return The value of the field.
     * @throws IllegalArgumentException If the field has no backing field.
     */
    private fun getFieldValue(field: KProperty<*>): Any {
        val annotation = field.findAnnotation<Column>()
        val value = field.javaField?.get(this)
            ?: error("Field ${field.name} has no backing field.")

        if(annotation != null) {
            val serializer = annotation.retrieveSerializer()
            if(serializer != null) {
                return serializedField(value, annotation.serializer)
            }

            if(annotation.json) {
                return jsonField(value)
            }
        }

        return value
    }

    /**
     * Retrieve the fields of the model.
     * @return The fields of the model.
     */
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