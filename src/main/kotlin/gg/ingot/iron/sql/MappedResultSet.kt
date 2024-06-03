package gg.ingot.iron.sql

import gg.ingot.iron.transformer.ResultTransformer.model
import java.sql.ResultSet
import kotlin.reflect.KClass

class MappedResultSet<T: Any> internal constructor(
    private val resultSet: ResultSet,
    private val clazz: KClass<T>
){

    /**
     * Moves the cursor to the next row in the result set.
     * @return True if the cursor is moved to a valid row, false if there are no more rows.
     * @since 1.0
     */
    fun next(): Boolean {
        return resultSet.next()
    }

    /**
     * Gets the model from the result set at its current row.
     * @return The last model in the result set.
     * @since 1.0
     */
    fun get(): T {
        return resultSet.model(clazz)
    }

    /**
     * Moves the cursor to the next row in the result set and gets the model.
     * @return The last model in the result set or null if there are no rows.
     * @since 1.0
     */
    fun getNext(): T? {
        return if (next()) {
            get()
        } else {
            null
        }
    }

    /**
     * Get all the models from the result set.
     * @return A list of the mapped models.
     * @since 1.0
     */
    fun all(): List<T> {
        val models = mutableListOf<T>()
        while (resultSet.next()) {
            models.add(resultSet.model(clazz))
        }
        return models
    }

    /**
     * Closes the result set.
     * @since 1.0
     */
    fun close() {
        resultSet.close()
    }

}