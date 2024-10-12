package gg.ingot.iron.controller.engine

import gg.ingot.iron.Iron
import gg.ingot.iron.controller.controller.TableController
import gg.ingot.iron.controller.engine.impl.MySQLEngine
import gg.ingot.iron.controller.engine.impl.PostgresEngine
import gg.ingot.iron.controller.engine.impl.SqliteEngine
import gg.ingot.iron.controller.exceptions.NoEngineException
import gg.ingot.iron.controller.query.SqlFilter
import gg.ingot.iron.DBMS
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
     * Escapes the column name to prevent issues with reserved keywords
     * @param name The name of the column
     * @return The escaped column name
     */
    abstract fun column(name: String): String

    /**
     * Retrieves all entities from the table
     * @return The list of all entities in the table
     */
    abstract suspend fun all(filter: SqlFilter<T>?): List<T>

    /**
     * Insert a new entity into the table
     * @param entity The entity to insert
     * @param fetch Whether to pull the entity after inserting
     * @return The entity that was inserted, if `fetch` is true then this will be pulled from the database.
     * Otherwise, it will be the entity that was inserted.
     */
    abstract suspend fun insert(entity: T, fetch: Boolean): T

    /**
     * Insert multiple entities into the table
     * @param entities The list of entities to insert
     * @param fetch Whether to fetch the entities after inserting them
     * @return The entities that were inserted, if `fetch` is true then this will reflect the
     * database values, otherwise it will be exact same list that was passed in
     */
    abstract suspend fun insertMany(entities: List<T>, fetch: Boolean = false): List<T>

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
    abstract suspend fun first(filter: SqlFilter<T>?): T?

    /**
     * Delete all entities from the table
     */
    abstract suspend fun clear()

    /**
     * Delete multiple entities from the table that match the filter
     */
    abstract suspend fun delete(filter: SqlFilter<T>)

    /**
     * Delete a single entity from the table
     */
    abstract suspend fun delete(entity: T)

    /**
     * Upsert an entity into the table, if it is already in the table it will be updated, otherwise it will be inserted
     * @param entity The entity to upsert
     * @param fetch Whether to fetch the entity after inserting or updating it
     */
    abstract suspend fun upsert(entity: T, fetch: Boolean = false): T

    /**
     * Update a single entity in the table
     * @param entity The entity to update
     * @param fetch Whether to pull the entity after inserting
     * @return The entity that was inserted, if `fetch` is true then this will be pulled from the database.
     * Otherwise, it will be the entity that was inserted.
     */
    abstract suspend fun update(entity: T, fetch: Boolean): T

}