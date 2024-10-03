package gg.ingot.iron.annotations

import gg.ingot.iron.serialization.*
import gg.ingot.iron.strategies.EnumTransformation
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(
    /**
     * The name of the column in the database
     */
    val name: String = "",

    /**
     * The name of the variable in queries that are dynamically replaced, this is used in situations
     * where you want to use a variable in a query such as `:variable`, by default this is the
     * same as the field name and NOT the column name
     */
    val variable: String = "",

    /**
     * If this column should be ignored when building the entity
     */
    val ignore: Boolean = false,

    /**
     * If this column is a json column in the database
     */
    val json: Boolean = false,

    /**
     * If this column is nullable in the database
     */
    val nullable: Boolean = false,

    /**
     * If this column is the primary key for the table, this is used in conjunction with the
     * controller module
     */
    val primaryKey: Boolean = false,

    /**
     * The enum transformation to use for this column, defaults to Iron's default (EnumTransformation::class),
     * you can specify a custom enum transformation by providing a KClass<out EnumTransformation> here, such as
     * EnumTransformation.Ordinal::class.
     */
    val enum: KClass<out EnumTransformation> = EnumTransformation::class,

    val adapter: KClass<out ColumnAdapter<*, *>> = EmptyAdapter::class,
    val deserializer: KClass<out ColumnDeserializer<*, *>> = EmptyDeserializer::class,
    val serializer: KClass<out ColumnSerializer<*, *>> = EmptySerializer::class
)

internal fun Column.retrieveDeserializer(): ColumnDeserializer<*, *>? {
    if(adapter != EmptyAdapter::class) {
        return adapter.objectInstance
            ?: adapter.createInstance()
    }

    if(deserializer != EmptyDeserializer::class) {
        return deserializer.objectInstance
            ?: deserializer.createInstance()
    }

    return null
}

internal fun Column.retrieveSerializer(): ColumnSerializer<*, *>? {
    if(adapter != EmptyAdapter::class) {
        return adapter.objectInstance
            ?: adapter.createInstance()
    }

    if(serializer != EmptySerializer::class) {
        return serializer.objectInstance
            ?: serializer.createInstance()
    }

    return null
}