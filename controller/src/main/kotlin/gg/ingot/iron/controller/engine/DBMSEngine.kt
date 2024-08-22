package gg.ingot.iron.controller.engine

import gg.ingot.iron.Iron
import gg.ingot.iron.controller.engine.impl.MySQLEngine
import gg.ingot.iron.controller.engine.impl.PostgresEngine
import gg.ingot.iron.controller.engine.impl.SqliteEngine
import gg.ingot.iron.controller.exceptions.NoEngineException
import gg.ingot.iron.controller.query.SQL
import gg.ingot.iron.controller.query.SqlPredicate
import gg.ingot.iron.controller.tables.TableController
import gg.ingot.iron.representation.DBMS
import org.slf4j.LoggerFactory

/**
 * Defines the SQL to execute when running controller operations, this varies from dbms to dbms.
 * Queries support placeholders and are required most the times, each function will explain in
 * detail what placeholders are available.
 *
 * Custom engines can be registered with [DBMSEngine.register], however if built-in support is already
 * provided, it's recommended to use those.
 */
abstract class DBMSEngine<T: Any>(
    internal val iron: Iron,
    internal val controller: TableController<T>
) {
    @Suppress("unused")
    companion object {
        private val logger = LoggerFactory.getLogger(DBMSEngine::class.java)
        private val engines: MutableMap<DBMS, Class<*>> = mutableMapOf(
            DBMS.SQLITE to SqliteEngine::class.java,
            DBMS.MYSQL to MySQLEngine::class.java,
            DBMS.POSTGRESQL to PostgresEngine::class.java,
        )

        fun register(dbms: DBMS, engine: Class<DBMSEngine<*>>) {
            if (engines.containsKey(dbms)) {
                logger.warn("A engine for ${dbms.value} is already defined, it will be overwritten")
            }

            this.engines[dbms] = engine
        }

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T: Any> getEngine(iron: Iron, controller: TableController<T>): DBMSEngine<T> {
            if (iron.settings.driver == null) {
                throw IllegalStateException("Iron has no driver specified, try specifying one manually.")
            }

            return engines[iron.settings.driver]
                ?.getDeclaredConstructor(Iron::class.java, TableController::class.java)
                ?.newInstance(iron, controller) as? DBMSEngine<T>
                ?: throw NoEngineException(iron.settings.driver!!)
        }
    }

    /**
     * Retrieves all entities from the table
     * @return The list of all entities in the table
     */
    abstract suspend fun all(filter: (SQL<T>.() -> SqlPredicate)?): List<T>

    /**
     * Insert a new entity into the table
     * @param entity The entity to insert
     * @param pull Whether to pull the entity after inserting
     * @return The entity that was inserted, if `pull` is true then this will be pulled from the database.
     * Otherwise, it will be the entity that was inserted.
     */
    abstract suspend fun insert(entity: T, pull: Boolean): T

    /**
     * Get the amount of rows in the table
     * @return The amount of rows in the table
     */
    abstract suspend fun count(): Int

    /**
     * Drop the table
     */
    abstract suspend fun drop()

    /**
     * Get a single entity from the table
     */
    abstract suspend fun first(filter: (SQL<T>.() -> SqlPredicate)?): T?

    /**
     * Delete all entities from the table
     */
    abstract suspend fun clear()

    /**
     * Delete multiple entities from the table that match the filter
     */
    abstract suspend fun delete(filter: (SQL<T>.() -> SqlPredicate))

    /**
     * Delete a single entity from the table
     */
    abstract suspend fun delete(entity: T)

    /**
     * Update a single entity in the table
     * @param entity The entity to update
     */
    abstract suspend fun update(entity: T)

}