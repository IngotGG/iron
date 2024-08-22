package gg.ingot.iron.controller.tables

import gg.ingot.iron.Inflector
import gg.ingot.iron.Iron
import gg.ingot.iron.controller.Controller
import gg.ingot.iron.controller.engine.DBMSEngine
import gg.ingot.iron.controller.query.SQL
import gg.ingot.iron.controller.query.SqlPredicate
import gg.ingot.iron.representation.EntityModel

@Suppress("MemberVisibilityCanBePrivate")
class TableController<T: Any>(private val iron: Iron, internal val clazz: Class<T>) {

    init {
        if (clazz.isSynthetic) {
            throw IllegalArgumentException("Synthetic classes are not supported for security reasons")
        }
    }

    private val inflector = Inflector(iron)
    private val engine = DBMSEngine.getEngine(iron, this)

    private val annotation: Controller = clazz.getAnnotation(Controller::class.java)
        ?: throw IllegalStateException("Class ${clazz.simpleName} does not have the @Controller annotation")

    /**
     * Make sure the table name is valid, this is the only part of the query that is
     * interpolated, and we don't want to allow SQL injection, even though user content
     * shouldn't ever be coming in here. Better safe than sorry.
     */
    private fun isValid(tableName: String): Boolean {
        return tableName.isNotBlank() && tableName.matches(tableRegex)
    }

    /**
     * The effective table name for this controller
     */
    val tableName: String
        get() {
            return annotation.table.ifEmpty {
                inflector.tableName(clazz.simpleName)
            }.takeIf { isValid(it) }
                ?: throw IllegalStateException("Table name for ${clazz.simpleName} is invalid: ${annotation.table}")
        }

    /**
     * The parsed entity model for the table for looking at the details of the model
     * @return An entity model for the table
     */
    val model: EntityModel by lazy {
        iron.modelTransformer.transform(clazz)
    }

    /**
     * Creates a unique selector for the entity that only selects the primary key
     * @param entity The entity to create a unique selector for
     * @return A unique selector for the entity
     */
    internal fun uniqueSelector(entity: T): SqlPredicate {
        val primaryKey = model.fields.firstOrNull { it.isPrimaryKey }
        if (primaryKey == null) {
            throw IllegalStateException("No primary key found for ${clazz.simpleName}, mark one with @Column(primaryKey = true)")
        }

        return SqlPredicate.where(
            "${primaryKey.columnName} = :${primaryKey.variableName}",
            primaryKey.variableName to primaryKey.value(entity)
        )
    }

    /**
     * Get all rows from the table
     * @param filter The filter to apply to the query
     * @return A list of all entities in the table
     */
    suspend fun all(filter: (SQL<T>.() -> SqlPredicate)? = null): List<T> {
        return engine.all(filter)
    }

    /**
     * Insert an entity into the table
     * @param entity The entity to insert
     * @param fetch Whether to fetch the entity after inserting it
     * @return The entity that was inserted, if `fetch` is true then this will be the
     * complete entity, otherwise it will be exact same entity that was passed in
     */
    suspend fun insert(entity: T, fetch: Boolean = false): T {
        return engine.insert(entity, fetch)
    }

    /**
     * Get the size of the table
     * @return The amount of entities in the table
     */
    suspend fun count(): Int {
        return engine.count()
    }

    /**
     * Drop the table
     * @apiNote This is a destructive operation, it cannot be undone
     */
    suspend fun drop() {
        engine.drop()
    }

    /**
     * Get the first entity from the table that matches the filter
     * @param filter The filter to apply to the query
     */
    suspend fun first(filter: (SQL<T>.() -> SqlPredicate)? = null): T? {
        return engine.first(filter)
    }

    /**
     * Delete all entities from the table
     */
    suspend fun clear() {
        engine.clear()
    }

    /**
     * Delete multiple entities from the table that match the filter
     * @param filter The filter to apply to the query
     */
    suspend fun delete(filter: (SQL<T>.() -> SqlPredicate)) {
        engine.delete(filter)
    }

    /**
     * Delete a single entity from the table
     * @param entity The entity to delete
     */
    suspend fun delete(entity: T) {
        engine.delete(entity)
    }

    /**
     * Update a single entity in the table
     * @param entity The entity to update
     */
    suspend fun update(entity: T) {
        engine.update(entity)
    }

    private companion object {
        private val tableRegex = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
    }
}

inline fun <reified T: Any> Iron.controller(): TableController<T> {
    return TableController(this, T::class.java)
}