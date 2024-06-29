package gg.ingot.iron.sql.params

import gg.ingot.iron.serialization.ColumnSerializer

data class ColumnSerializedField internal constructor(val value: Any?, val serializer: ColumnSerializer<*, *>)

fun <T: Any> serializedField(value: T?, serializer: ColumnSerializer<T, *>) = ColumnSerializedField(value, serializer)