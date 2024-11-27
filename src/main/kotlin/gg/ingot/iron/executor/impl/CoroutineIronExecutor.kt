package gg.ingot.iron.executor.impl

import gg.ingot.iron.Iron
import gg.ingot.iron.bindings.SqlBindings
import gg.ingot.iron.executor.IronConnection
import gg.ingot.iron.executor.transaction.Transaction
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.expressions.SQL
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language

open class CoroutineIronExecutor(private val iron: Iron): IronConnection {
    private val blockingExecutor = BlockingIronExecutor(iron)

    @Suppress("DuplicatedCode")
    @JvmName("transactionCoroutine")
    suspend fun <T> transaction(block: suspend Transaction.() -> T): T {
        return iron.use {
            return@use withContext(iron.settings.dispatcher) {
                return@withContext blockingExecutor.transaction(it, block)
            }
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

    suspend fun prepare(@Language("SQL") statement: String, variable: SqlBindings, vararg variables: SqlBindings): IronResultSet {
        return withContext(iron.settings.dispatcher) {
            return@withContext blockingExecutor.prepare(statement, variable, *variables)
        }
    }

    suspend fun execute(@Language("SQL") statement: String): Boolean {
        return withContext(iron.settings.dispatcher) {
            return@withContext blockingExecutor.execute(statement)
        }
    }

    suspend fun run(builder: SQL): IronResultSet {
        return withContext(iron.settings.dispatcher) {
            return@withContext blockingExecutor.run(builder)
        }
    }

    suspend fun run(builder: Sql): IronResultSet {
        return withContext(iron.settings.dispatcher) {
            return@withContext blockingExecutor.run(builder)
        }
    }
}