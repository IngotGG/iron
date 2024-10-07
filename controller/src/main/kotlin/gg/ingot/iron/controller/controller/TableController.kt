package gg.ingot.iron.controller.controller

import gg.ingot.iron.Iron
import gg.ingot.iron.controller.Controller
import gg.ingot.iron.controller.engine.DBMSEngine
import gg.ingot.iron.controller.query.SqlFilter
import gg.ingot.iron.controller.query.SqlPredicate
import gg.ingot.iron.representation.EntityModel

@Suppress("MemberVisibilityCanBePrivate")
class TableController<T: Any>(val iron: Iron, internal val clazz: Class<T>) {

    init {
        if (clazz.isSynthetic) {
            throw IllegalArgumentException("Synthetic classes are not supported for security reasons")
        }
    }

    private val engine = DBMSEngine.getEngine(iron, this)
    private val interceptors: MutableList<Interceptor<T>> = mutableListOf()

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
                iron.inflector.tableName(clazz.simpleName)
            }.takeIf { isValid(it) }
                ?: throw IllegalStateException("Table name for ${clazz.simpleName} is invalid: ${annotation.table}")
        }

    /**
     * The parsed entity model for the table for looking at the details of the model
     * @return An entity model for the table
     */
    val model: EntityModel by lazy {
        iron.modelReader.read(clazz)
    }

    /**
     * Creates a unique selector for the entity that only selects the primary key
     * @param entity The entity to create a unique selector for
     * @return A unique selector for the entity
     */
    fun uniqueSelector(entity: T): SqlPredicate {
        val primaryKey = model.fields.firstOrNull { it.field.isPrimaryKey() }
        if (primaryKey == null) {
            throw IllegalStateException("No primary key found for ${clazz.simpleName}, mark one with @Column(primaryKey = true)")
        }

        return SqlPredicate.where(
            "${primaryKey.field.column} = :${primaryKey.field.variable}",
            primaryKey.field.variable to primaryKey.value(entity)
        )
    }

    /**
     * Add an interceptor for entities before they are going to be inserted or updated in the
     * database, this allows for easily updating a `updated_at` field, logging, or validating data, you however aren't
     * able to prevent an update or insert operation from being performed.
     *
     * @param interceptor The interceptor itself
     */
    fun interceptor(interceptor: Interceptor<T>) {
        interceptors.add(interceptor)
    }

    /**
     * Run an entity through the interceptors
     * @param entity The entity to run through the interceptors
     */
    private fun intercept(entity: T): T {
        return interceptors.fold(entity) { acc, interceptor -> interceptor.intercept(acc) }
    }

    /**
     * Get all rows from the table
     * @param filter The filter to apply to the query
     * @return A list of all entities in the table
     */
    suspend fun all(filter: SqlFilter<T>? = null): List<T> {
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
        return engine.insert(intercept(entity), fetch)
    }

    /**
     * Insert multiple entities into the table
     * @param entities The list of entities to insert
     * @param fetch Whether to fetch the entities after inserting them
     * @return The entities that were inserted, if `fetch` is true then this will reflect the
     * database values, otherwise it will be exact same list that was passed in
     */
    suspend fun insertMany(entities: Collection<T>, fetch: Boolean = false): List<T> {
        return engine.insertMany(entities.map { intercept(it) }, fetch)
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
    suspend fun first(filter: SqlFilter<T>? = null): T? {
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
    suspend fun delete(filter: SqlFilter<T>) {
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
     * @param fetch Whether to fetch the entity after inserting it
     */
    suspend fun update(entity: T, fetch: Boolean = false): T {
        return engine.update(intercept(entity), fetch)
    }

    /**
     * Upsert an entity into the table, if it is already in the table it will be updated, otherwise it will be inserted
     * @param entity The entity to upsert
     * @param fetch Whether to fetch the entity after inserting or updating it
     */
    suspend fun upsert(entity: T, fetch: Boolean = false): T {
        return engine.upsert(intercept(entity), fetch)
    }

    companion object {
        private val tableRegex = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
        private val controllers: MutableMap<Iron, MutableMap<Class<*>, TableController<*>>> = mutableMapOf()

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T: Any> getController(clazz: Class<T>, iron: Iron): TableController<T> {
            return controllers.getOrPut(iron) { mutableMapOf() }
                .getOrPut(clazz) { TableController(iron, clazz) } as TableController<T>
        }
    }
}

inline fun <reified T: Any> Iron.controller(): TableController<T> {
    return TableController.getController(T::class.java, this)
}