@file:Suppress("DuplicatedCode")

package gg.ingot.iron.sql

import java.sql.ResultSet

/**
 * Extension function to retrieve a single value from a result set.
 * @return The value from the result set.
 */
inline fun <reified T> ResultSet.singleValue(): T {
    return singleValueNullable<T>()
        ?: error("No results in result set")
}

/**
 * Extension function to retrieve a single value from a result set.
 * @return The value from the result set or null if the value is null.
 */
inline fun <reified T> ResultSet.singleValueNullable(): T? {
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
inline fun <reified T> ResultSet.allValues(): List<T> {
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
inline fun <reified T> ResultSet.allValuesNullable(): List<T?> {
    check(metaData.columnCount == 1) { "Result set must have exactly one column" }

    val list = mutableListOf<T?>()
    while (next()) {
        list.add(getNullable<T>(1))
    }
    return list
}

/**
 * Extension function to retrieve a column from a result set
 * and cast it to the desired type.
 * @param column The column name to retrieve.
 * @return The value from the result set.
 */
inline fun <reified T> ResultSet.get(column: String): T {
    return getNullable<T>(column) ?: error("Value is null")
}

/**
 * Extension function to retrieve a column from a result set
 * and cast it to the desired type.
 * @param column The column index to retrieve.
 * @return The value from the result set.
 */
inline fun <reified T> ResultSet.get(column: Int): T {
    return getNullable<T>(column) ?: error("Value is null")
}

/**
 * Extension function to retrieve a column from a result set
 * and cast it to the desired type.
 * @param column The column name to retrieve.
 * @return The value from the result set, or null if the value is null.
 */
inline fun <reified T> ResultSet.getNullable(column: String): T? {
    // for some reason throwing it in the when clause
    // doesn't work?
    if(Array::class.java.isAssignableFrom(T::class.java)) {
        return getArray(column)?.array as? T
    }

    return when (T::class) {
        Int::class -> getInt(column)
        Double::class -> getDouble(column)
        Float::class -> getFloat(column)
        Short::class -> getShort(column)
        Long::class -> getLong(column)
        Byte::class -> getByte(column)
        ByteArray::class -> getBytes(column)
        String::class -> getString(column)
        java.sql.Date::class -> getDate(column)
        java.sql.Time::class -> getTime(column)
        java.sql.Timestamp::class -> getTimestamp(column)
        java.net.URL::class -> getURL(column)
        else -> error("Unsupported type ${T::class.simpleName}")
    } as? T
}

/**
 * Extension function to retrieve a column from a result set
 * and cast it to the desired type.
 * @param column The column index to retrieve.
 * @return The value from the result set.
 */
inline fun <reified T> ResultSet.getNullable(column: Int): T? {
    return getNullable(metaData.getColumnName(column))
}