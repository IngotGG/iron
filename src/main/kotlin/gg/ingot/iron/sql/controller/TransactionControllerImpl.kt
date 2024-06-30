package gg.ingot.iron.sql.controller

import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.sql.params.SqlParams

internal class TransactionControllerImpl(
    private val controller: Controller
) : TransactionActionableController {
    /** The action to execute after the transaction is committed. */
    private var afterCommit: TransactionAction? = null

    /** The action to execute after the transaction is rolled back. */
    private var afterRollback: TransactionAction? = null

    override fun afterCommit(block: TransactionAction) {
        check(afterCommit == null) { "Cannot set afterCommit twice" }
        afterCommit = block
    }

    override fun afterRollback(block: TransactionAction) {
        check(afterRollback == null) { "Cannot set afterRollback twice" }
        afterRollback = block
    }

    /**
     * Commits the transaction.
     * @since 1.3
     */
    fun commit() = afterCommit?.invoke()

    /**
     * Rolls back the transaction.
     * @since 1.3
     */
    fun rollback() = afterRollback?.invoke()

    override fun query(query: String): IronResultSet = controller.query(query)

    override fun prepare(statement: String, vararg values: Any?): IronResultSet = controller.prepare(statement, *values)
    override fun prepare(statement: String, model: ExplodingModel): IronResultSet = controller.prepare(statement, model)

    override fun prepare(statement: String, model: SqlParams): IronResultSet = controller.prepare(statement, model)

    override fun execute(statement: String): Boolean = controller.execute(statement)
}