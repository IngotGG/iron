package gg.ingot.iron.pool

import gg.ingot.iron.Iron
import java.sql.Connection
import java.sql.DriverManager

/**
 * A connection pool that manages a single connection to a database.
 * @author DebitCardz
 * @since 1.2
 */
class SingleConnectionPool(
    connectionString: String,
    iron: Iron
) : ConnectionPool {
    private val connection = if(iron.settings.driverProperties != null) {
        DriverManager.getConnection(connectionString, iron.settings.driverProperties)
    } else {
        DriverManager.getConnection(connectionString)
    }

    override fun connection(): Connection {
        return connection
    }

    override fun release(connection: Connection) = Unit

    override fun close(force: Boolean) {
        connection.close()
    }
}