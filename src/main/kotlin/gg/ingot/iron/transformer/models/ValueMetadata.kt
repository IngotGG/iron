package gg.ingot.iron.transformer.models

import gg.ingot.iron.representation.EntityField
import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.serialization.ColumnSerializer
import gg.ingot.iron.sql.binding.types.JsonType
import gg.ingot.iron.strategies.EnumTransformation
import java.util.*

data class ValueMetadata private constructor(
    val nullable: Boolean,
    val isOptional: Boolean,
    val isArray: Boolean,
    val isCollection: Boolean,
    val isSet: Boolean,
    val isJson: Boolean,
    val serializer: ColumnSerializer<*, *>?,
    val deserializer: ColumnDeserializer<*, *>?,
    val enumTransformation: EnumTransformation?
) {

    internal constructor(field: EntityField): this(
        nullable = field.nullable,
        isOptional = field.isOptional,
        isArray = field.isArray,
        isCollection = field.isCollection,
        isSet = field.isSet,
        isJson = field.isJson,
        serializer = field.serializer,
        deserializer = field.deserializer,
        enumTransformation = field.enumTransformation
    )

    internal constructor(value: Any?): this(
        isArray = value != null && value::class.java.isArray,
        isSet = value is Set<*>,
        isCollection = value is Collection<*>,
        isJson = value is JsonType<*>,
        nullable = true,
        isOptional = value is Optional<*>,
        serializer = null,
        deserializer = null,
        enumTransformation = null
    )

}