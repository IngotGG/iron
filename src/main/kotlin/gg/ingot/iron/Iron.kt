package gg.ingot.iron

import gg.ingot.iron.executor.impl.BlockingIronExecutor
import gg.ingot.iron.executor.impl.CompletableIronExecutor
import gg.ingot.iron.executor.impl.CoroutineIronExecutor
import gg.ingot.iron.executor.impl.DeferredIronExecutor
import gg.ingot.iron.executor.transaction.Transaction
import gg.ingot.iron.model.ModelReader
import gg.ingot.iron.pool.ConnectionPool
import gg.ingot.iron.pool.MultiConnectionPool
import gg.ingot.iron.pool.SingleConnectionPool
import gg.ingot.iron.representation.DBMS
import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.sql.params.SqlParams
import gg.ingot.iron.sql.params.SqlParamsBuilder
import gg.ingot.iron.transformer.ModelTransformer
import gg.ingot.iron.transformer.PlaceholderTransformer
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
    val modelReader = ModelReader(this)

    /** The connection pool used to manage connections to the database. */
    internal var pool: ConnectionPool? = null

    /** The placeholder transformer used to transform values from the result set into their corresponding types. */
    internal val placeholderTransformer = PlaceholderTransformer(this)

    /** The value transformer used to transform values from the result set into their corresponding types. */
    internal val valueTransformer = ValueTransformer(this)

    /** The result transformer used to transform the result set into a model. */
    internal val modelTransformer = ModelTransformer(this)

    /** The default executor to use if one isn't specified */
    private val executor = CoroutineIronExecutor(this)

    /** Whether Iron is currently closed. */
    val isClosed: Boolean
        get() = pool == null

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
            MultiConnectionPool(connectionString, this)
        } else {
            logger.trace("Using single connection pool.")
            SingleConnectionPool(connectionString, this)
        }

        return this
    }

    /**
     * Returns a blocking executor for the Iron instance. Any operations performed on this executor will block the
     * current thread until the operation is complete.
     */
    fun blocking() = BlockingIronExecutor(this)

    /**
     * Returns a coroutine executor for the Iron instance. Any operations performed on this executor will be suspended
     * until the operation is complete.
     */
    fun coroutines() = CoroutineIronExecutor(this)

    /**
     * Returns a deferred executor for the Iron instance. Any operations performed on this executor will
     * be executed in the dispatcher provided to [IronSettings] and returned as a deferred to be awaited
     * at a later time.
     */
    fun deferred() = DeferredIronExecutor(this)

    /**
     * Returns a completable executor for the Iron instance. Any operations performed on this executor will be completed
     * using completable futures.
     */
    fun completable() = CompletableIronExecutor(this)

    /**
     * Use the connection to perform operations on the database.
     * @param block The closure to execute with the connection.
     * @since 1.0
     */
    suspend fun <T : Any?> use(block: suspend (Connection) -> T): T {
        val connection = pool?.connection()
            ?: error("Connection is not open, call connect() before using the connection.")

        val response = block(connection)
        pool?.release(connection)

        return response
    }

    /**
     * Use the connection to perform operations on the database synchronously.
     * @param block The closure to execute with the connection.
     * @since 1.0
     */
    fun <T : Any?> useBlocking(block: (Connection) -> T): T {
        val connection = pool?.connection()
            ?: error("Connection is not open, call connect() before using the connection.")

        val response = block(connection)
        pool?.release(connection)

        return response
    }

    /**
     * Closes the connection to the database.
     * @since 1.0
     */
    @JvmOverloads
    fun close(force: Boolean = false) {
        pool?.close(force)
        pool = null
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
