package gg.ingot.iron.pool

import gg.ingot.iron.Iron
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * A connection pool that manages multiple connections to a database.
 *
 * This pool will attempt to keep a minimum amount of connections open at all times, and will create new connections
 * as needed up to a maximum amount of connections, any connections past the minimum will be available to use
 * up until it hits its TTL or is closed.
 *
 * @author DebitCardz
 * @since 1.2
 */
class MultiConnectionPool(
    private val connectionString: String,
    private val iron: Iron,
) : ConnectionPool {
    private val logger = LoggerFactory.getLogger(MultiConnectionPool::class.java)

    private val coroutineScope = CoroutineScope(iron.settings.dispatcher + SupervisorJob())

    /** The pool of persistent connections. */
    private val pool = ArrayBlockingQueue<Connection>(iron.settings.minimumActiveConnections)

    /** The amount of open connections */
    private val openConnections = AtomicInteger(0)

    init {
        logger.trace(
            "Creating multi connection pool with minimum connections of {} and maximum connections of {}.",
            iron.settings.minimumActiveConnections,
            iron.settings.maximumConnections
        )

        // Create persistent connections
        repeat(iron.settings.minimumActiveConnections) {
            pool.add(createConnection())
        }
    }

    override fun connection(): Connection {
        var connection = pull() ?: error("No connection available in the pool, try increasing the maximum connections.")

        if(connection.isClosed) {
            logger.error("Acquired a closed connection, recreating connection.")
            connection = createConnection()
        }

        return connection
    }

    override fun release(connection: Connection) {
        // ensure releasing doesn't block the consumer
        coroutineScope.launch {
            runCatching {
                push(connection)
            }.onFailure {
                logger.error("Failed to release connection back to the pool, {}", it.message)

                connection.close()
                openConnections.decrementAndGet()
            }
        }
    }

    override fun close(force: Boolean) {
        logger.trace("Closing connection pool, force: {}", force)
        if (force) {
            coroutineScope.cancel()
            pool.forEach(Connection::close)
        }
    }

    private fun push(connection: Connection) {
        if (iron.isClosed) {
            logger.debug("Iron is closed, cannot push connection back to the pool.")
            connection.close()
            return
        }

        val offered = pool.offer(connection, iron.settings.connectionPollTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        if (!offered) {
            logger.trace("Connection pool is full, closing connection.")
            connection.close()
        }

        logger.trace("Connection task finished, open connections: {}", openConnections.get())
        openConnections.decrementAndGet()
    }

    private fun pull(): Connection? {
        if (iron.isClosed) {
            logger.debug("Iron is closed, cannot pull new connection.")
            return null
        }

        if (openConnections.get() >= iron.settings.maximumConnections) {
            logger.trace("Maximum connections reached, waiting for a connection to be released.")
            return pool.poll(iron.settings.connectionPollTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        }

        val connection = pool.poll() ?: createConnection()
        openConnections.incrementAndGet()

        logger.trace("Acquired a connection from the pool, open connections: {}", openConnections.get())
        return connection
    }

    private fun createConnection(): Connection {
        logger.trace("Creating a new connection for the pool.")
        return if(iron.settings.driverProperties != null) {
            DriverManager.getConnection(connectionString, iron.settings.driverProperties)
        } else {
            DriverManager.getConnection(connectionString)
        }
    }
}