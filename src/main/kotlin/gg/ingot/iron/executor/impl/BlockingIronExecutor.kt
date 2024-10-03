package gg.ingot.iron.executor.impl

import gg.ingot.iron.Iron
import gg.ingot.iron.executor.IronConnection
import gg.ingot.iron.executor.transaction.Transaction
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.sql.binding.SqlBindings
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.function.Consumer

open class BlockingIronExecutor(
    private val iron: Iron,
    private val connection: Connection? = null
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
        return if (connection != null) {
            block(connection)
        } else {
            iron.useBlocking(block)
        }
    }

    fun query(@Language("SQL") query: String): IronResultSet {
        logger.trace("Executing Query\n{}", query)

        return use {
            val resultSet = it.createStatement()
                .executeQuery(query)
            return@use IronResultSet(resultSet, iron)
        }
    }

    fun prepare(@Language("SQL") statement: String, vararg values: Any?): IronResultSet {
        logger.trace("Preparing Statement\n{}", statement)

        // Check to see if we have any remaining variables
        val variables = iron.placeholderTransformer.getVariables(statement)
        if (variables.isNotEmpty()) {
            error("The statement contains variables that are not bound, make sure to bind all variables before executing the statement. Missing variables: ${variables.joinToString(", ")}")
        }

        return use {
            val preparedStatement = it.prepareStatement(statement)

            require(preparedStatement.parameterMetaData.parameterCount == values.size) {
                "The number of parameters provided does not match the number of parameters in the prepared statement."
            }

            for((index, value) in values.withIndex()) {
                preparedStatement.setObject(
                    index + 1,
                    iron.placeholderTransformer.convert(value, iron.settings.serialization)
                )
            }

            val resultSet = if (preparedStatement.execute()) {
                preparedStatement.resultSet
            } else {
                null
            }

            return@use IronResultSet(resultSet, iron)
        }
    }

    fun prepare(@Language("SQL") statement: String, variable: SqlBindings, vararg variables: SqlBindings): IronResultSet {
        val concatenated = variable.concat(variables.fold(variable) { acc, binding -> acc.concat(binding) })
        val parsed = concatenated.parse(iron)

        return iron.placeholderTransformer.parseParams(statement, parsed).let { (stmt, params) ->
            prepare(stmt, *params.toTypedArray())
        }
    }

    fun execute(@Language("SQL") statement: String): Boolean {
        logger.trace("Executing Statement\n{}", statement)

        return use {
            return@use it.createStatement()
                .execute(statement)
        }
    }

    private companion object {
        /** The logger for this class. */
        private val logger = LoggerFactory.getLogger(BlockingIronExecutor::class.java)
    }
}