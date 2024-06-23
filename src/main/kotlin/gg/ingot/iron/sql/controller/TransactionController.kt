package gg.ingot.iron.sql.controller

import gg.ingot.iron.sql.MappedResultSet
import gg.ingot.iron.sql.SqlParameters
import java.sql.ResultSet
import kotlin.reflect.KClass

sealed interface TransactionController : Controller {
    fun afterCommit(block: TransactionAction)

    fun afterRollback(block: TransactionAction)
}

internal class TransactionControllerImpl(
    private val controller: Controller
) : TransactionController {
    private var afterCommit: TransactionAction? = null

    private var afterRollback: TransactionAction? = null

    override fun afterCommit(block: TransactionAction) {
        check(afterCommit == null) { "Cannot set afterCommit twice" }
        afterCommit = block
    }

    fun commit() = afterCommit?.invoke()

    override fun afterRollback(block: TransactionAction) {
        check(afterRollback == null) { "Cannot set afterRollback twice" }
        afterRollback = block
    }

    fun rollback() = afterRollback?.invoke()

    override fun <T> transaction(block: TransactionController.() -> T): T = error("Embedded transactions are not supported.")

    override fun query(query: String): ResultSet = controller.query(query)

    override fun <T : Any> query(query: String, clazz: KClass<T>): MappedResultSet<T> = controller.query(query, clazz)

    override fun prepare(statement: String, vararg values: Any?): ResultSet? = controller.prepare(statement, *values)

    override fun <T : Any> prepare(statement: String, clazz: KClass<T>, vararg values: Any?): MappedResultSet<T> = controller.prepare(statement, clazz, *values)

    override fun <T : Any> prepare(statement: String, clazz: KClass<T>, values: SqlParameters): MappedResultSet<T> = controller.prepare(statement, clazz, values)

    override fun execute(statement: String): Boolean = controller.execute(statement)
}

private typealias TransactionAction = () -> Unit