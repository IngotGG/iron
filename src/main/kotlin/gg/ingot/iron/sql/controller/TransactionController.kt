package gg.ingot.iron.sql.controller

sealed interface TransactionController : Controller {
    /**
     * Executes a transaction on the database.
     * @param block The block to execute within the transaction.
     * @return The result of the transaction.
     * @since 1.3
     */
    fun <T> transaction(block: TransactionActionableController.() -> T): T
}