package gg.ingot.iron.model

import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.annotations.retrieveDeserializer
import gg.ingot.iron.annotations.retrieveSerializer
import gg.ingot.iron.representation.EntityField
import gg.ingot.iron.representation.EntityModel
import gg.ingot.iron.sql.params.ColumnJsonField
import gg.ingot.iron.sql.params.ColumnSerializedField
import gg.ingot.iron.stratergies.EnumTransformation
import gg.ingot.iron.stratergies.NamingStrategy
import org.jetbrains.annotations.Nullable
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

/**
 * Transforms a model to an entity representation which holds information about the class model, allowing
 * for the model to be easily built. This class interfaces with reflection to build the entity.
 * @author santio
 * @since 2.0
 */
class ModelReader(
    private val iron: Iron
) {

    /**
     * Transforms a class into an entity model, which holds information about the reflected class.
     * @param clazz The class to transform into an entity model.
     * @return The entity model representation of the class.
     */
    fun read(clazz: Class<*>): EntityModel {
        return models.getOrPut(clazz) {
            generateModel(clazz)
        }
    }

    /**
     * Transforms a class into an entity model, which holds information about the reflected class.
     * @param clazz The kotlin class to transform into an entity model.
     * @return The entity model representation of the class.
     */
    fun read(clazz: KClass<*>): EntityModel {
        return read(clazz.java)
    }

    private fun generateModel(clazz: Class<*>): EntityModel {
        val entityFields = mutableListOf<EntityField>()
        val modelAnnotation = clazz.annotations.find { it is Model } as Model?

        val fields = if (clazz.kotlin.isData) {
            val constructor = clazz.kotlin.primaryConstructor
            var properties = clazz.kotlin.declaredMemberProperties
                .filter { it.javaField != null }

            if (constructor != null) {
                properties = properties.sortedBy {
                    constructor.parameters.indexOfFirst { param -> param.name == it.name }
                }
            }

            properties.map { ModelField(iron, it, it.javaField!!) }
        } else {
            clazz.declaredFields
                .map { ModelField(iron, null, it) }
        }.filter { !it.isIgnored() }

        // Check if we have multiple primary keys
        val primaryKey = fields.filter { it.isPrimaryKey() }
        if (primaryKey.size > 1) {
            throw IllegalArgumentException("Iron doesn't support having composite primary keys in model '${clazz.simpleName}'")
        }

        for(field in fields) {
            val nullable = field.kotlin?.returnType?.isMarkedNullable
                ?: (field.annotation<Nullable>() != null).takeIf { it }
                ?: field.details?.nullable
                ?: false

            val serializer = field.details?.retrieveSerializer()
                ?: iron.settings.adapters?.retrieveSerializer(field.java.type)

            val deserializer = field.details?.retrieveDeserializer()
                ?: iron.settings.adapters?.retrieveDeserializer(field.java.type)

            val enumTransformation = field.details?.enum
                ?.takeIf { it != EnumTransformation::class }
                ?.objectInstance

            entityFields.add(EntityField(
                field = field,
                nullable = nullable,
                isJson = field.details?.json ?: false,
                serializer = serializer,
                deserializer = deserializer,
                enumTransformation = enumTransformation
            ))
        }

        val strategy = modelAnnotation?.namingStrategy
            .takeIf { it != NamingStrategy.NONE }
            ?: iron.settings.namingStrategy

        return EntityModel(
            clazz,
            entityFields,
            strategy
        )
    }

    /**
     * Get the value of a field in an exploding model
     * @param model The exploding model
     * @param field The entity representing the reflection details of the field
     * @return The value of the field
     */
    internal fun getModelValue(model: Any, field: EntityField): Any? {
        if (!model.javaClass.isAnnotationPresent(Model::class.java)) {
            throw IllegalArgumentException("Model '${model::class.java.name}' must be annotated with @Model")
        }

        val value = field.field.java.get(model)
            ?: return null

        return if (field.serializer != null) {
            ColumnSerializedField(value, field.serializer)
        } else if (field.isJson) {
            ColumnJsonField(value)
        } else {
            value
        }
    }

    private companion object {
        /** Cached map of [EntityModel]. */
        val models = ConcurrentHashMap<Class<*>, EntityModel>()
    }
}