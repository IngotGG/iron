package gg.ingot.iron.sql.types

import gg.ingot.iron.models.SqlColumn

/**
 * Represents a value that has contextual information such as the column definition
 * which holds the serializer / adapter if it exists.
 * @param value The value to serialize
 * @param column The column definition
 * @author santio
 * @since 2.0
 */
data class ContextualValue(
    val value: Any?,
    val column: SqlColumn
)
