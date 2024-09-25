package gg.ingot.iron.transformerOld

import gg.ingot.iron.Inflector
import gg.ingot.iron.IronSettings
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.annotations.retrieveDeserializer
import gg.ingot.iron.annotations.retrieveSerializer
import gg.ingot.iron.representation.EntityField
import gg.ingot.iron.representation.EntityModel
import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.serialization.ColumnSerializer
import gg.ingot.iron.sql.params.ColumnJsonField
import gg.ingot.iron.sql.params.ColumnSerializedField
import gg.ingot.iron.strategies.NamingStrategy
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.kotlinProperty

/**
 * Transforms a model to an entity representation which holds information about the class model, allowing
 * for the model to be easily built. This class interfaces with reflection to build the entity.
 * @author Santio
 * @since 1.0
 */
class ModelTransformerOld(
    private val settings: IronSettings,
    private val inflector: Inflector
) {

    /**
     * Transforms a class into an entity model, which holds information about the reflected class.
     * @param clazz The class to transform into an entity model.
     * @return The entity model representation of the class.
     */
    fun transform(clazz: Class<*>): EntityModel {
        return models.getOrPut(clazz) {
            val fields = mutableListOf<EntityField>()
            val modelAnnotation = clazz.annotations.find { it is Model } as Model?

            val declaredFields = clazz.declaredFields.associateWith {
                it.kotlinProperty?.name ?: it.name
            }

            val properties = if (clazz.kotlin.isData) {
                // It's kotlin
                val kotlin = clazz.kotlin
                val members = kotlin.declaredMemberProperties
                val parameters = kotlin.primaryConstructor?.parameters
                    ?: error("Primary constructor not found for data class: $clazz")

                parameters.associate { parameter ->
                    val field = members.find { it.name == parameter.name }
                        ?: error("Field not found for parameter: $parameter")

                    field.javaField!! to field.findAnnotation<Column>()
                }
            } else {
                declaredFields.keys.associateWith { it.annotations.find { it is Column } as Column? }
            }

            var hasPrimaryKey = false
            for((field, annotation) in properties.entries) {
                field.isAccessible = true

                if (annotation?.ignore == true || field.isSynthetic || Modifier.isTransient(field.modifiers)) {
                    continue
                }

                if (annotation?.primaryKey == true) {
                    if (hasPrimaryKey) {
                        throw IllegalArgumentException("Iron doesn't support having multiple primary keys in a model")
                    }

                    hasPrimaryKey = true
                }

                fields.add(EntityField(
                    field = field,
                    columnName = retrieveName(field, annotation),
                    variableName = annotation?.variable?.takeIf { it.isNotBlank() } ?: field.name,
                    nullable = field.kotlinProperty?.returnType?.isMarkedNullable ?: annotation?.nullable ?: false,
                    isJson = annotation?.json ?: false,
                    isPrimaryKey = annotation?.primaryKey ?: false,
                    serializer = retrieveSerializer(field, annotation),
                    deserializer = retrieveDeserializer(field, annotation)
                ))
            }

            val strategy = modelAnnotation?.namingStrategy
                .takeIf { it != NamingStrategy.NONE }
                ?: settings.namingStrategy

            EntityModel(
                clazz,
                fields,
                strategy
            )
        }
    }

    /**
     * Transforms a class into an entity model, which holds information about the reflected class.
     * @param clazz The kotlin class to transform into an entity model.
     * @return The entity model representation of the class.
     */
    fun transform(clazz: KClass<*>): EntityModel {
        return transform(clazz.java)
    }

    /**
     * Get the value of a field in an exploding model
     * @param model The exploding model
     * @param field The entity representing the reflection details of the field
     * @return The value of the field
     */
    internal fun getModelValue(model: Any, field: EntityField): Any? {
        if (!model.javaClass.isAnnotationPresent(Model::class.java)) {
            throw IllegalArgumentException("Model must be annotated with @Model")
        }

        val value = field.field.get(model)
            ?: return null

        return if (field.serializer != null) {
            ColumnSerializedField(value, field.serializer)
        } else if (field.isJson) {
            ColumnJsonField(value)
        } else {
            value
        }
    }

    /**
     * Retrieve the name for the given field.
     * @param field The field to retrieve the name for.
     * @param annotation The column annotation for the field.
     * @return The name for the field.
     */
    private fun retrieveName(
        field: Field,
        annotation: Column?
    ): String {
        return annotation?.name?.takeIf { it.isNotBlank() } ?: inflector.columnName(field)
    }

    /**
     * Retrieve the deserializer for the given field.
     * @param annotation The column annotation for the field.
     * @return The deserializer for the field if it exists, otherwise null.
     */
    private fun retrieveDeserializer(
        field: Field,
        annotation: Column?
    ): ColumnDeserializer<*, *>? {
        return annotation?.retrieveDeserializer() ?: settings.adapters?.retrieveDeserializer(field.type)
    }

    /**
     * Retrieve the serializer for the given field.
     * @param field The field to retrieve the serializer for.
     * @param annotation The column annotation for the field.
     * @return The serializer for the field if it exists, otherwise null.
     */
    private fun retrieveSerializer(
        field: Field,
        annotation: Column?
    ): ColumnSerializer<*, *>? {
        return annotation?.retrieveSerializer() ?: settings.adapters?.retrieveSerializer(field.type)
    }

    companion object {
        /** Cached map of [EntityModel]. */
        private val models = ConcurrentHashMap<Class<*>, EntityModel>()
    }
}