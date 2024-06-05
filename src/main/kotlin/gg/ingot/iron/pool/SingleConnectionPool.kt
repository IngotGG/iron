package gg.ingot.iron.pool

import java.sql.Connection
import java.sql.DriverManager

/**
 * A connection pool that manages a single connection to a database.
 * @author DebitCardz
 * @since 1.2
 */
class SingleConnectionPool(
    connectionString: String
) : ConnectionPool {
    private val connection = DriverManager.getConnection(connectionString)

    override fun connection(): Connection {
        return connection
    }

    override fun release(connection: Connection) = Unit

    override fun close() {
        connection.close()
    }
}