package gg.ingot.iron.executor.impl

import gg.ingot.iron.Iron
import gg.ingot.iron.executor.IronConnection
import gg.ingot.iron.executor.transaction.Transaction
import gg.ingot.iron.sql.binding.SqlBindings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import org.intellij.lang.annotations.Language

open class DeferredIronExecutor(private val iron: Iron): IronConnection {
    private val blockingExecutor = BlockingIronExecutor(iron)

    private val scope = CoroutineScope(iron.settings.dispatcher + SupervisorJob())

    fun <T> transaction(block: suspend Transaction.() -> T): Deferred<T> {
        return scope.async {
            return@async iron.use {
                return@use blockingExecutor.transaction(it, block)
            }
        }
    }

    fun query(@Language("SQL") query: String) = scope.async {
        blockingExecutor.query(query)
    }


    fun prepare(@Language("SQL") statement: String, vararg values: Any?) = scope.async {
        blockingExecutor.prepare(statement, *values)
    }

    fun prepare(@Language("SQL") statement: String, variable: SqlBindings, vararg variables: SqlBindings) = scope.async {
        blockingExecutor.prepare(statement, variable, *variables)
    }

    fun prepare(@Language("SQL") statement: String) = scope.async {
        blockingExecutor.prepare(statement)
    }

    fun execute(@Language("SQL") statement: String): Deferred<Boolean> = scope.async {
        blockingExecutor.execute(statement)
    }
}