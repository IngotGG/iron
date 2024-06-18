@file:Suppress("DuplicatedCode")

package gg.ingot.iron.sql

import gg.ingot.iron.serialization.ColumnDeserializer
import java.sql.ResultSet

/**
 * Extension function to retrieve a single value from a result set.
 * @return The value from the result set.
 */
inline fun <reified T> ResultSet?.singleValue(): T {
    return singleValueNullable<T>()
        ?: error("No results in result set")
}

/**
 * Extension function to retrieve a single value from a result set.
 * @return The value from the result set or null if the value is null.
 */
inline fun <reified T> ResultSet?.singleValueNullable(): T? {
    if(this == null) {
        return null
    }

    check(metaData.columnCount == 1) { "Result set must have exactly one column" }
    check(next()) { "No results in result set" }

    val value = get<T>(1)

    check(!next()) { "More than one result in result set" }

    return value
}

/**
 * Extension function to retrieve all values from a result set.
 * @return A list of values from the result set.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> ResultSet?.allValues(): List<T> {
    val values = allValuesNullable<T>()
    check(!values.any { it == null }) { "Result set contains null values" }

    // we've already checked there are no null values
    // & i don't want to re-allocate a new ArrayList for this op - tech
    return values as List<T>
}

/**
 * Extension function to retrieve all values from a result set.
 * @return A list of values from the result set.
 */
inline fun <reified T> ResultSet?.allValuesNullable(): List<T?> {
    if(this == null) {
        return emptyList()
    }

    check(metaData.columnCount == 1) { "Result set must have exactly one column" }

    val list = mutableListOf<T?>()
    while (next()) {
        list.add(getNullable<T>(1))
    }
    return list
}

inline fun <reified T> ResultSet?.singleValueDeserialized(deserializer: ColumnDeserializer<*, T>): T {
    return singleValueDeserializedNullable(deserializer)
        ?: error("No results in result set")
}

inline fun <reified T> ResultSet?.singleValueDeserializedNullable(deserializer: ColumnDeserializer<*, T>): T? {
    if(this == null) {
        return null
    }

    check(metaData.columnCount == 1) { "Result set must have exactly one column" }
    check(next()) { "No results in result set" }

    val value = getDeserializedNullable<T>(1, deserializer)

    check(!next()) { "More than one result in result set" }

    return value
}

/**
 * Extension function to retrieve a column from a result set
 * and cast it to the desired type.
 * @param column The column name to retrieve.
 * @return The value from the result set.
 */
inline fun <reified T> ResultSet?.get(column: String): T {
    return getNullable<T>(column) ?: error("Value is null")
}

/**
 * Extension function to retrieve a column from a result set
 * and cast it to the desired type.
 * @param column The column index to retrieve.
 * @return The value from the result set.
 */
inline fun <reified T> ResultSet?.get(column: Int): T {
    return getNullable<T>(column) ?: error("Value is null")
}

/**
 * Extension function to retrieve a column from a result set
 * and cast it to the desired type.
 * @param column The column name to retrieve.
 * @return The value from the result set, or null if the value is null.
 */
inline fun <reified T> ResultSet?.getNullable(column: String): T? {
    if(this == null) {
        return null
    }

    // use array and convert to a collection from that if you need to
    if(Collection::class.java.isAssignableFrom(T::class.java)) {
        error("Use the Array type instead of a Collection")
    }

    // for some reason throwing it in the when clause
    // doesn't work?
    if(Array::class.java.isAssignableFrom(T::class.java)) {
        return getArray(column)?.array as? T
    }

    // enum checks
    if(Enum::class.java.isAssignableFrom(T::class.java)) {
        val value = getString(column)
            ?: return null

        val clazz = T::class.java as Class<out Enum<*>>
        return java.lang.Enum.valueOf(clazz, value) as? T
    }

    // jdbc drivers like postgres can have their own types
    // like PGobject so we just have to allow everything to pass
    return getObject(column) as? T
}

/**
 * Extension function to retrieve a column from a result set
 * and cast it to the desired type.
 * @param column The column index to retrieve.
 * @return The value from the result set.
 */
inline fun <reified T> ResultSet?.getNullable(column: Int): T? {
    if(this == null) {
        return null
    }

    return getNullable(metaData.getColumnName(column))
}

inline fun <reified T> ResultSet?.getDeserialized(column: String, deserializer: ColumnDeserializer<*, T>): T {
    return getDeserializedNullable(column, deserializer)
        ?: error("Value is null")
}

inline fun <reified T> ResultSet?.getDeserialized(column: Int, deserializer: ColumnDeserializer<*, T>): T {
    return getDeserializedNullable(column, deserializer)
        ?: error("Value is null")
}

inline fun <reified T> ResultSet?.getDeserializedNullable(column: String, deserializer: ColumnDeserializer<*, T>): T? {
    if(this == null) {
        return null
    }
    deserializer as ColumnDeserializer<Any, T>

    val value = getNullable<Any>(column) ?: return null
    return deserializer.fromDatabaseValue(value)
}

inline fun <reified T> ResultSet?.getDeserializedNullable(column: Int, deserializer: ColumnDeserializer<*, T>): T? {
    if(this == null) {
        return null
    }

    return getDeserializedNullable(metaData.getColumnName(column), deserializer)
}