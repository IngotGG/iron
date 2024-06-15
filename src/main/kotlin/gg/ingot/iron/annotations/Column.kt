package gg.ingot.iron.annotations

import gg.ingot.iron.serialization.*
import gg.ingot.iron.serialization.EmptyDeserializer
import gg.ingot.iron.serialization.EmptySerializer
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(
    val name: String = "",
    val ignore: Boolean = false,
    val json: Boolean = false,

    val transformer: KClass<out ColumnTransformer<*, *>> = EmptyTransformer::class,
    val deserializer: KClass<out ColumnDeserializer<*, *>> = EmptyDeserializer::class,
    val serializer: KClass<out ColumnSerializer<*, *>> = EmptySerializer::class
)

internal fun Column.retrieveDeserializer(): ColumnDeserializer<*, *>? {
    if(transformer != EmptyTransformer::class) {
        return transformer.objectInstance
            ?: transformer.createInstance()
    }

    if(deserializer != EmptyDeserializer::class) {
        return deserializer.objectInstance
            ?: deserializer.createInstance()
    }

    return null
}

internal fun Column.retrieveSerializer(): ColumnSerializer<*, *>? {
    if(transformer != EmptyTransformer::class) {
        return transformer.objectInstance
            ?: transformer.createInstance()
    }

    if(serializer != EmptySerializer::class) {
        return serializer.objectInstance
            ?: serializer.createInstance()
    }

    return null
}