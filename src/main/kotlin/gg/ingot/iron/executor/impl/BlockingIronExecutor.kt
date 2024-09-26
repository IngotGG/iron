package gg.ingot.iron.executor.impl

import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.executor.IronConnection
import gg.ingot.iron.executor.transaction.Transaction
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.sql.params.SqlParams
import gg.ingot.iron.sql.params.SqlParamsBuilder
import gg.ingot.iron.sql.params.sqlParams
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.function.Consumer

open class BlockingIronExecutor(private val iron: Iron): IronConnection {
    fun <T> transaction(block: Transaction.() -> T): T {
        val transactionController = Transaction(iron)

        return iron.useBlocking {
            return@useBlocking try {
                it.autoCommit = false

                val result = block(transactionController)

                it.commit()
                transactionController.commit()

                result
            } catch (ex: Exception) {
                it.rollback()
                transactionController.rollback()

                throw ex
            } finally {
                it.autoCommit = true
            }
        }
    }

    fun transaction(block: Consumer<Transaction>) {
        return transaction<Unit> {
            block.accept(this)
        }
    }

    fun query(@Language("SQL") query: String): IronResultSet {
        logger.trace("Executing Query\n{}", query)

        return iron.useBlocking {
            val resultSet = it.createStatement()
                .executeQuery(query)
            return@useBlocking IronResultSet(resultSet, iron)
        }
    }

    fun prepare(@Language("SQL") statement: String, vararg values: Any?): IronResultSet {
        var params: SqlParamsBuilder? = null

        for (model in values) {
            if (model == null) {
                continue
            }

            if (model.javaClass.isAnnotationPresent(Model::class.java)) {
                if (params == null) params = sqlParams(mapOf())
                params + model
            }
        }

        if (params != null) {
            return prepare(statement, params)
        }

        logger.trace("Preparing Statement\n{}", statement)

        return iron.useBlocking {
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

            return@useBlocking IronResultSet(resultSet, iron)
        }
    }

    fun prepare(@Language("SQL") statement: String, model: SqlParamsBuilder): IronResultSet {
        return prepare(statement, model.build(iron))
    }

    fun prepare(@Language("SQL") statement: String, values: SqlParams): IronResultSet {
        return this.parseParams(statement, values).let { (stmt, params) ->
            prepare(stmt, *params.toTypedArray())
        }
    }


    fun execute(@Language("SQL") statement: String): Boolean {
        logger.trace("Executing Statement\n{}", statement)

        return iron.useBlocking {
            return@useBlocking it.createStatement()
                .execute(statement)
        }
    }

    private companion object {
        /** The logger for this class. */
        private val logger = LoggerFactory.getLogger(BlockingIronExecutor::class.java)
    }
}