@file:Suppress("UNCHECKED_CAST")

package gg.ingot.iron.sql

import gg.ingot.iron.serialization.ColumnDeserializer
import java.sql.ResultSet

/**
 * Gets a value from the ResultSet at the specified column.
 * @param column The column to get the value from.
 * @return The value at the column.
 * @throws IllegalStateException If the value is null.
 */
inline fun <reified T> ResultSet?.get(column: String): T {
    return getNullable(column) ?: error("Value is null")
}

/**
 * Gets a value from the ResultSet at the specified column.
 * @param column The column to get the value from.
 * @return The value at the column.
 * @throws IllegalStateException If the value is null.
 */
inline fun <reified T> ResultSet?.get(column: Int): T {
    return getNullable<T>(column) ?: error("Value is null")
}

/**
 * Gets a nullable value from the ResultSet at the specified column.
 * @param column The column to get the value from.
 * @return The value at the column or null if the value is null.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> ResultSet?.getNullable(column: String): T? {
    if(this == null) return null

    if(Collection::class.java.isAssignableFrom(T::class.java))
        error("Use an Array instead of a Collection")
    if(Array::class.java.isAssignableFrom(T::class.java))
        return getArray(column)?.array as? T

    if(Enum::class.java.isAssignableFrom(T::class.java)) {
        val value = getString(column) ?: return null
        return java.lang.Enum.valueOf(T::class.java as Class<out Enum<*>>, value) as? T
    }

    return getObject(column) as? T
}

/**
 * Gets a nullable value from the ResultSet at the specified column.
 * @param column The column to get the value from.
 * @return The value at the column or null if the value is null.
 */
inline fun <reified T> ResultSet?.getNullable(column: Int): T? {
    if(this == null) return null
    return getNullable(metaData.getColumnName(column))
}

/**
 * Gets a single value from the ResultSet.
 * @return The single value from the ResultSet.
 * @throws IllegalStateException If there are no results in the ResultSet.
 * @throws IllegalStateException If there are more than one result in the ResultSet.
 */
inline fun <reified T> ResultSet?.singleColumn(): T {
    return singleColumnNullable<T>() ?: error("No results in ResultSet")
}

/**
 * Gets a single nullable value from the ResultSet.
 * @return The single value from the ResultSet or null if there are no results.
 * @throws IllegalStateException If there are more than one result in the ResultSet.
 */
inline fun <reified T> ResultSet?.singleColumnNullable(): T? {
    if(this == null) return null

    check(metaData?.columnCount == 1) { "ResultSet must have exactly one column" }
    check(next()) { "No results in ResultSet" }

    val value = getNullable<T>(1)
    check(!next()) { "ResultSet has more than one row" }

    return value
}

/**
 * Gets all values from the ResultSet.
 * @return A list of all values from the ResultSet.
 * @throws IllegalStateException If any value is null.
 * @throws IllegalStateException If there are more than one column in the ResultSet.
 */
inline fun <reified T> ResultSet?.allSingleColumn(): List<T> {
    val values = allSingleColumnNullable<T>()
    check(!values.any { it == null }) { "ResultSet contains null values" }

    // we've already checked there are no null values
    // & i don't want to re-allocate a new ArrayList for this op - tech
    return values as List<T>
}

/**
 * Gets all nullable values from the ResultSet.
 * @return A list of all values from the ResultSet or null if the ResultSet is null.
 * @throws IllegalStateException If there are more than one column in the ResultSet.
 */
inline fun <reified T> ResultSet?.allSingleColumnNullable(): List<T?> {
    if(this == null) return emptyList()

    check(metaData.columnCount == 1) { "ResultSet must have exactly one column" }
    val l = mutableListOf<T?>()
    while(next())
        l.add(getNullable(1))
    return l
}

/**
 * Gets a mapped value from the ResultSet at the specified column.
 * @param column The column to get the value from.
 * @param deserializer The deserializer to use to map the value.
 * @return The mapped value at the column.
 * @throws IllegalStateException If the value is null.
 * @throws IllegalStateException If the value cannot be mapped.
 */
inline fun <reified T> ResultSet?.mapped(column: String, deserializer: ColumnDeserializer<*, T>): T {
    return mappedNullable(column, deserializer) ?: error("Value is null")
}

