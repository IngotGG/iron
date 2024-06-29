package gg.ingot.iron.sql.controller

import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.sql.IronResultSet

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

    override fun query(query: String): IronResultSet = controller.query(query)

    override fun prepare(statement: String, vararg values: Any?): IronResultSet = controller.prepare(statement, *values)

    override fun prepare(statement: String, model: ExplodingModel): IronResultSet = controller.prepare(statement, model)

    override fun execute(statement: String): Boolean = controller.execute(statement)
}

private typealias TransactionAction = () -> Unit