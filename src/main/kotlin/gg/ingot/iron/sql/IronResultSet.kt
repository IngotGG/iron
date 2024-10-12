package gg.ingot.iron.sql

import gg.ingot.iron.Iron
import gg.ingot.iron.serialization.ColumnDeserializer
import java.sql.Connection
import java.sql.ResultSet

/**
 * A wrapper over result set allowing for iron-specific operations
 * @param resultSet The underlying result set
 * @param iron The iron instance
 * @author Santio, DebitCardz
 * @since 1.0
 */
@Suppress("MemberVisibilityCanBePrivate")
class IronResultSet internal constructor(
    val connection: Connection,
    val resultSet: ResultSet?,
    val iron: Iron,
): AutoCloseable {

//    Core Functions

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
     * Releases this ResultSet object's database and JDBC resources immediately instead of waiting for this to happen
     * when it is automatically closed.
     */
    override fun close() {
        connection.close()
        resultSet?.close()
    }

//    Get Functions

    /**
     * Gets the model from the result set at its current row.
     * @param clazz The class to transform the result to
     * @return The model in the result set, or null if there are no more results.
     * @since 1.0
     */
    @Suppress("UNCHECKED_CAST")
    fun <T: Any> get(
        clazz: Class<T>,
        columnLabel: String? = null,
        deserializer: ColumnDeserializer<*, T>? = null,
        json: Boolean = false
    ): T? {
        requireNotNull(resultSet) { "The prepared statement did not return a result" }
        return iron.resultMapper.read(resultSet, columnLabel, clazz, deserializer,  json) as T?
    }

    /**
     * Gets the model from the result set at its current row.
     * @param columnLabel The column label to read from a value.
     * @return The model in the result set, or null if there are no more results.
     */
    @JvmOverloads
    inline fun <reified T : Any> get(
        columnLabel: String? = null,
        deserializer: ColumnDeserializer<*, T>? = null,
        json: Boolean = false
    ): T? = get(T::class.java, columnLabel, deserializer, json)

    /**
     * Gets the model from the result set at its current row.
     * @param index The index of the column to read from.
     * @return The model in the result set, or null if there are no more results.
     */
    inline fun <reified T : Any> get(
        index: Int,
        deserializer: ColumnDeserializer<*, T>? = null,
        json: Boolean = false
    ): T? = get(T::class.java, resultSet?.metaData?.getColumnLabel(index), deserializer, json)

//    GetNext Functions

    /**
     * Moves the cursor to the next row in the result set and gets the model.
     * @param clazz The class to transform the result to
     * @return The model in the result set, or null if there are no more results.
     */
    fun <T: Any> getNext(
        clazz: Class<T>,
        deserializer: ColumnDeserializer<*, T>? = null,
        json: Boolean = false
    ): T? {
        return if (next()) get(clazz = clazz, deserializer = deserializer, json = json) else null
    }

    /**
     * Moves the cursor to the next row in the result set and gets the model.
     * @return The model in the result set, or null if there are no more results.
     */
    inline fun <reified T: Any> getNext(
        deserializer: ColumnDeserializer<*, T>? = null,
        json: Boolean = false
    ): T? = getNext(T::class.java, deserializer, json)

//    SingleNullable Functions

    /**
     * Retrieve a single result from the result set.
     * If a class annotated with [gg.ingot.iron.annotations.Model] is passed, it will be transformed into a model.
     * If not then the first column will be returned as the result.
     *
     * Calling this function will close the result set.
     *
     * @param clazz The class to transform the result to
     * @param deserializer The deserializer to use for the result.
     * @return The single result from the result set.
     */
    fun <T : Any> singleNullable(
        clazz: Class<T>,
        deserializer: ColumnDeserializer<*, T>? = null,
        json: Boolean = false
    ): T? {
        val value = getNext(clazz, deserializer = deserializer, json = json)

        if(next()) {
            error("Expected a single or no result, but found more than one")
        }

        close()
        return value
    }

    /**
     * Retrieve a single result from the result set.
     * If a class annotated with [gg.ingot.iron.annotations.Model] is passed, it will be transformed into a model.
     * If not then the first column will be returned as the result.
     *
     * Calling this function will close the result set.
     *
     * @param deserializer The deserializer to use for the result.
     * @return The single result from the result set.
     */
    inline fun <reified T : Any> singleNullable(
        deserializer: ColumnDeserializer<*, T>? = null,
        json: Boolean = false
    ): T? = singleNullable(T::class.java, deserializer, json)

//    Single Functions

    /**
     * Retrieve a single result from the result set.
     * If a class annotated with [gg.ingot.iron.annotations.Model] is passed, it will be transformed into a model.
     * If not then the first column will be returned as the result.
     *
     * Calling this function will close the result set.
     *
     * @param deserializer The deserializer to use for the result.
     * @return The single result from the result set.
     */
    @JvmOverloads
    fun <T: Any> single(
        clazz: Class<T>,
        deserializer: ColumnDeserializer<*, T>? = null,
        json: Boolean = false
    ): T {
        return singleNullable(clazz, deserializer, json) ?: error("Expected a single result, but found none.")
    }

    /**
     * Retrieve a single result from the result set.
     * If a class annotated with [gg.ingot.iron.annotations.Model] is passed, it will be transformed into a model.
     * If not then the first column will be returned as the result.
     *
     * Calling this function will close the result set.
     *
     * @param deserializer The deserializer to use for the result.
     * @return The single result from the result set.
     */
    inline fun <reified T : Any> single(
        deserializer: ColumnDeserializer<*, T>? = null,
        json: Boolean = false
    ): T = single(T::class.java, deserializer, json)

//    AllNullable Functions

    /**
     * Retrieve all results from the result set.
     * If a class annotated with [gg.ingot.iron.annotations.Model] is passed, it will be transformed into a model.
     * If not then the first column will be returned as the result.
     *
     * Calling this function will close the result set.
     *
     * @param deserializer The deserializer to use for the result.
     * @return The results from the result set.
     */
    fun <T: Any> allNullable(
        clazz: Class<T>,
        deserializer: ColumnDeserializer<*, T>? = null,
        json: Boolean = false
    ): List<T?> {
        val v = mutableListOf<T?>()

        println("Current row: ${resultSet!!.row}")
        while(next()) {
            v.add(get(clazz, deserializer = deserializer, json = json))
        }

        close()
        return v
    }

    /**
     * Retrieve all results from the result set.
     * If a class annotated with [gg.ingot.iron.annotations.Model] is passed, it will be transformed into a model.
     * If not then the first column will be returned as the result.
     *
     * Calling this function will close the result set.
     *
     * @param deserializer The deserializer to use for the result.
     * @return The results from the result set.
     */
    inline fun <reified T : Any> allNullable(
        deserializer: ColumnDeserializer<*, T>? = null,
        json: Boolean = false
    ): List<T?> = allNullable(T::class.java, deserializer, json)

//    All Functions

    /**
     * Retrieve all results from the result set.
     * If a class annotated with [gg.ingot.iron.annotations.Model] is passed, it will be transformed into a model.
     * If not then the first column will be returned as the result.
     *
     * Calling this function will close the result set.
     *
     * @param deserializer The deserializer to use for the result.
     * @return The results from the result set.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T: Any> all(
        clazz: Class<T>,
        deserializer: ColumnDeserializer<*, T>? = null,
        json: Boolean = false
    ): List<T> {
        val v = allNullable(clazz, deserializer, json)
        check(!v.any { it == null }) { "ResultSet contains null values" }

        return v as List<T>
    }

    /**
     * Retrieve all results from the result set.
     * If a class annotated with [gg.ingot.iron.annotations.Model] is passed, it will be transformed into a model.
     * If not then the first column will be returned as the result.
     *
     * Calling this function will close the result set.
     *
     * @param deserializer The deserializer to use for the result.
     * @return The results from the result set.
     */
    inline fun <reified T : Any> all(
        deserializer: ColumnDeserializer<*, T>? = null,
        json: Boolean = false
    ): List<T> =  all(T::class.java, deserializer, json)
}
