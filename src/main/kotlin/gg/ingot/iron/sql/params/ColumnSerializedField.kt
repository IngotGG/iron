package gg.ingot.iron.sql.params

import gg.ingot.iron.serialization.ColumnSerializer

/**
 * Represents a serialized field in a column.
 * @param value The value of the serialized field.
 * @param serializer The serializer for the serialized field.
 */
data class ColumnSerializedField internal constructor(val value: Any?, val serializer: ColumnSerializer<*, *>)

/**
 * Create a serialized field for the given value.
 * @param value The value of the serialized field.
 * @param serializer The serializer for the serialized field.
 * @return The wrapped serialized field.
 */
fun <T: Any> serializedField(value: T?, serializer: ColumnSerializer<T, *>) = ColumnSerializedField(value, serializer)