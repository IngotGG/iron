package gg.ingot.iron.executor.impl

import gg.ingot.iron.Iron
import gg.ingot.iron.executor.IronConnection
import gg.ingot.iron.executor.transaction.Transaction
import gg.ingot.iron.sql.params.SqlParams
import gg.ingot.iron.sql.params.SqlParamsBuilder
import kotlinx.coroutines.*
import org.intellij.lang.annotations.Language

open class DeferredIronExecutor(private val iron: Iron): IronConnection {
    private val blockingExecutor = BlockingIronExecutor(iron)

    private val scope = CoroutineScope(iron.settings.dispatcher + SupervisorJob())

    fun <T> transaction(block: suspend Transaction.() -> T): Deferred<T> {
        val transactionController = Transaction(iron)

        return scope.async {
            iron.use {
                try {
                    it.autoCommit = false

                    val result = block(transactionController)

                    it.commit()
                    transactionController.commit()

                    result
                } finally {
                    it.autoCommit = true
                }
            }
        }
    }

    fun query(@Language("SQL") query: String) = scope.async {
        blockingExecutor.query(query)
    }


    fun prepare(@Language("SQL") statement: String, vararg values: Any?) = scope.async {
        blockingExecutor.prepare(statement, *values)
    }

    fun prepare(@Language("SQL") statement: String, model: SqlParamsBuilder) = scope.async {
        blockingExecutor.prepare(statement, model)
    }

    fun prepare(@Language("SQL") statement: String)= scope.async {
        blockingExecutor.prepare(statement)
    }

    fun prepare(@Language("SQL") statement: String, values: SqlParams) = scope.async {
        blockingExecutor.prepare(statement, values)
    }

    fun execute(@Language("SQL") statement: String): Deferred<Boolean> = scope.async {
        blockingExecutor.execute(statement)
    }
}