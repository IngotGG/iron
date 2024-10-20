package gg.ingot.iron.sql.types

import gg.ingot.iron.sql.Sql

/**
 * Represents a type-safe column in the database.
 * @param name The name of the column
 * @param table The table the column belongs to
 * @author santio
 * @since 2.0
 */
@Suppress("MemberVisibilityCanBePrivate")
data class Column(
    val name: String,
    val table: String? = null,
): Reference() {
    /**
     * Gets the fully qualified name of the column, including the table if it exists.
     * @param sql The sql instance
     * @return The fully qualified name of the column
     */
    fun qualified(sql: Sql): String {
        return if (table == null) sql.driver.literal(name)
        else "${sql.driver.literal(table)}.$name"
    }

    /**
     * Gets the fully qualified name of the column, including the table if it exists.
     * @param sql The sql instance
     * @return The fully qualified name of the column
     */
    override fun asString(sql: Sql): String {
        val qualified = qualified(sql)
        val selectable = RefValue(qualified)

        val compiled = functions.fold(selectable) { acc, function ->
            RefValue(function(acc, sql))
        }

        return if (alias == null) compiled.asString(sql)
        else "${compiled.asString(sql)} AS ${sql.driver.literal(alias!!)}"
    }
}

/**
 * Reference a column by name.
 * @param name The name of the column
 * @return A type-safe column
 */
fun column(name: String): Column {
    return Column(name = name)
}

/**
 * Reference a column by name and table.
 * @param table The table the column belongs to
 * @param name The name of the column
 * @return A type-safe column
 */
fun column(table: String, name: String): Column {
    return Column(name = name, table = table)
}

/**
 * Get the average of a selectable value.
 * @param value The value to average
 * @return A type-safe column
 */
fun avg(value: Any): Reference {
    val compiled = RefValue.of(value)
    compiled.functions.add { "AVG(${this.asString(it)})" }
    return compiled
}

/**
 * Get the count of a selectable value.
 * @param value The value to count
 * @return A type-safe column
 */
fun count(value: Any): Reference {
    val compiled = RefValue.of(value)
    compiled.functions.add { "COUNT(${this.asString(it)})" }
    return compiled
}

/**
 * Get the maximum of a selectable value.
 * @param value The value to maximum
 * @return A type-safe column
 */
fun max(value: Any): Reference {
    val compiled = RefValue.of(value)
    compiled.functions.add { "MAX(${this.asString(it)})" }
    return compiled
}

/**
 * Get the minimum of a selectable value.
 * @param value The value to minimum
 * @return A type-safe column
 */
fun min(value: Any): Reference {
    val compiled = RefValue.of(value)
    compiled.functions.add { "MIN(${this.asString(it)})" }
    return compiled
}

/**
 * Get the sum of a selectable value.
 * @param value The value to sum
 * @return A type-safe column
 */
fun sum(value: Any): Reference {
    val compiled = RefValue.of(value)
    compiled.functions.add { "SUM(${this.asString(it)})" }
    return compiled
}

/**
 * Get the ceiling of a selectable value.
 * @param value The value to ceil
 * @return A type-safe column
 */
fun ceil(value: Any): Reference {
    val compiled = RefValue.of(value)
    compiled.functions.add { "CEIL(${this.asString(it)})" }
    return compiled
}

/**
 * Get the floor of a selectable value.
 * @param value The value to floor
 * @return A type-safe column
 */
fun floor(value: Any): Reference {
    val compiled = RefValue.of(value)
    compiled.functions.add { "FLOOR(${this.asString(it)})" }
    return compiled
}

/**
 * Get the round of a selectable value.
 * @param value The value to round
 * @return A type-safe column
 */
fun round(value: Any): Reference {
    val compiled = RefValue.of(value)
    compiled.functions.add { "ROUND(${this.asString(it)})" }
    return compiled
}

/**
 * Get the absolute value of a selectable value.
 * @param value The value to absolute
 * @return A type-safe column
 */
fun abs(value: Any): Reference {
    val compiled = RefValue.of(value)
    compiled.functions.add { "ABS(${this.asString(it)})" }
    return compiled
}

/**
 * Returns the first non-null value from the given values.
 * @param value The first value
 * @param values The remaining values
 * @return A type-safe column
 */
fun coalesce(value: Any, vararg values: Any): Reference {
    val compiled = RefValue.of(value)
    val others = values.map { RefValue.of(it) }
    compiled.functions.add { sql -> "COALESCE(${this.asString(sql)}, ${others.joinToString { it.asString(sql) }})" }
    return compiled
}