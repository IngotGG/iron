package gg.ingot.iron.sql.params

/**
 * Represents a JSON field in a column.
 * @param value The value of the JSON field.
 */
data class ColumnJsonField internal constructor(val value: Any?)

/**
 * Create a JSON field for the given value.
 * @param value The value of the JSON field.
 * @return The wrapped JSON field.
 */
fun jsonField(value: Any?) = ColumnJsonField(value)