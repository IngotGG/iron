package gg.ingot.iron.representation

import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.UseModelSerializers
import gg.ingot.iron.annotations.retrieveMatchingSerializer
import gg.ingot.iron.annotations.retrieveSerializer
import gg.ingot.iron.serialization.ColumnSerializer
import gg.ingot.iron.sql.SqlParameters
import gg.ingot.iron.sql.jsonField
import gg.ingot.iron.sql.serializedField
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
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
            it.kProperty.name to getFieldValue(it)
        }
    }

    /**
     * Retrieve the value of a field.
     * @param field The field to retrieve the value of.
     * @return The value of the field.
     * @throws IllegalArgumentException If the field has no backing field.
     */
    private fun getFieldValue(field: Field): Any {
        val value = field.kProperty.javaField?.get(this)
            ?: error("Field ${field.kProperty.name} has no backing field.")

        return if(field.serializer != null) {
            serializedField(value, field.serializer)
        } else if(field.json) {
            jsonField(value)
        } else {
            value
        }
    }

    /**
     * Retrieve the fields of the model.
     * @return The fields of the model.
     */
    private fun getFields(): List<Field> {
        val kClass = this::class

        return cachedFields.getOrPut(kClass) {
            val useSerializersAnnotation = this::class.findAnnotation<UseModelSerializers>()

            val params = kClass.primaryConstructor
                ?.parameters
                ?: error("Model ${kClass.simpleName} has no primary constructor.")

            val sortedFields = kClass.declaredMemberProperties
                .filterNot { it.javaField?.isSynthetic == true }
                .onEach { it.isAccessible = true }
                .sortedBy { params.indexOfFirst { param -> param.name == it.name } }
                .map { it }

            sortedFields.map {
                val columnAnnotation = it.findAnnotation<Column>()

                val serializer = if(columnAnnotation != null) {
                    columnAnnotation.retrieveSerializer()
                } else if(useSerializersAnnotation != null) {
                    useSerializersAnnotation.retrieveMatchingSerializer(it.returnType.classifier as KClass<*>)
                } else {
                    null
                }

                if(serializer != null) {
                    logger.debug("Using serializer ${serializer::class.simpleName} for field ${it.name}, this was cached.")
                }

                Field(it, columnAnnotation?.json ?: false, serializer)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExplodingModel::class.java)

        /** A cache of fields for each model. */
        private val cachedFields = HashMap<KClass<*>, List<Field>>()
    }
}

private data class Field(
    val kProperty: KProperty<*>,
    val json: Boolean,
    val serializer: ColumnSerializer<*, *>?
)