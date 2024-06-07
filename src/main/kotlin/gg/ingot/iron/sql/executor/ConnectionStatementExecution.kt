package gg.ingot.iron.sql.executor

import gg.ingot.iron.sql.MappedResultSet
import gg.ingot.iron.transformer.ResultTransformer
import java.sql.Connection
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * Executes statements on the database.
 * @author DebitCardz
 * @since 1.3
 */
internal class ConnectionStatementExecution(
    private val conn: Connection,
    private val resultTransformer: ResultTransformer
) : StatementExecutor {
    override fun <T> transaction(block: StatementExecutor.() -> T): T {
        try {
            conn.autoCommit = false
            val result = block()
            conn.commit()
            return result
        } catch (e: Exception) {
            conn.rollback()
            throw e
        } finally {
            conn.autoCommit = true
        }
    }

    override fun execute(statement: String): Boolean {
        return conn.createStatement()
            .execute(statement)
    }

    override fun query(query: String): ResultSet {
        return conn.createStatement()
            .executeQuery(query)
    }

    override fun <T : Any> queryMapped(query: String, clazz: KClass<T>): MappedResultSet<T> {
        return MappedResultSet(query(query), clazz, resultTransformer)
    }

    override fun prepare(statement: String, vararg values: Any): ResultSet? {
        val preparedStatement = conn.prepareStatement(statement)

        for ((index, value) in values.withIndex()) {
            preparedStatement.setObject(index + 1, value)
        }

        return if (preparedStatement.execute()) {
            preparedStatement.resultSet
        } else {
            null
        }
    }

    override fun <T : Any> prepareMapped(statement: String, clazz: KClass<T>, vararg values: Any): MappedResultSet<T> {
        val resultSet = prepare(statement, *values)
            ?: error("No result set was returned from the prepared statement.")

        return MappedResultSet(resultSet, clazz, resultTransformer)
    }
}