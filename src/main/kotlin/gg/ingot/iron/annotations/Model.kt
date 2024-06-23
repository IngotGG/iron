package gg.ingot.iron.annotations

import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.serialization.ColumnSerializer
import gg.ingot.iron.strategies.NamingStrategy
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

/**
 * Represents a class that is a model for a database entity.
 * @param namingStrategy The naming strategy to use for all columns in the model.
 */
annotation class Model(
    val namingStrategy: NamingStrategy = NamingStrategy.NONE,
)

/**
 * Represents serializers to use for all types a part of a model
 * that match the un-serialized type of the serializer.
 */
@Target(AnnotationTarget.CLASS)
annotation class UseModelSerializers(
    vararg val serializers: KClass<out ColumnSerializer<*, *>>
)

/**
 * Represents deserializers to use for all types a part of a model
 * that match the un-serialized type of the deserializer.
 */
@Target(AnnotationTarget.CLASS)
annotation class UseModelDeserializers(
    vararg val deserializers: KClass<out ColumnDeserializer<*, *>>
)

/**
 * Retrieves the name of the column from the annotation or the field name.
 * @param type The type of the field.
 * @return The potential column serializer.
 */
internal fun UseModelSerializers.retrieveMatchingSerializer(type: KClass<*>): ColumnSerializer<*, *>? {
    for(serializer in serializers) {
        val columnSerializer = serializer.supertypes.firstOrNull {
            it.isSubtypeOf(ColumnSerializer::class.starProjectedType)
        }

        val fromType = columnSerializer?.arguments?.getOrNull(0)?.type?.classifier as? KClass<*>
            ?: continue

        if(fromType == type) {
            return serializer.objectInstance
                ?: serializer.createInstance()
        }
    }
    return null
}

/**
 * Retrieves the name of the column from the annotation or the field name.
 * @param type The type of the field.
 * @return The potential column deserializer.
 */
internal fun UseModelDeserializers.retrieveMatchingDeserializer(type: KClass<*>): ColumnDeserializer<*, *>? {
    for(deserializer in deserializers) {
        val columnDeserializer = deserializer.supertypes.firstOrNull {
            it.isSubtypeOf(ColumnDeserializer::class.starProjectedType)
        }

        val toType = columnDeserializer?.arguments?.getOrNull(1)?.type?.classifier as? KClass<*>
            ?: continue

        if(toType == type) {
            return deserializer.objectInstance
                ?: deserializer.createInstance()
        }
    }
    return null
}