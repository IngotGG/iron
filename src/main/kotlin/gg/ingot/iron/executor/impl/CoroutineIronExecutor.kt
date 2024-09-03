package gg.ingot.iron.executor.impl

import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.executor.IronConnection
import gg.ingot.iron.executor.transaction.Transaction
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.sql.params.SqlParamsBuilder
import gg.ingot.iron.sql.params.sqlParams
import gg.ingot.iron.transformer.PlaceholderTransformer
import org.slf4j.LoggerFactory
import java.util.function.Consumer

open class CoroutineIronExecutor(private val iron: Iron): IronConnection {
    @JvmName("transactionCoroutine")
    suspend fun <T> transaction(block: suspend Transaction.() -> T): T {
        val transactionController = Transaction(iron)

        return iron.use {
            return@use try {
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

    suspend fun transaction(block: Consumer<Transaction>) {
        return transaction {
            block.accept(this)
        }
    }

    suspend fun query(query: String): IronResultSet {
        logger.trace("Executing Query\n{}", query)

        return iron.use {
            val resultSet = it.createStatement()
                .executeQuery(query)
            return@use IronResultSet(resultSet, iron.settings.serialization, iron.resultTransformer)
        }
    }

    suspend fun prepare(statement: String, vararg values: Any?): IronResultSet {
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

        return iron.use {
            val preparedStatement = it.prepareStatement(statement)

            require(preparedStatement.parameterMetaData.parameterCount == values.size) {
                "The number of parameters provided does not match the number of parameters in the prepared statement."
            }

            for((index, value) in values.withIndex()) {
                preparedStatement.setObject(
                    index + 1,
                    PlaceholderTransformer.convert(value, iron.settings.serialization)
                )
            }

            val resultSet = if (preparedStatement.execute()) {
                preparedStatement.resultSet
            } else {
                null
            }

            return@use IronResultSet(resultSet, iron.settings.serialization, iron.resultTransformer)
        }
    }

    suspend fun prepare(statement: String, model: SqlParamsBuilder): IronResultSet {
        return prepare(statement, model.build(iron.modelTransformer))
    }

    suspend fun execute(statement: String): Boolean {
        logger.trace("Executing Statement\n{}", statement)

        return iron.use {
            return@use it.createStatement()
                .execute(statement)
        }
    }

    private companion object {
        /** The logger for this class. */
        private val logger = LoggerFactory.getLogger(CoroutineIronExecutor::class.java)
    }
}