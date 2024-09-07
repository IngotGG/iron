package gg.ingot.iron.executor.impl

import gg.ingot.iron.Iron
import gg.ingot.iron.executor.IronConnection
import gg.ingot.iron.executor.transaction.Transaction
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.sql.params.SqlParams
import gg.ingot.iron.sql.params.SqlParamsBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

class CompletableIronExecutor(private val iron: Iron): IronConnection {
    private val blockingExecutor = BlockingIronExecutor(iron)

    private val scope = CoroutineScope(iron.settings.dispatcher + SupervisorJob())

    private fun <T> complete(block: suspend () -> T): CompletableFuture<T> {
        val future = CompletableFuture<T>()

        scope.launch {
            try {
                future.complete(block())
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }

    fun <T> transaction(block: Transaction.() -> T): CompletableFuture<T> {
        return complete {
            blockingExecutor.transaction<T>(block)
        }
    }

    fun transaction(block: Consumer<Transaction>): CompletableFuture<Unit> {
        return transaction<Unit> {
            block.accept(this)
        }
    }

    fun query(query: String): CompletableFuture<IronResultSet> {
        return complete {
            blockingExecutor.query(query)
        }
    }

    fun prepare(statement: String, vararg values: Any?): CompletableFuture<IronResultSet> {
        return complete {
            blockingExecutor.prepare(statement, *values)
        }
    }

    fun prepare(statement: String, model: SqlParamsBuilder): CompletableFuture<IronResultSet> {
        return complete {
            blockingExecutor.prepare(statement, model)
        }
    }

    fun prepare(@Language("SQL") statement: String, values: SqlParams): CompletableFuture<IronResultSet> {
        return complete {
            blockingExecutor.prepare(statement, values)
        }
    }

    fun execute(statement: String): CompletableFuture<Boolean> {
        return complete {
            blockingExecutor.execute(statement)
        }
    }
}