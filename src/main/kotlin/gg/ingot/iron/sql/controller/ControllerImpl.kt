package gg.ingot.iron.sql.controller

import gg.ingot.iron.sql.MappedResultSet
import gg.ingot.iron.sql.SqlParameters
import gg.ingot.iron.transformer.ResultTransformer
import gg.ingot.iron.transformer.isArray
import gg.ingot.iron.transformer.isCollection
import gg.ingot.iron.transformer.isEnum
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
            val kClass = value::class
            val paramIndex = index + 1

            // parse enum values to db
            if(isEnum(kClass)) {
                when {
                    isArray(kClass) -> preparedStatement.setObject(paramIndex, (value as Array<*>).map { (it as Enum<*>).name }.toTypedArray())
                    isCollection(kClass) -> preparedStatement.setObject(paramIndex, (value as Collection<*>).map { (it as Enum<*>).name }.toTypedArray())
                    else -> preparedStatement.setObject(paramIndex, (value as Enum<*>).name)
                }
                continue
            }

            preparedStatement.setObject(paramIndex, value)
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

    override fun <T : Any> prepare(statement: String, clazz: KClass<T>, values: SqlParameters): MappedResultSet<T> {
        val resultSet = prepare(statement, values)
            ?: error("No result set was returned from the prepared statement.")

        return MappedResultSet(resultSet, clazz, resultTransformer)
    }
}