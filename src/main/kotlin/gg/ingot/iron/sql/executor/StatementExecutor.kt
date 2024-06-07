package gg.ingot.iron.sql.executor

import gg.ingot.iron.sql.MappedResultSet
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * Executes statements on the database.
 * @author DebitCardz
 * @since 1.3
 */
interface StatementExecutor {
    /**
     * Starts a transaction on the connection.
     * @since 1.0
     */
    fun <T : Any?> transaction(block: StatementExecutor.() -> T): T

    /**
     * Executes a raw statement on the database.
     *
     * **Note:** This method does no validation on the statement, it is up to the user to ensure the statement is safe.
     * @param statement The statement to execute on the database.
     * @return If the first result is a ResultSet object; false if it is an update count or there are no results
     * @since 1.0
     */
    fun execute(@Language("SQL") statement: String): Boolean

    /**
     * Executes a raw query on the database and returns the result set.
     *
     * **Note:** This method does no validation on the query, it is up to the user to ensure the query is safe.
     * @param query The query to execute on the database.
     * @return The result set from the query.
     * @since 1.0
     */
    fun query(@Language("SQL") query: String): ResultSet

    /**
     * Executes a raw query on the database and maps the result set to a model.
     *
     * **Note:** This method does no validation on the query, it is up to the user to ensure the query is safe.
     * @param query The query to execute on the database.
     * @param clazz The class to map the result set to.
     * @return A result set mapped to the model.
     * @since 1.0
     */
    fun <T : Any> queryMapped(@Language("SQL") query: String, clazz: KClass<T>): MappedResultSet<T>


    /**
     * Prepares a statement on the database. This method should be preferred over [execute] for security reasons.
     * @param statement The statement to prepare on the database. This statement should contain `?` placeholders for
     * the values, any values passed in through this parameter is not sanitized.
     * @param values The values to bind to the statement.
     * @return The prepared statement.
     * @since 1.0
     */
    fun prepare(@Language("SQL") statement: String, vararg values: Any): ResultSet?

    /**
     * Prepares a statement on the database and maps the result set to a model. This method should be preferred over
     * [execute] for security reasons.
     * @param statement The statement to prepare on the database. This statement should contain `?` placeholders for
     * the values, any values passed in through this parameter is not sanitized.
     * @param clazz The class to map the result set to.
     * @param values The values to bind to the statement.
     * @return A result set mapped to the model.
     */
    fun <T : Any> prepareMapped(@Language("SQL") statement: String, clazz: KClass<T>, vararg values: Any): MappedResultSet<T>
}

/**
 * Helper method allowing for inline usage of the query method.
 * @see [StatementExecutor.queryMapped]
 * @since 1.0
 */
@JvmName("queryMappedInline")
inline fun <reified T : Any> StatementExecutor.queryMapped(@Language("SQL") query: String): MappedResultSet<T> {
    return queryMapped(query, T::class)
}

/**
 * Helper method allowing for inline usage of the prepare method.
 * @see [StatementExecutor.prepareMapped]
 * @since 1.0
 */
@JvmName("prepareMappedInline")
inline fun <reified T: Any> StatementExecutor.prepareMapped(@Language("SQL") statement: String, vararg values: Any): MappedResultSet<T> {
    return prepareMapped(statement, T::class, *values)
}
