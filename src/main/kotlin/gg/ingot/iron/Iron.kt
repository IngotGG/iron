package gg.ingot.iron

import gg.ingot.iron.representation.DBMS
import gg.ingot.iron.sql.MappedResultSet
import kotlinx.coroutines.*
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.reflect.KClass

/**
 * The entry point for the Iron database library, which allows for easy database connections and queries.
 * @param connectionString The connection string to the database, which should be in the format of `jdbc:<dbms>:<connection>`.
 * @since 1.0
 */
@Suppress("MemberVisibilityCanBePrivate")
class Iron(
    private val connectionString: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val logger = LoggerFactory.getLogger(Iron::class.java)
    private var connection: Connection? = null

    /**
     * Establishes a connection to the database using the provided connection string.
     *
     * If the DBMS is not recognized, the driver will not be loaded and the user will need to load it manually before
     * calling this method.
     *
     * @return The Iron instance for chaining.
     * @since 1.0
     * @see DBMS
     */
    fun connect(): Iron {
        val dbmsValue = connectionString.removePrefix("jdbc:").substringBefore(":").lowercase()
        val dbms = DBMS.fromValue(dbmsValue)
        logger.trace("Using DBMS {} for value {}.", dbms?.name ?: "<user supplied>", dbmsValue)

        if (dbms == null) {
            logger.warn("No DBMS found for value $dbmsValue, make sure you load the driver " +
                "manually before calling connect().")
        }

        dbms?.load()
        connection = DriverManager.getConnection(connectionString)

        return this
    }

    /**
     * Use the connection to perform operations on the database.
     * @param block The closure to execute with the connection.
     * @since 1.0
     */
    suspend fun <T: Any?> use(block: suspend (Connection) -> T): T {
        val connection = connection
            ?: error("Connection is not open, call connect() before using the connection.")

        return block(connection)
    }

    /**
     * Starts a transaction on the connection.
     * @since 1.0
     */
    suspend fun <T: Any?> transaction(block: suspend Iron.() -> T): T {
        val connection = connection
            ?: error("Connection is not open, call connect() before using the connection.")

        try {
            connection.autoCommit = false
            val result = block()
            connection.commit()

            return result
        } catch (e: Exception) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
        }
    }

    /**
     * Executes a raw query on the database and returns the result set.
     *
     * **Note:** This method does no validation on the query, it is up to the user to ensure the query is safe.
     * @param query The query to execute on the database.
     * @return The result set from the query.
     * @since 1.0
     */
    suspend fun query(@Language("SQL") query: String): ResultSet {
        val connection = connection
            ?: error("Connection is not open, call connect() before using the connection.")

        return withContext(dispatcher) {
            connection.createStatement().executeQuery(query)
        }
    }

    /**
     * Executes a raw query on the database and maps the result set to a model.
     *
     * **Note:** This method does no validation on the query, it is up to the user to ensure the query is safe.
     * @param query The query to execute on the database.
     * @param clazz The class to map the result set to.
     * @return A result set mapped to the model.
     * @since 1.0
     */
    suspend fun <T: Any> query(@Language("SQL") query: String, clazz: KClass<T>): MappedResultSet<T> {
        val resultSet = query(query)
        return MappedResultSet(resultSet, clazz)
    }

    /**
     * Helper method allowing for inline usage of the query method.
     * @see query
     * @since 1.0
     */
    @JvmName("queryInline")
    suspend inline fun <reified T: Any> query(@Language("SQL") query: String): MappedResultSet<T> {
        return query(query, T::class)
    }

    /**
     * Executes a raw statement on the database.
     *
     * **Note:** This method does no validation on the statement, it is up to the user to ensure the statement is safe.
     * @param statement The statement to execute on the database.
     * @return If the first result is a ResultSet object; false if it is an update count or there are no results
     * @since 1.0
     */
    suspend fun execute(@Language("SQL") statement: String): Boolean {
        val connection = connection
            ?: error("Connection is not open, call connect() before using the connection.")

        return withContext(dispatcher) {
            connection.createStatement().execute(statement)
        }
    }

    /**
     * Prepares a statement on the database. This method should be preferred over [execute] for security reasons.
     * @param statement The statement to prepare on the database. This statement should contain `?` placeholders for
     * the values, any values passed in through this parameter is not sanitized.
     * @param values The values to bind to the statement.
     * @return The prepared statement.
     * @since 1.0
     */
    suspend fun prepare(@Language("SQL") statement: String, vararg values: Any): ResultSet? {
        val connection = connection
            ?: error("Connection is not open, call connect() before using the connection.")

        return withContext(dispatcher) {
            val preparedStatement = connection.prepareStatement(statement)

            for ((index, value) in values.withIndex()) {
                preparedStatement.setObject(index + 1, value)
            }

            if (preparedStatement.execute()) {
                preparedStatement.resultSet
            } else {
                null
            }
        }
    }
    /**
     * Prepares a statement on the database and maps the result set to a model. This method should be preferred over
     * [execute] for security reasons.
     * @param statement The statement to prepare on the database. This statement should contain `?` placeholders for
     * the values, any values passed in through this parameter is not sanitized.
     * @param clazz The class to map the result set to.
     * @param values The values to bind to the statement.
     * @return A result set mapped to the model.
     */
    suspend fun <T: Any> prepare(@Language("SQL") statement: String, clazz: KClass<T>, vararg values: Any): MappedResultSet<T> {
        val resultSet = prepare(statement, *values)
            ?: error("No result set was returned from the prepared statement.")

        return MappedResultSet(resultSet, clazz)
    }

    /**
     * Helper method allowing for inline usage of the prepare method.
     * @see prepare
     * @since 1.0
     */
    @JvmName("prepareInline")
    suspend inline fun <reified T: Any> prepare(@Language("SQL") statement: String, vararg values: Any): MappedResultSet<T> {
        return prepare(statement, T::class, *values)
    }

    /**
     * Closes the connection to the database.
     * @since 1.0
     */
    fun close() {
        connection?.close()
    }

}