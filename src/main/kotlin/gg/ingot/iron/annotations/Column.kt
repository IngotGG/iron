package gg.ingot.iron.annotations

import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.serialization.ColumnSerializer
import gg.ingot.iron.serialization.EmptyDeserializer
import gg.ingot.iron.serialization.EmptySerializer
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(
    val name: String = "",
    val ignore: Boolean = false,
    val json: Boolean = false,
    val deserializer: KClass<out ColumnDeserializer<*, *>> = EmptyDeserializer::class,
    val serializer: KClass<out ColumnSerializer<*, *>> = EmptySerializer::class
)