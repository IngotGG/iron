package gg.ingot.iron.executor.impl

import gg.ingot.iron.Iron
import gg.ingot.iron.bindings.SqlBindings
import gg.ingot.iron.executor.IronConnection
import gg.ingot.iron.executor.transaction.Transaction
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.transformer.PlaceholderParser
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.function.Consumer
import javax.sql.rowset.CachedRowSet
import javax.sql.rowset.RowSetFactory
import javax.sql.rowset.RowSetProvider


open class BlockingIronExecutor(
    private val iron: Iron,
    private var connection: Connection? = null
): IronConnection {
    internal suspend fun <T> transaction(connection: Connection, block: suspend Transaction.() -> T): T {
        val transactionController = Transaction(iron, connection)

        return try {
            connection.autoCommit = false
            val result = block(transactionController)

            connection.commit()
            transactionController.commit()

            result
        } catch (ex: Exception) {
            connection.rollback()
            transactionController.rollback()

            throw ex
        } finally {
            connection.autoCommit = true
        }
    }

    @Suppress("DuplicatedCode")
    fun <T> transaction(block: Transaction.() -> T): T {
        return use {
            return@use runBlocking {
                return@runBlocking transaction(it, block)
            }
        }
    }

    fun transaction(block: Consumer<Transaction>) {
        return transaction<Unit> {
            block.accept(this)
        }
    }

    internal fun <T> use(block: (Connection) -> T): T {
        return if (connection?.isClosed == false) block(connection!!)
        else iron.useBlocking(block)
    }

    fun query(@Language("SQL") query: String): IronResultSet {
        logger.trace("Executing Query\n{}", query)

        return use {
            val resultSet = it.createStatement()
                .executeQuery(query)

            val cached = factory.createCachedRowSet()
            cached.populate(resultSet)

            return@use IronResultSet(cached, iron)
        }
    }

    fun prepare(@Language("SQL") statement: String, vararg values: Any?): IronResultSet {
        logger.trace("Preparing Statement\n{}", statement)

        val mappedValues = values.map { iron.resultMapper.serialize(null, it) }

        // If we have bindings, run them through the other method
        if (mappedValues.isNotEmpty() && mappedValues.all { it is SqlBindings }) {
            val first = mappedValues.first() as SqlBindings
            return prepare(statement, first, *mappedValues.drop(1).map { it as SqlBindings }.toTypedArray())
        }

        // Check to see if we have any remaining variables
        val variables = PlaceholderParser.getVariables(statement)
        if (variables.isNotEmpty()) {
            error("The statement contains variables that are not bound, make sure to bind all variables before executing the statement. Missing variables: ${variables.joinToString(", ")}")
        }

        return use {
            val preparedStatement = it.prepareStatement(statement)

            require(preparedStatement.parameterMetaData.parameterCount == mappedValues.size) {
                "The number of parameters provided does not match the number of parameters in the prepared statement."
            }

            for((index, value) in mappedValues.withIndex()) {
                preparedStatement.setObject(
                    index + 1,
                    value
                )
            }

            val resultSet = if (preparedStatement.execute()) {
                val resultSet = preparedStatement.resultSet
                val cached = factory.createCachedRowSet()
                cached.populate(resultSet)
                cached
            } else {
                null
            }

            return@use IronResultSet(resultSet, iron)
        }
    }

    fun prepare(@Language("SQL") statement: String, variable: SqlBindings, vararg variables: SqlBindings): IronResultSet {
        val concatenated = variable.concat(variables.fold(variable) { acc, binding -> acc.concat(binding) })
        val parsed = concatenated.parse(iron)

        return PlaceholderParser.parseParams(statement, parsed).let { (stmt, params) ->
            prepare(stmt, *params.toTypedArray())
        }
    }

    fun execute(@Language("SQL") statement: String): Boolean {
        logger.trace("Executing Statement\n{}", statement)

        return use { connection ->
            return@use connection.createStatement().execute(statement)
        }
    }

    private companion object {
        /** The logger for this class. */
        private val logger = LoggerFactory.getLogger(BlockingIronExecutor::class.java)

        /** The factory to use for creating [CachedRowSet]. */
        var factory: RowSetFactory = RowSetProvider.newFactory()
    }
}