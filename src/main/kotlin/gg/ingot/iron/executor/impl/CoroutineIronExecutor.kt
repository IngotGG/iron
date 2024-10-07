package gg.ingot.iron.executor.impl

import gg.ingot.iron.Iron
import gg.ingot.iron.executor.IronConnection
import gg.ingot.iron.executor.transaction.Transaction
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.sql.binding.SqlBindings
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language
import java.util.function.Consumer

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
}