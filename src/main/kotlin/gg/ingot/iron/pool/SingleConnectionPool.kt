package gg.ingot.iron.pool

import gg.ingot.iron.IronSettings
import java.sql.Connection
import java.sql.DriverManager

/**
 * A connection pool that manages a single connection to a database.
 * @author DebitCardz
 * @since 1.2
 */
class SingleConnectionPool(
    connectionString: String,
    settings: IronSettings
) : ConnectionPool {
    private val connection = if(settings.driverProperties != null) {
        DriverManager.getConnection(connectionString, settings.driverProperties)
    } else {
        DriverManager.getConnection(connectionString)
    }

    override fun connection(): Connection {
        return connection
    }

    override fun release(connection: Connection) = Unit

    override fun close() {
        connection.close()
    }
}