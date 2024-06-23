package gg.ingot.iron.sql

import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.transformer.ResultTransformer
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * A wrapper over result set allowing for iron-specific operations
 * @param resultSet The underlying result set
 * @param transformer The result transformer that iron uses
 * @author santio
 */
@Suppress("MemberVisibilityCanBePrivate")
class IronResultSet internal constructor(
    val resultSet: ResultSet?,
    private val transformer: ResultTransformer
) {

    /**
     * Moves the cursor forward one row from its current position. A ResultSet cursor is initially positioned before
     * the first row; the first call to the method next makes the first row the current row; the second call makes the
     * second row the current row, and so on.
     * @return Whether the current row exists.
     */
    fun next(): Boolean {
        requireNotNull(resultSet) { "The prepared statement did not return a result" }

        return resultSet.next()
    }

    /**
     * Gets the model from the result set at its current row.
     * @param clazz The class to transform the result to
     * @return The last model in the result set.
     * @since 1.0
     */
    fun <T: Any> get(clazz: KClass<T>): T {
        requireNotNull(resultSet) { "The prepared statement did not return a result" }

        return this.transformer.read(resultSet, clazz);
    }

    /**
     * Gets the model from the result set at its current row.
     * @return The last model in the result set.
     * @since 1.0
     */
    inline fun <reified T: Any> get(): T {
        return this.get(T::class)
    }

    /**
     * Moves the cursor to the next row in the result set and gets the model.
     * @return The last model in the result set or null if there are no rows.
     * @since 1.0
     */
    inline fun <reified T: Any> getNext(): T? {
        return if (next()) {
            get<T>()
        } else {
            null
        }
    }

    /**
     * Gets the model from the result set at its current row.
     * @return The model from the result set or null if the result set is empty.
     * @since 1.1
     */
    inline fun <reified T: Any> singleNullable(): T? {
        val value = getNext<T>()
        if (next()) {
            error("Expected a single or no result, but found more than one")
        }

        return value
    }

    /**
     * Gets the model from the result set at its current row.
     * Always expects exactly a single result to exist and throws an exception if there are more or less.
     * @return The model from the result set.
     * @since 1.1
     * @throws IllegalStateException If the result set is empty.
     */
    inline fun <reified T: Any> single(): T {
        return singleNullable() ?: error("Expected a single result, but found none.")
    }

    /**
     * Get all the models from the result set.
     * @return A list of the mapped models.
     * @since 1.0
     */
    inline fun <reified T: Any> all(): List<T> {
        val models = mutableListOf<T>()
        while (this.next()) models.add(get())
        return models
    }

    /**
     * Gets a nullable value from the ResultSet at the specified column.
     * @param column The column to get the value from.
     * @return The value at the column or null if the value is null.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getNullable(column: String, clazz: Class<T>): T? {
        requireNotNull(resultSet) { "The prepared statement did not return a result" }

        if(Collection::class.java.isAssignableFrom(clazz))
            error("Use an Array instead of a Collection")

        if(Array::class.java.isAssignableFrom(clazz))
            return resultSet.getArray(column)?.array as? T

        if(Enum::class.java.isAssignableFrom(clazz)) {
            val value = resultSet.getString(column) ?: return null
            return java.lang.Enum.valueOf(clazz as Class<out Enum<*>>, value) as? T
        }

        return resultSet.getObject(column) as? T
    }

    /**
     * Gets a nullable value from the ResultSet at the specified column.
     * @param column The column to get the value from.
     * @return The value at the column or null if the value is null.
     */
    inline fun <reified T> getNullable(column: String): T? {
        return this.getNullable(column, T::class.java)
    }

    /**
     * Gets a nullable value from the ResultSet at the specified column.
     * @param column The column to get the value from.
     * @return The value at the column or null if the value is null.
     */
    inline fun <reified T> getNullable(column: Int): T? {
        requireNotNull(resultSet) { "The prepared statement did not return a result" }

        return this.getNullable(resultSet.metaData.getColumnName(column), T::class.java)
    }

    /**
     * Gets a value from the ResultSet at the specified column.
     * @param column The column to get the value from.
     * @return The value at the column.
     * @throws IllegalStateException If the value is null.
     */
    inline fun <reified T> get(column: String): T {
        return this.getNullable(column, T::class.java) ?: error("Value is null")
    }

    /**
     * Gets a value from the ResultSet at the specified column.
     * @param column The column to get the value from.
     * @return The value at the column.
     * @throws IllegalStateException If the value is null.
     */
    inline fun <reified T> get(column: Int): T {
        return this.getNullable(column) ?: error("Value is null")
    }

    /**
     * Gets a single nullable value from the ResultSet. This only works if you return a
     * single column back
     * @return The single value from the ResultSet or null if there are no results.
     * @throws IllegalStateException If there are more than one result in the ResultSet.
     */
    inline fun <reified T> columnSingleNullable(deserializer: ColumnDeserializer<Any, T>? = null): T? {
        requireNotNull(resultSet) { "The prepared statement did not return a result" }

        check(resultSet.metaData?.columnCount == 1) { "ResultSet must have exactly one column" }
        check(this.next()) { "No results in ResultSet" }

        val value = if (deserializer != null) {
            getNullable<Any>(1)?.let { deserializer.fromDatabaseValue(it) }
        } else {
            get<T>(1)
        }

        check(!this.next()) { "ResultSet has more than one row" }
        return value;
    }

    /**
     * Gets a single value from the ResultSet. This only works if you are returning a single
     * column back.
     * @return The single value from the ResultSet.
     * @throws IllegalStateException If there are no results in the ResultSet.
     * @throws IllegalStateException If there are more than one result in the ResultSet.
     */
    inline fun <reified T> columnSingle(deserializer: ColumnDeserializer<Any, T>? = null): T {
        return columnSingleNullable<T>(deserializer) ?: error("No results in ResultSet")
    }

    /**
     * Gets all values from the ResultSet. This only works if you are returning a
     * single column back.
     * @return A list of all values from the ResultSet.
     * @throws IllegalStateException If any value is null.
     * @throws IllegalStateException If there are more than one column in the ResultSet.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> columnAll(deserializer: ColumnDeserializer<Any, T>? = null): List<T> {
        val values = columnsAllNullable<T>(deserializer)
        check(!values.any { it == null }) { "ResultSet contains null values" }

        return values as List<T>
    }

    /**
     * Gets all nullable values from the ResultSet. This only works if you are returning
     * a single column back.
     * @return A list of all values from the ResultSet or null if the ResultSet is null.
     * @throws IllegalStateException If there are more than one column in the ResultSet.
     */
    inline fun <reified T> columnsAllNullable(deserializer: ColumnDeserializer<Any, T>? = null): List<T?> {
        requireNotNull(resultSet) { "The prepared statement did not return a result" }

        check(resultSet.metaData.columnCount == 1) { "ResultSet must have exactly one column" }
        val l = mutableListOf<T?>()

        while(next()) {
            val value = if (deserializer != null) {
                getNullable<Any>(1)?.let { deserializer.fromDatabaseValue(it) }
            } else {
                get<T>(1)
            }

            l.add(value)
        }

        return l
    }

    /**
    * Releases this ResultSet object's database and JDBC resources immediately instead of waiting for this to happen
    * when it is automatically closed.
    */
    fun close() {
        resultSet?.close()
    }

}