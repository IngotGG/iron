package gg.ingot.iron.sql

import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.serialization.JsonAdapter
import gg.ingot.iron.serialization.SerializationAdapter
import gg.ingot.iron.transformer.ResultTransformer
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * A wrapper over result set allowing for iron-specific operations
 * @param resultSet The underlying result set
 * @param transformer The result transformer that iron uses
 * @author Santio, DebitCardz
 */
@Suppress("MemberVisibilityCanBePrivate")
class IronResultSet internal constructor(
    val resultSet: ResultSet?,
    val serializationAdapter: SerializationAdapter?,
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
    fun <T: Any> get(clazz: KClass<T>, columnLabel: String? = null): T? {
        requireNotNull(resultSet) { "The prepared statement did not return a result" }

        return transformer.read(resultSet, clazz, columnLabel)
    }

    /**
     * Gets the model from the result set at its current row.
     * @return The last model in the result set.
     * @since 1.0
     */
    inline fun <reified T: Any> get(): T? {
        return this.get(T::class)
    }

    /**
     * Gets the model from the result set at its current row.
     * @param columnLabel The column label to read from a value.
     * @return The last model in the result set.
     */
    inline fun <reified T : Any> get(columnLabel: String): T? {
        requireNotNull(resultSet) { "The prepared statement did not return a result" }
        return get(T::class, columnLabel)
    }

    /**
     * Gets the model from the result set at its current row.
     * @param index The index of the column to read from.
     * @return The last model in the result set.
     */
    inline fun <reified T : Any> get(index: Int): T? {
        requireNotNull(resultSet) { "The prepared statement did not return a result" }
        return get(T::class, resultSet.metaData.getColumnLabel(index))
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
     * Retrieve a single result from the result set.
     * If a class annotated with [gg.ingot.iron.annotations.Model] is passed, it will be transformed into a model.
     * If not then the first column will be returned as the result.
     * @param deserializer The deserializer to use for the result.
     * @return The single result from the result set.
     */
    inline fun <reified T : Any> singleNullable(deserializer: ColumnDeserializer<*, T>? = null): T? {
        if(deserializer != null) {
            val value = getNext<Any>()
            if(next()) {
                error("Expected a single or no result, but found more than one")
            }

            if(value == null) return null

            deserializer as ColumnDeserializer<Any, T>
            return deserializer.fromDatabaseValue(value)
        } else {
            val value = getNext<T>()
            if(next()) {
                error("Expected a single or no result, but found more than one")
            }

            return value
        }
    }

    /**
     * Retrieve a single result from the result set.
     * If a class annotated with [gg.ingot.iron.annotations.Model] is passed, it will be transformed into a model.
     * If not then the first column will be returned as the result.
     * @param deserializer The deserializer to use for the result.
     * @return The single result from the result set.
     */
    inline fun <reified T : Any> single(deserializer: ColumnDeserializer<*, T>? = null): T {
        return singleNullable(deserializer) ?: error("Expected a single result, but found none.")
    }

    /**
     * Retrieve a single result from the result set while deserializing the fields from json
     * @return The single result from the result set.
     */
    inline fun <reified T: Any> singleJson(): T {
        return singleJsonNullable<T>() ?: error("Expected a single result, but found none.")
    }

    /**
     * Retrieve a single result from the result set while deserializing the fields from json
     * @return The single result from the result set.
     */
    inline fun <reified T: Any> singleJsonNullable(): T? {
        requireNotNull(serializationAdapter) { "A serializer adapter has not been passed through IronSettings, you will not be able to automatically deserialize JSON." }

        return singleNullable<T>(JsonAdapter(
            serializationAdapter,
            T::class.java
        ))
    }

    /**
     * Retrieve all results from the result set.
     * If a class annotated with [gg.ingot.iron.annotations.Model] is passed, it will be transformed into a model.
     * If not then the first column will be returned as the result.
     * @param deserializer The deserializer to use for the result.
     * @return The results from the result set.
     */
    inline fun <reified T : Any> allNullable(deserializer: ColumnDeserializer<*, T>? = null): List<T?> {
        val v = mutableListOf<T?>()

        while(next()) {
            if(deserializer != null) {
                val value = get<Any>()
                if(value != null) {
                    deserializer as ColumnDeserializer<Any, T>
                    v.add(deserializer.fromDatabaseValue(value))
                } else {
                    v.add(null)
                }
            } else {
                v.add(get<T>())
            }
        }

        return v
    }

    /**
     * Retrieve all results from the result set.
     * If a class annotated with [gg.ingot.iron.annotations.Model] is passed, it will be transformed into a model.
     * If not then the first column will be returned as the result.
     * @param deserializer The deserializer to use for the result.
     * @return The results from the result set.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> all(deserializer: ColumnDeserializer<*, T>? = null): List<T> {
        val v = allNullable<T>(deserializer)
        check(!v.any { it == null }) { "ResultSet contains null values" }

        return v as List<T>
    }

    /**
     * Retrieve all results from the result set while deserializing the fields from json
     * @return The single result from the result set.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T: Any> allJson(): List<T> {
        val v = allJsonNullable<T>()
        check(!v.any { it == null }) { "ResultSet contains null values" }

        return v as List<T>
    }

    /**
     * Retrieve a single result from the result set while deserializing the fields from json
     * @return The single result from the result set.
     */
    inline fun <reified T: Any> allJsonNullable(): List<T?> {
        requireNotNull(serializationAdapter) { "A serializer adapter has not been passed through IronSettings, you will not be able to automatically deserialize JSON." }

        return allNullable<T>(JsonAdapter(
            serializationAdapter,
            T::class.java
        ))
    }

    /**
    * Releases this ResultSet object's database and JDBC resources immediately instead of waiting for this to happen
    * when it is automatically closed.
    */
    fun close() {
        resultSet?.close()
    }
}
