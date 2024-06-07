package gg.ingot.iron

import gg.ingot.iron.pool.ConnectionPool
import gg.ingot.iron.pool.MultiConnectionPool
import gg.ingot.iron.pool.SingleConnectionPool
import gg.ingot.iron.representation.DBMS
import gg.ingot.iron.sql.MappedResultSet
import gg.ingot.iron.sql.executor.StatementExecution
import gg.ingot.iron.sql.executor.StatementExecutor
import gg.ingot.iron.sql.executor.SuspendingStatementExecutor
import gg.ingot.iron.transformer.ResultTransformer
import gg.ingot.iron.transformer.ValueTransformer
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.sql.Connection
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
    private val settings: IronSettings = IronSettings(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : SuspendingStatementExecutor {
    private val logger = LoggerFactory.getLogger(Iron::class.java)

    /** The connection pool used to manage connections to the database. */
    private var pool: ConnectionPool? = null

    /** The value transformer used to transform values from the result set into their corresponding types. */
    private val valueTransformer = ValueTransformer(settings.serialization)

    /** The result transformer used to transform the result set into a model. */
    private val resultTransformer = ResultTransformer(valueTransformer)

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
        }
        dbms?.load()

        pool = if(settings.isMultiConnectionPool) {
            logger.trace("Using multi connection pool.")
            MultiConnectionPool(connectionString, settings, dispatcher)
        } else {
            logger.trace("Using single connection pool.")
            SingleConnectionPool(connectionString, settings)
        }

        return this
    }

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

    override suspend fun <T> transaction(block: StatementExecutor.() -> T): T {
        val conn = pool?.connection()
            ?: error("Connection is not open, call connect() before using the connection.")

        return withContext(dispatcher) {
            StatementExecution(conn, resultTransformer)
                .transaction(block)
        }.also { pool?.release(conn) }
    }

    override suspend fun execute(statement: String): Boolean {
        val conn = pool?.connection()
            ?: error("Connection is not open, call connect() before using the connection.")

        return withContext(dispatcher) {
            StatementExecution(conn, resultTransformer)
                .execute(statement)
        }.also { pool?.release(conn) }
    }

    override suspend fun query(query: String): ResultSet {
        val conn = pool?.connection()
            ?: error("Connection is not open, call connect() before using the connection.")

        return withContext(dispatcher) {
            StatementExecution(conn, resultTransformer)
                .query(query)
        }.also { pool?.release(conn) }
    }

    override suspend fun <T : Any> queryMapped(query: String, clazz: KClass<T>): MappedResultSet<T> {
        val conn = pool?.connection()
            ?: error("Connection is not open, call connect() before using the connection.")

        return withContext(dispatcher) {
            StatementExecution(conn, resultTransformer)
                .queryMapped(query, clazz)
        }.also { pool?.release(conn) }
    }

    override suspend fun prepare(statement: String, vararg values: Any): ResultSet? {
        val conn = pool?.connection()
            ?: error("Connection is not open, call connect() before using the connection.")

        return withContext(dispatcher) {
            StatementExecution(conn, resultTransformer)
                .prepare(statement, *values)
        }.also { pool?.release(conn) }
    }

    override fun <T : Any> prepareMapped(statement: String, clazz: KClass<T>, vararg values: Any): MappedResultSet<T> {
        val conn = pool?.connection()
            ?: error("Connection is not open, call connect() before using the connection.")

        return StatementExecution(conn, resultTransformer)
            .prepareMapped(statement, clazz, *values)
            .also { pool?.release(conn) }
    }

    /**
     * Closes the connection to the database.
     * @since 1.0
     */
    fun close() {
        pool?.close()
    }
}