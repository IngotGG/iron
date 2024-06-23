package gg.ingot.iron.sql.controller

import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.sql.SqlParameters
import org.intellij.lang.annotations.Language

/**
 * Controller interface for handling database transactions and queries.
 * @author DebitCardz
 * @since 1.3
 */
sealed interface Controller {
    /**
     * Starts a transaction on the connection.
     * @throws Exception If an error occurs during the transaction.
     * @since 1.0
     */
    fun <T : Any?> transaction(block: TransactionController.() -> T): T

    /**
     * Executes a raw query on the database and returns the result set.
     *
     * **Note:** This method does no validation on the query, it is up to the user to ensure the query is safe.
     * @param query The query to execute on the database.
     * @return The result set from the query.
     * @since 1.0
     */
    fun query(@Language("SQL") query: String): IronResultSet

    /**
     * Prepares a statement on the database. This method should be preferred over [execute] for security reasons.
     * @param statement The statement to prepare on the database. This statement should contain `?` placeholders for
     * the values, any values passed in through this parameter is not sanitized.
     * @param values The values to bind to the statement.
     * @return The prepared statement.
     * @since 1.0
     */
    fun prepare(@Language("SQL") statement: String, vararg values: Any?): IronResultSet

    /**
     * Prepares a statement on the database. This method should be preferred over [execute] for security reasons.
     * @param statement The statement to prepare on the database. This statement should contain named placeholders for
     * the values, any values passed in through this parameter is not sanitized.
     * @param values The values to bind to the statement.
     * @return The prepared statement.
     * @since 1.3
     */
    fun prepare(@Language("SQL") statement: String, values: SqlParameters): IronResultSet {
        val insertedValues = mutableListOf<Any>()

        val parsedStatement = SQL_PLACEHOLDER_REGEX.replace(statement) { matchResult ->
            val group = matchResult.groupValues.first()

            // cast
            if(group.startsWith("::")) {
                group
            // wrapped in text or something
            } else if(SURROUNDING_QUOTES.any { it == group.first() && it == group.last() }) {
                group
            } else {
                val name = matchResult.groupValues[1]
                insertedValues.add(
                    values[name] ?: error("no value found for placeholder $name")
                )
                "?"
            }
        }

        return prepare(parsedStatement, *insertedValues.toTypedArray())
    }

    /**
     * Executes a raw statement on the database.
     *
     * **Note:** This method does no validation on the statement, it is up to the user to ensure the statement is safe.
     * @param statement The statement to execute on the database.
     * @return If the first result is a ResultSet object; false if it is an update count or there are no results
     * @since 1.0
     */
    fun execute(@Language("SQL") statement: String): Boolean

    private companion object {
        /** The regex for SQL placeholders. */
        val SQL_PLACEHOLDER_REGEX = "'(?:\\\\'|[^'])*'|\"(?:\\\\\"|[^\"])*\"|`(?:\\\\`|[^`])*`|::?(\\w+)".toRegex()

        /** Quote characters that may wrap placeholders. */
        val SURROUNDING_QUOTES = arrayOf('`', '\'', '"')
    }
}