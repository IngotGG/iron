package gg.ingot.iron.executor.impl

import gg.ingot.iron.Iron
import gg.ingot.iron.executor.IronConnection
import gg.ingot.iron.executor.transaction.Transaction
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.sql.params.SqlParams
import gg.ingot.iron.sql.params.SqlParamsBuilder
import kotlinx.coroutines.*
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.function.Consumer

open class CoroutineIronExecutor(private val iron: Iron): IronConnection {
    private val blockingExecutor = BlockingIronExecutor(iron)

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

    suspend fun query(@Language("SQL") query: String): IronResultSet {
        return withContext(iron.settings.dispatcher) {
            return@withContext blockingExecutor.query(query)
        }
    }

    suspend fun prepare(@Language("SQL") statement: String, vararg values: Any?): IronResultSet {
        return withContext(iron.settings.dispatcher) {
            return@withContext blockingExecutor.prepare(statement, *values)
        }
    }

    suspend fun prepare(@Language("SQL") statement: String, model: SqlParamsBuilder): IronResultSet {
        return withContext(iron.settings.dispatcher) {
            return@withContext blockingExecutor.prepare(statement, model)
        }
    }

    suspend fun prepare(@Language("SQL") statement: String, values: SqlParams): IronResultSet {
        return withContext(iron.settings.dispatcher) {
            return@withContext blockingExecutor.prepare(statement, values)
        }
    }

    suspend fun execute(statement: String): Boolean {
        return withContext(iron.settings.dispatcher) {
            return@withContext blockingExecutor.execute(statement)
        }
    }

    private companion object {
        /** The logger for this class. */
        private val logger = LoggerFactory.getLogger(CoroutineIronExecutor::class.java)
    }
}