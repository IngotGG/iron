package gg.ingot.iron.sql.controller

/**
 * A controller that can execute actions after a transaction is committed or rolled back.
 * @since 1.4
 * @author DebitCardz
 */
interface TransactionActionableController : Executor {
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