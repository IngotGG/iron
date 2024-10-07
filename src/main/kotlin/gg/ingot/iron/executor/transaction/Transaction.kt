package gg.ingot.iron.executor.transaction

import gg.ingot.iron.Iron
import gg.ingot.iron.executor.impl.BlockingIronExecutor
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.sql.binding.SqlBindings
import org.intellij.lang.annotations.Language
import java.sql.Connection

/**
 * A wrapper around a transaction that can execute actions after the transaction is committed or rolled back.
 * @since 2.0
 * @author santio
 */
class Transaction internal constructor(
    iron: Iron,
    connection: Connection? = null
) {
    /** The executor to use for the transaction. */
    private val executor = BlockingIronExecutor(iron, connection)

    /** The action to execute after the transaction is committed. */
    private var afterCommit: TransactionAction? = null

    /** The action to execute after the transaction is rolled back. */
    private var afterRollback: TransactionAction? = null

    fun afterCommit(block: TransactionAction) {
        check(afterCommit == null) { "Cannot set afterCommit twice" }
        afterCommit = block
    }

    fun afterRollback(block: TransactionAction) {
        check(afterRollback == null) { "Cannot set afterRollback twice" }
        afterRollback = block
    }

    /**
     * Commits the transaction.
     * @since 1.3
     */
    fun commit() = afterCommit?.run()

    /**
     * Rolls back the transaction.
     * @since 1.3
     */
    fun rollback() = afterRollback?.run()

    /**
     * Executes a raw query on the database and returns the result set.
     *
     * **Note:** This method does no validation on the query, it is up to the user to ensure the query is safe.
     * @param statement The query to execute on the database.
     * @return The result set from the query.
     * @since 1.0
     */
    fun query(@Language("SQL") statement: String): IronResultSet {
        return executor.query(statement)
    }

    /**
     * Executes a raw statement on the database.
     *
     * **Note:** This method does no validation on the statement, it is up to the user to ensure the statement is safe.
     * @param statement The statement to execute on the database.
     * @return If the first result is a ResultSet object; false if it is an update count or there are no results
     * @since 1.0
     */
    fun execute(@Language("SQL") statement: String): Boolean {
        return executor.execute(statement)
    }

    /**
     * Prepares a statement on the database. This method should be preferred over [execute] for security reasons.
     * @param statement The statement to prepare on the database. This statement should contain `?` placeholders for
     * the values, any values passed in through this parameter is not sanitized.
     * @param values The values to bind to the statement.
     * @return The prepared statement.
     * @since 1.0
     */
    fun prepare(@Language("SQL") statement: String, vararg values: Any?): IronResultSet {
        return executor.prepare(statement, *values)
    }

    /**
     * Prepares a statement on the database. This method should be preferred over [execute] for security reasons.
     * @param statement The statement to prepare on the database. This statement should contain `?` placeholders for
     * the values, any values passed in through this parameter is not sanitized.
     * @param variable The variable to bind to the statement.
     * @param variables The variables to bind to the statement.
     * @return The prepared statement.
     */
    @JvmName("prepareBindings")
    fun prepare(@Language("SQL") statement: String, variable: SqlBindings, vararg variables: SqlBindings): IronResultSet {
        return executor.prepare(statement, variable, *variables)
    }
}

typealias TransactionAction = Runnable