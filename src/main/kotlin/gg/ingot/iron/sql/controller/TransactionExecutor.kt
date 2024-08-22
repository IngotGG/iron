package gg.ingot.iron.sql.controller

/**
 * A controller that can execute transactions on the database.
 * @since 1.4
 * @author DebitCardz
 */
sealed interface TransactionExecutor : Executor {
    /**
     * Executes a transaction on the database.
     * @param block The block to execute within the transaction.
     * @return The result of the transaction.
     * @since 1.3
     */
    fun <T> transaction(block: TransactionActionableController.() -> T): T
}