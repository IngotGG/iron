package gg.ingot.iron.sql.controller

import gg.ingot.iron.sql.MappedResultSet
import gg.ingot.iron.transformer.ResultTransformer
import java.sql.Connection
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * Controller implementation for handling database transactions and queries.
 * @author DebitCardz
 * @since 1.3
 */
internal class ControllerImpl(
    private val connection: Connection,
    private val resultTransformer: ResultTransformer
) : Controller {
    override fun <T : Any?> transaction(block: Controller.() -> T): Result<T> {
        return try {
            connection.autoCommit = false
            val result = block()
            connection.commit()

            Result.success(result)
        } catch (e: Exception) {
            connection.rollback()

            Result.failure(e)
        } finally {
            connection.autoCommit = true
        }
    }

    override fun query(query: String): ResultSet {
        return connection.createStatement()
            .executeQuery(query)
    }

    override fun <T : Any> query(query: String, clazz: KClass<T>): MappedResultSet<T> {
        return MappedResultSet(query(query), clazz, resultTransformer)
    }

    override fun execute(statement: String): Boolean {
        return connection.createStatement()
            .execute(statement)
    }

    override fun prepare(statement: String, vararg values: Any): ResultSet? {
        val preparedStatement = connection.prepareStatement(statement)

        require(preparedStatement.parameterMetaData.parameterCount == values.size) {
            "The number of parameters provided does not match the number of parameters in the prepared statement."
        }

        for ((index, value) in values.withIndex()) {
            preparedStatement.setObject(index + 1, value)
        }

        return if (preparedStatement.execute()) {
            preparedStatement.resultSet
        } else {
            null
        }
    }

    override fun <T : Any> prepare(statement: String, clazz: KClass<T>, vararg values: Any): MappedResultSet<T> {
        val resultSet = prepare(statement, *values)
            ?: error("No result set was returned from the prepared statement.")

        return MappedResultSet(resultSet, clazz, resultTransformer)
    }
}