package gg.ingot.iron

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import gg.ingot.iron.bindings.SqlBindings
import gg.ingot.iron.executor.impl.BlockingIronExecutor
import gg.ingot.iron.executor.impl.CompletableIronExecutor
import gg.ingot.iron.executor.impl.CoroutineIronExecutor
import gg.ingot.iron.executor.impl.DeferredIronExecutor
import gg.ingot.iron.executor.transaction.Transaction
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.transformer.PlaceholderTransformer
import gg.ingot.iron.transformer.ResultMapper
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.sql.Connection

/**
 * The entry point for the Iron database library.
 *
 * Iron by default is designed to simply be a wrapper around HikariCP and provide an easy API for mapping
 * ResultSets to models and executing queries. Iron also provides some utilities for binding models to queries
 * but at the end of the day, Iron is purely a mapper.
 *
 * If you would like to work with a more type-safe API, you can see the Controller module for a more
 * ORM-like experience, however with more restrictions on who can use the API.
 *
 * To get started, you need to first connect to the database, you also need to shade the JDBC driver for your
 * database type into your project. (You can shade multiple drivers if you want to support multiple databases)
 *
 * Once you have shaded the driver, you can connect to the database using the `connect()` method. Depending
 * on the DBMS you are using, Iron will prefer pooling connections rather than using a single connection, for
 * some DBMS' this isn't possible and Iron will default to a single connection. This can be changed by modifying
 * the settings passed into iron.
 *
 * ```kotlin
 * val iron = Iron.create("jdbc:sqlite:data.db")
 *     .connect()
 * ```
 *
 * To change settings, you can use the `settings` property.
 *
 * ```kotlin
 * val iron = Iron.create("jdbc:postgresql://localhost:5432/mydb") {
 *     maxConnections = 10 # Pools the connections
 *     serialization = SerializationAdapter.Gson(Gson()) # Allows Iron to support JSON
 *     username = "root"
 *     password = "password"
 * }.connect()
 * ```
 *
 * @param connectionString The connection string to the database, which should be in the format of `jdbc:<dbms>:<connection>`.
 * @param settings The settings to use for the connection pool.
 * @since 1.0
 * @author santio
 */
@Suppress("MemberVisibilityCanBePrivate")
class Iron internal constructor(
    private val connectionString: String,
    val settings: IronSettings
): AutoCloseable {
    /** The connection pool used to manage connections to the database. We wrap over Hikari */
    internal var pool: HikariDataSource? = null

    /** The placeholder transformer used to transform values from the result set into their corresponding types. */
    internal val placeholderTransformer = PlaceholderTransformer(this)

    /** The value transformer used to transform values from the result set into their corresponding types. */
    internal val resultMapper = ResultMapper(this)

    /** The default executor to use if one isn't specified */
    private val executor = CoroutineIronExecutor(this)

    /** Listeners to be notified of when Iron is closed to allow for additional cleanup. */
    val onCloseListeners = mutableListOf<Runnable>()

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

        // Load the driver
        dbms?.load()

        pool = HikariDataSource(HikariConfig().apply {
            jdbcUrl = connectionString
            maximumPoolSize = settings.maxConnections
            minimumIdle = settings.minConnections
            connectionTimeout = settings.connectionPollTimeout.inWholeMilliseconds
            idleTimeout = settings.connectionTTL.inWholeMilliseconds
            settings.username?.let { username = it }
            settings.password?.let { password = it }
            settings.properties?.let { dataSourceProperties = it }
        })

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
     *
     * Notice: This method does not close the connection, you must close the connection yourself, or use
     * either single() or all() (or their variants) in [IronResultSet] to close the connection for you.
     *
     * @param block The closure to execute with the connection.
     * @since 1.0
     */
    suspend fun <T : Any?> use(block: suspend (Connection) -> T): T {
        val connection = pool?.connection
            ?: error("Connection is not open, call connect() before using the connection.")

        return block(connection)
    }

    /**
     * Use the connection to perform operations on the database synchronously.
     * @param block The closure to execute with the connection.
     * @since 1.0
     */
    fun <T : Any?> useBlocking(block: (Connection) -> T): T = runBlocking {
        this@Iron.use(block)
    }

    /**
     * Pass a listener to be notified when the connection is closed.
     * @param listener The listener to be notified.
     * @return The iron instance for chaining.
     * @since 2.0
     */
    fun onClose(listener: Runnable): Iron {
        onCloseListeners.add(listener)
        return this
    }

    /**
     * Closes the connection to the database.
     * @since 1.0
     */
    override fun close() {
        pool?.close()
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
     * Prepares a statement on the database. This method should be preferred over [execute] for security reasons.
     * @param statement The statement to prepare on the database. This statement should contain `?` placeholders for
     * the values, any values passed in through this parameter is not sanitized.
     * @param variable The variable to bind to the statement.
     * @param variables The variables to bind to the statement.
     * @return The prepared statement.
     */
    @JvmName("prepareBindings")
    suspend fun prepare(@Language("SQL") statement: String, variable: SqlBindings, vararg variables: SqlBindings): IronResultSet {
        return executor.prepare(statement, variable, *variables)
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

/**
 * The entry point for the Iron database library.
 *
 * Iron by default is designed to simply be a wrapper around HikariCP and provide an easy API for mapping
 * ResultSets to models and executing queries. Iron also provides some utilities for binding models to queries
 * but at the end of the day, Iron is purely a mapper.
 *
 * If you would like to work with a more type-safe API, you can see the Controller module for a more
 * ORM-like experience, however with more restrictions on who can use the API.
 *
 * To get started, you need to first connect to the database, you also need to shade the JDBC driver for your
 * database type into your project. (You can shade multiple drivers if you want to support multiple databases)
 *
 * Once you have shaded the driver, you can connect to the database using the `connect()` method. Depending
 * on the DBMS you are using, Iron will prefer pooling connections rather than using a single connection, for
 * some DBMS' this isn't possible and Iron will default to a single connection. This can be changed by modifying
 * the settings passed into iron.
 *
 * ```kotlin
 * val iron = Iron.create("jdbc:sqlite:data.db")
 *     .connect()
 * ```
 *
 * To change settings, you can use the `settings` property.
 *
 * ```kotlin
 * val iron = Iron.create("jdbc:postgresql://localhost:5432/mydb") {
 *     maxConnections = 10 # Pools the connections
 *     serialization = SerializationAdapter.Gson(Gson()) # Allows Iron to support JSON
 *     username = "root"
 *     password = "password"
 * }.connect()
 * ```
 *
 * @param connectionString The connection string to the database, which should be in the format of `jdbc:<dbms>:<connection>`.
 * @param settings The settings to use for the connection pool.
 * @since 1.0
 * @author santio
 */
@JvmOverloads
@JvmName("create")
fun Iron(connectionString: String, settings: IronSettings.() -> Unit = {}): Iron {
    return Iron(connectionString, IronSettings().apply(settings))
}
