package gg.ingot.iron.pool

import java.sql.Connection

/**
 * A connection pool that manages connections to a database.
 * @author DebitCardz
 * @since 1.2
 */
interface ConnectionPool {
    /**
     * Gets a connection from the pool.
     * @return A connection from the pool or a new connection if the pool is empty.
     */
    fun connection(): Connection

    /**
     * Releases a connection back to the pool.
     * @param connection The connection to release.
     */
    fun release(connection: Connection)

    /**
     * Closes the connection pool.
     * @param force If the pool should be closed immediately, or wait for all connections to be released.
     */
    fun close(force: Boolean)

    /**
     * Uses a connection from the pool and releases it after the block is executed.
     * @param block The block to execute with the connection.
     */
    fun use(block: (Connection) -> Unit) {
        val connection = connection()
        try {
            block(connection)
        } finally {
            release(connection)
        }
    }
}