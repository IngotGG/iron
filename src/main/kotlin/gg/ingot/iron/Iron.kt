package gg.ingot.iron

import gg.ingot.iron.executor.impl.BlockingIronExecutor
import gg.ingot.iron.executor.impl.CompletableIronExecutor
import gg.ingot.iron.executor.impl.CoroutineIronExecutor
import gg.ingot.iron.executor.transaction.Transaction
import gg.ingot.iron.pool.ConnectionPool
import gg.ingot.iron.pool.MultiConnectionPool
import gg.ingot.iron.pool.SingleConnectionPool
import gg.ingot.iron.representation.DBMS
import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.sql.params.SqlParams
import gg.ingot.iron.sql.params.SqlParamsBuilder
import gg.ingot.iron.transformer.ModelTransformer
import gg.ingot.iron.transformer.ResultTransformer
import gg.ingot.iron.transformer.ValueTransformer
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.sql.Connection

/**
 * The entry point for the Iron database library, which allows for easy database connections and queries.
 * @param connectionString The connection string to the database, which should be in the format of `jdbc:<dbms>:<connection>`.
 * @since 1.0
 * @author santio
 */
@Suppress("MemberVisibilityCanBePrivate")
class Iron internal constructor(
    private val connectionString: String,
    val settings: IronSettings
) {
    /** The inflector used to transform names into their corresponding requested form. */
    val inflector = Inflector(this)

    /** The model transformer used to transform models into their corresponding entity representation. */
    val modelTransformer = ModelTransformer(settings, inflector)

    /** The connection pool used to manage connections to the database. */
    internal var pool: ConnectionPool? = null

    /** The value transformer used to transform values from the result set into their corresponding types. */
    internal val valueTransformer = ValueTransformer(settings.serialization)

    /** The result transformer used to transform the result set into a model. */
    internal val resultTransformer = ResultTransformer(modelTransformer, valueTransformer)

    /** The default executor to use if one isn't specified */
    private val executor = CoroutineIronExecutor(this)

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
        val dbmsValue = connectionString.removePrefix("jdbc:").substringBefore(":")

        val dbms = settings.driver
            ?: DBMS.fromValue(dbmsValue)
        logger.trace("Using DBMS {} for value {}.", dbms?.name ?: "<user supplied>", dbmsValue)

        if (dbms == null) {
            logger.warn("No DBMS found for value $dbmsValue, make sure you load the driver manually before calling connect().")
        } else settings.driver = dbms

        dbms?.load()

        pool = if(settings.isMultiConnectionPool) {
            logger.trace("Using multi connection pool.")
            MultiConnectionPool(connectionString, settings)
        } else {
            logger.trace("Using single connection pool.")
            SingleConnectionPool(connectionString, settings)
        }

        return this
    }

    fun blocking() = BlockingIronExecutor(this)
    fun coroutines() = CoroutineIronExecutor(this)
    fun completable() = CompletableIronExecutor(this)

    /**
     * Use the connection to perform operations on the database.
     * @param block The closure to execute with the connection.
     * @since 1.0
     */
    suspend fun <T : Any?> use(block: suspend (Connection) -> T): T {
        val connection = pool?.connection()
            ?: error("Connection is not open, call connect() before using the connection.")

        return block(connection)
            .also { pool?.release(connection) }
    }

    /**
     * Use the connection to perform operations on the database synchronously.
     * @param block The closure to execute with the connection.
     * @since 1.0
     */
    fun <T : Any?> useBlocking(block: (Connection) -> T): T {
        val connection = pool?.connection()
            ?: error("Connection is not open, call connect() before using the connection.")

        return block(connection)
            .also { pool?.release(connection) }
    }

    /**
     * Closes the connection to the database.
     * @since 1.0
     */
    fun close() {
        pool?.close()
    }

    /**
     * Starts a transaction on the connection.
     * @since 1.0
     */
    @JvmName("transactionSuspend")
    suspend fun <T : Any?> transaction(block: suspend Transaction.() -> T): T {
        return executor.transaction(block)
    }

    /**
     * Executes a raw query on the database and returns the result set.
     *
     * **Note:** This method does no validation on the query, it is up to the user to ensure the query is safe.
     * @param statement The query to execute on the database.
     * @return The result set from the query.
     * @since 1.0
     */
    suspend fun query(@Language("SQL") statement: String): IronResultSet {
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
    suspend fun execute(@Language("SQL") statement: String): Boolean {
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
    suspend fun prepare(@Language("SQL") statement: String, vararg values: Any?): IronResultSet {
        return executor.prepare(statement, *values)
    }

    /**
     * Prepares a statement on the database. This method should be preferred over [execute] for security reasons. This
     * will take an [ExplodingModel] and extract the values from it and put them in the query for you.
     * @param statement The statement to prepare on the database. This statement should contain `?` placeholders for
     * the values, any values passed in through this parameter is not sanitized.
     * @param model The model to get the data from
     * @return The prepared statement.
     * @since 1.0
     */
    @JvmName("prepareExplodingModel")
    suspend fun prepare(@Language("SQL") statement: String, model: ExplodingModel): IronResultSet {
        return executor.prepare(statement, model)
    }

    @JvmName("prepareBuilder")
    suspend fun prepare(@Language("SQL") statement: String, params: SqlParamsBuilder): IronResultSet {
        return executor.prepare(statement, params)
    }

    /**
     * Prepares a statement on the database. This method should be preferred over [execute] for security reasons.
     * @param statement The statement to prepare on the database. This statement should contain `?` placeholders for
     * the values, any values passed in through this parameter is not sanitized.
     * @param values The values to bind to the statement.
     * @return The prepared statement.
     */
    @JvmName("prepareSqlParams")
    suspend fun prepare(@Language("SQL") statement: String, values: SqlParams): IronResultSet {
        return executor.prepare(statement, values)
    }

    companion object {
        /** Error message to send when a connection is requested but [Iron.connect] has not been called. */
        private const val UNOPENED_CONNECTION_MESSAGE = "Connection is not open, call connect() before using the connection."

        /** The logger for the Iron class. */
        private val logger = LoggerFactory.getLogger(Iron::class.java)

        @JvmStatic
        @JvmName("create")
        fun create(connectionString: String, settings: IronSettings): Iron {
            return Iron(connectionString, settings)
        }
    }
}

@JvmOverloads
@JvmName("create")
fun Iron(connectionString: String, block: IronSettings.() -> Unit = {}): Iron {
    return Iron(connectionString, IronSettings().apply(block))
}