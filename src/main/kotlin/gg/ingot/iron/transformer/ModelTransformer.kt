package gg.ingot.iron.transformer

import gg.ingot.iron.annotations.Column
import gg.ingot.iron.repository.ModelRepository
import gg.ingot.iron.representation.EntityField
import gg.ingot.iron.representation.EntityModel
import gg.ingot.iron.serialization.*
import gg.ingot.iron.serialization.EmptyDeserializer
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

/**
 * Transforms a model to an entity representation which holds information about the class model, allowing
 * for the model to be easily built. This class interfaces with reflection to build the entity.
 * @author Santio
 * @since 1.0
 */
internal object ModelTransformer {
    /**
     * Transforms a class into an entity model, which holds information about the class model.
     * @param clazz The class to transform into an entity model.
     * @return The entity model representation of the class.
     */
    fun transform(clazz: KClass<*>): EntityModel {
        return ModelRepository.models.getOrPut(clazz) {
            val fields = mutableListOf<EntityField>()

            for (field in clazz.declaredMemberProperties) {
                val annotation = field.annotations.find { it is Column } as Column?

                if (annotation != null && annotation.ignore || field.javaField?.isSynthetic == true) {
                    continue
                }

                fields.add(EntityField(
                    field = field,
                    javaField = field.javaField ?: error("Field ${field.name} has no backing field."),
                    columnName = retrieveName(field, annotation),
                    nullable = field.returnType.isMarkedNullable,
                    isJson = annotation?.json ?: false,
                    isArray = isArray(field),
                    isCollection = isCollection(field),
                    isEnum = isEnum(field),
                    deserializer = retrieveDeserializer(field, annotation)
                ))
            }

            EntityModel(clazz, fields)
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
     * Retrieve the deserializer for the given field.
     * @param annotation The column annotation for the field.
     * @return The deserializer for the field if it exists, otherwise null.
     */
    @Suppress("UNCHECKED_CAST")
    private fun retrieveDeserializer(
        field: KProperty<*>,
        annotation: Column?
    ): ColumnDeserializer<*, *>? {
        if (annotation == null || annotation.deserializer == EmptyDeserializer::class) {
            return null
        }

        return annotation.deserializer.objectInstance
            ?: annotation.deserializer.createInstance()
    }
}