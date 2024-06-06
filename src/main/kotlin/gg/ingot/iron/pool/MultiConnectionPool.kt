package gg.ingot.iron.pool

import gg.ingot.iron.IronSettings
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

/**
 * A connection pool that manages multiple connections to a database.
 * @author DebitCardz
 * @since 1.2
 */
class MultiConnectionPool(
    private val connectionString: String,
    private val settings: IronSettings
) : ConnectionPool {
    private val logger = LoggerFactory.getLogger(MultiConnectionPool::class.java)

    /**
     * The pool of connections.
     */
    private val pool = ArrayBlockingQueue<Connection>(settings.minimumActiveConnections)

    /**
     * The amount of open connections in the pool.
     */
    private val openConnections = AtomicInteger(0)

    init {
        logger.trace(
            "Creating multi connection pool with minimum connections of {} and maximum connections of {}.",
            settings.minimumActiveConnections,
            settings.maximumConnections
        )

        // always keep a minimum amount of connections
        repeat(settings.minimumActiveConnections) {
            pool.add(createConnection())
        }
        openConnections.set(settings.minimumActiveConnections)
    }

    override fun connection(): Connection {
        val timeoutMs = settings.connectionPollTimeout
            .inWholeMilliseconds

        // if we're at the complete maximum amt of connections
        // we have to wait for one to be released to us
        if(openConnections.get() >= settings.maximumConnections) {
            val acquiredConnection = pool.poll(timeoutMs, TimeUnit.MILLISECONDS)
            if(acquiredConnection != null) {
                openConnections.incrementAndGet()
            }

            if(acquiredConnection?.isClosed == true) {
                openConnections.decrementAndGet()
                logger.warn("Acquired a closed connection, attempting recovery.")
                return connection()
            }

            return acquiredConnection ?: error("Failed to get a connection from the pool.")
        }

        // if we have an available connection, use it or if we haven't
        // hit our max just create one
        val acquiredConnection = pool.poll() ?: run {
            // incr open conns if we have to establish a new one
            openConnections.incrementAndGet()
            createConnection()
        }

        if(acquiredConnection.isClosed) {
            openConnections.decrementAndGet()
            logger.warn("Acquired a closed connection, attempting recovery.")
            return connection()
        }

        return acquiredConnection ?: error("Failed to get a connection from the pool.")
    }

    override fun release(connection: Connection) {
        // give some buffer room in case we need to handle bursts
        // this'll wait a bit so if connections are taken we have one
        // ready to be put back in the pool
        val success = pool.offer(
            connection,
            10.seconds.inWholeMilliseconds,
            TimeUnit.MILLISECONDS
        )

        if (!success) {
            connection.close()
            openConnections.decrementAndGet()
        }
    }

    override fun close() {
        pool.forEach(Connection::close)
        openConnections.set(0)
    }

    private fun createConnection(): Connection {
        return if(settings.driverProperties != null) {
            DriverManager.getConnection(connectionString, settings.driverProperties)
        } else {
            DriverManager.getConnection(connectionString)
        }
    }
}