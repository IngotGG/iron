package gg.ingot.iron

import gg.ingot.iron.pool.ConnectionPool
import gg.ingot.iron.pool.MultiConnectionPool
import gg.ingot.iron.pool.SingleConnectionPool
import gg.ingot.iron.representation.DBMS
import gg.ingot.iron.sql.MappedResultSet
import gg.ingot.iron.sql.controller.Controller
import gg.ingot.iron.sql.controller.ControllerImpl
import gg.ingot.iron.transformer.ResultTransformer
import gg.ingot.iron.transformer.ValueTransformer
import kotlinx.coroutines.*
import org.intellij.lang.annotations.Language
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
) {
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

    private suspend fun <T : Any?> withController(block: suspend (Controller) -> T): T {
        val connection = pool?.connection()
            ?: error(UNOPENED_CONNECTION_MESSAGE)

        return block(ControllerImpl(connection, resultTransformer))
            .also { pool?.release(connection) }
    }

    /**
     * Closes the connection to the database.
     * @since 1.0
     */
    fun close() {
        pool?.close()
    }

    suspend fun <T : Any?> transaction(block: Controller.() -> T): T {
        return withContext(dispatcher) { withController { controller ->
            controller.transaction(block)
        } }
    }

    suspend fun query(@Language("SQL") statement: String): ResultSet {
        return withContext(dispatcher) { withController { controller ->
            controller.query(statement)
        } }
    }

    suspend fun <T : Any> queryMapped(@Language("SQL") statement: String, clazz: KClass<T>): MappedResultSet<T> {
        return withContext(dispatcher) { withController { controller ->
            controller.query(statement, clazz)
        } }
    }

    suspend inline fun <reified T : Any> queryMapped(@Language("SQL") statement: String): MappedResultSet<T> = queryMapped(statement, T::class)

    suspend fun execute(statement: String): Boolean {
        return withContext(dispatcher) { withController { controller ->
            controller.execute(statement)
        } }
    }

    suspend fun prepare(@Language("SQL") statement: String, vararg values: Any): ResultSet? {
        return withContext(dispatcher) { withController { controller ->
            controller.prepare(statement, *values)
        } }
    }

    suspend fun <T : Any> prepareMapped(@Language("SQL") statement: String, clazz: KClass<T>, vararg values: Any): MappedResultSet<T> {
        return withContext(dispatcher) { withController { controller ->
            controller.prepare(statement, clazz, *values)
        } }
    }

    suspend inline fun <reified T : Any> prepareMapped(@Language("SQL") statement: String, vararg values: Any) = prepareMapped(statement, T::class, *values)

    internal companion object {
        /** Error message to send when a connection is requested but [Iron.connect] has not been called. */
        private const val UNOPENED_CONNECTION_MESSAGE = "Connection is not open, call connect() before using the connection."
    }
}