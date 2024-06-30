package gg.ingot.iron.sql.controller

interface TransactionActionableController : Controller {
    /**
     * Executes the given block after the transaction is committed.
     * @param block The block to execute.
     */
    fun afterCommit(block: TransactionAction)

    /**
     * Executes the given block after the transaction is rolled back.
     * @param block The block to execute.
     */
    fun afterRollback(block: TransactionAction)
}

/** The action to execute after the transaction is committed. */
internal typealias TransactionAction = () -> Unit