/**
 * Gets a mapped value from the ResultSet at the specified column.
 * @param column The column to get the value from.
 * @param deserializer The deserializer to use to map the value.
 * @return The mapped value at the column.
 * @throws IllegalStateException If the value is null.
 * @throws IllegalStateException If the value cannot be mapped.
 */
inline fun <reified T> ResultSet?.mapped(column: Int, deserializer: ColumnDeserializer<*, T>): T {
    return mappedNullable(column, deserializer) ?: error("Value is null")
}

/**
 * Gets a mapped nullable value from the ResultSet at the specified column.
 * @param column The column to get the value from.
 * @param deserializer The deserializer to use to map the value.
 * @return The mapped value at the column or null if the value is null.
 * @throws IllegalStateException If the value cannot be mapped.
 * @throws IllegalStateException If the value is null.
 */
inline fun <reified T> ResultSet?.mappedNullable(column: String, deserializer: ColumnDeserializer<*, T>): T? {
    if(this == null) return null
    deserializer as ColumnDeserializer<Any, T>

    val value = getNullable<Any>(column) ?: return null
    return deserializer.fromDatabaseValue(value)
}

/**
 * Gets a mapped nullable value from the ResultSet at the specified column.
 * @param column The column to get the value from.
 * @param deserializer The deserializer to use to map the value.
 * @return The mapped value at the column or null if the value is null.
 * @throws IllegalStateException If the value cannot be mapped.
 */
inline fun <reified T> ResultSet?.mappedNullable(column: Int, deserializer: ColumnDeserializer<*, T>): T? {
    if(this == null) return null
    return mappedNullable(metaData.getColumnName(column), deserializer)
}

/**
 * Gets a single mapped value from the ResultSet.
 * @param deserializer The deserializer to use to map the value.
 * @return The single mapped value from the ResultSet.
 * @throws IllegalStateException If there are no results in the ResultSet.
 * @throws IllegalStateException If there are more than one result in the ResultSet.
 * @throws IllegalStateException If the value cannot be mapped.
 */
inline fun <reified T> ResultSet?.singleColumnMapped(deserializer: ColumnDeserializer<*, T>): T {
    return singleColumnMappedNullable(deserializer) ?: error("No results in ResultSet")
}

/**
 * Gets a single nullable mapped value from the ResultSet.
 * @param deserializer The deserializer to use to map the value.
 * @return The single mapped value from the ResultSet or null if there are no results.
 * @throws IllegalStateException If there are more than one result in the ResultSet.
 * @throws IllegalStateException If the value cannot be mapped.
 */
inline fun <reified T> ResultSet?.singleColumnMappedNullable(deserializer: ColumnDeserializer<*, T>): T? {
    if(this == null) return null

    check(metaData?.columnCount == 1) { "ResultSet must have exactly one column" }
    if(!next()) return null

    val value = mappedNullable<T>(1, deserializer)
    check(!next()) { "ResultSet has more than one row" }

    return value
}

/**
 * Gets all mapped values from the ResultSet.
 * @param deserializer The deserializer to use to map the values.
 * @return A list of all mapped values from the ResultSet.
 * @throws IllegalStateException If any value is null.
 * @throws IllegalStateException If there are more than one column in the ResultSet.
 */
inline fun <reified T> ResultSet?.allSingleColumnMapped(deserializer: ColumnDeserializer<*, T>): List<T> {
    val values = allSingleColumnMappedNullable(deserializer)
    check(!values.any { it == null }) { "ResultSet contains null values" }

    // we've already checked there are no null values
    // & i don't want to re-allocate a new ArrayList for this op - tech
    return values as List<T>
}

/**
 * Gets all nullable mapped values from the ResultSet.
 * @param deserializer The deserializer to use to map the values.
 * @return A list of all mapped values from the ResultSet or null if the ResultSet is null.
 * @throws IllegalStateException If there are more than one column in the ResultSet.
 * @throws IllegalStateException If the value cannot be mapped.
 */
inline fun <reified T> ResultSet?.allSingleColumnMappedNullable(deserializer: ColumnDeserializer<*, T>): List<T?> {
    if(this == null) return emptyList()

    check(metaData.columnCount == 1) { "ResultSet must have exactly one column" }
    val l = mutableListOf<T?>()
    while(next())
        l.add(mappedNullable(1, deserializer))
    return l
}