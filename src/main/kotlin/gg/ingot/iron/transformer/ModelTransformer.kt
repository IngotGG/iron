package gg.ingot.iron.transformer

import gg.ingot.iron.IronSettings
import gg.ingot.iron.annotations.*
import gg.ingot.iron.representation.EntityField
import gg.ingot.iron.representation.EntityModel
import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.serialization.ColumnSerializer
import gg.ingot.iron.sql.params.ColumnJsonField
import gg.ingot.iron.sql.params.ColumnSerializedField
import gg.ingot.iron.strategies.NamingStrategy
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * Transforms a model to an entity representation which holds information about the class model, allowing
 * for the model to be easily built. This class interfaces with reflection to build the entity.
 * @author Santio
 * @since 1.0
 */
internal class ModelTransformer(
    private val namingStrategy: NamingStrategy,
    private val adapters: IronSettings.Adapters? = null
) {
    /**
     * Transforms a class into an entity model, which holds information about the class model.
     * @param clazz The class to transform into an entity model.
     * @return The entity model representation of the class.
     */
    fun transform(clazz: KClass<*>): EntityModel {
        return models.getOrPut(clazz) {
            val fields = mutableListOf<EntityField>()
            val modelAnnotation = clazz.annotations.find { it is Model } as Model?
            var properties = clazz.declaredMemberProperties

            if (clazz.primaryConstructor != null) {
                val constructor = clazz.primaryConstructor!!
                properties = properties.sortedBy {
                    constructor.parameters.indexOfFirst { param -> param.name == it.name }
                }
            }

            for(field in properties) {
                field.isAccessible = true
                val annotation = field.annotations.find { it is Column } as Column?

                if (annotation != null && annotation.ignore || field.javaField?.isSynthetic == true) {
                    continue
                }

                fields.add(EntityField(
                    field = field,
                    javaField = field.javaField ?: error("Field ${field.name} (${field}) has no backing field."),
                    columnName = retrieveName(field, annotation),
                    nullable = field.returnType.isMarkedNullable,
                    isJson = annotation?.json ?: false,
                    serializer = retrieveSerializer(field, annotation),
                    deserializer = retrieveDeserializer(field, annotation)
                ))
            }

            val strategy = modelAnnotation?.namingStrategy
                .takeIf { it != NamingStrategy.NONE }
                ?: namingStrategy

            EntityModel(
                clazz,
                fields,
                strategy
            )
        }
    }

    /**
     * Retrieve the name for the given field.
     * @param field The field to retrieve the name for.
     * @param annotation The column annotation for the field.
     * @return The name for the field.
     */
    private fun retrieveName(
        field: KProperty<*>,
        annotation: Column?
    ): String {
        // defaults to "" if column is added but no name is provided
        return if(
            annotation != null
            && annotation.name.isNotEmpty()
        ) {
            annotation.name
        } else {
            field.name
        }
    }

    /**
     * Get the value of a field in an exploding model
     * @param model The exploding model
     * @param field The entity representing the reflection details of the field
     * @return The value of the field
     */
    internal fun getModelValue(model: ExplodingModel, field: EntityField): Any? {
        val value = field.javaField.get(model)
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
     * Retrieve the deserializer for the given field.
     * @param annotation The column annotation for the field.
     * @return The deserializer for the field if it exists, otherwise null.
     */
    private fun retrieveDeserializer(
        field: KProperty<*>,
        annotation: Column?
    ): ColumnDeserializer<*, *>? {
        val kClass = field.returnType.classifier as KClass<*>
        return annotation?.retrieveDeserializer() ?: adapters?.retrieveDeserializer(kClass)
    }

    /**
     * Retrieve the serializer for the given field.
     * @param annotation The column annotation for the field.
     * @return The serializer for the field if it exists, otherwise null.
     */
    private fun retrieveSerializer(
        field: KProperty<*>,
        annotation: Column?
    ): ColumnSerializer<*, *>? {
        val kClass = field.returnType.classifier as KClass<*>
        return annotation?.retrieveSerializer() ?: adapters?.retrieveSerializer(kClass)
    }

    companion object {
        /** Cached map of [EntityModel]. */
        private val models = ConcurrentHashMap<KClass<*>, EntityModel>()
    }
}