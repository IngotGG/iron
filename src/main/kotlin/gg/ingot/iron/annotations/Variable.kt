package gg.ingot.iron.annotations

import gg.ingot.iron.serialization.ColumnSerializer
import gg.ingot.iron.serialization.EmptySerializer
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Variable(
    val json: Boolean = false,
    val serializer: KClass<out ColumnSerializer<*, *>> = EmptySerializer::class
)
