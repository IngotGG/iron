package gg.ingot.iron.controller.controller

import gg.ingot.iron.DBMS
import gg.ingot.iron.Iron
import gg.ingot.iron.bindings.Bindings
import gg.ingot.iron.controller.query.SqlFilter
import gg.ingot.iron.controller.query.SqlPredicate
import gg.ingot.iron.controller.query.SqlPredicate.Companion.fetchCount
import gg.ingot.iron.controller.query.SqlPredicate.Companion.where
import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.IronResultSet
import org.jooq.*
import org.jooq.impl.DSL

/**
 * A controller for working with a model in the database. This provides an ORM-like interface for
 * working with entities.
 * @param clazz The class to use for the model
 * @author santio
 * @since 2.0
 */
@Suppress("MemberVisibilityCanBePrivate")
class TableController<T: Any>(val iron: Iron, internal val clazz: Class<T>) {

    internal suspend fun <T: Any?> useJooq(block: suspend (DSLContext).() -> T): T = iron.use {
        val create: DSLContext = DSL.using(it, dialect)
        create.block()
    }

    private val dialect = when(iron.settings.driver) {
        DBMS.SQLITE -> SQLDialect.SQLITE
        DBMS.H2 -> SQLDialect.H2
        DBMS.POSTGRESQL -> SQLDialect.POSTGRES
        DBMS.MYSQL -> SQLDialect.MYSQL
        DBMS.MARIADB -> SQLDialect.MARIADB
        else -> error("Unsupported DBMS: ${iron.settings.driver}, either Iron or Jooq does not support this DBMS, please see https://www.jooq.org/download/support-matrix")
    }

    val table = SqlTable.get(clazz)
        ?: error("Class ${clazz.simpleName} is not a model, please make sure you annotate your model with @Model")

    /**
     * Creates a selector for the entity that only selects the primary keys
     * @param entity The entity to create a unique selector for
     * @return A unique selector for the entity
     */
    fun selector(entity: T): SqlFilter<T> {
        val primaryKeys = table.columns.filter { it.primaryKey }
        if (primaryKeys.isEmpty()) error("No primary keys found for ${clazz.simpleName}, mark one or more with @Column(primaryKey = true)")

        var condition: Condition? = null

        primaryKeys.forEach {
            val value = iron.resultMapper.serialize(it, it.value(entity))
            condition =
                if (condition != null) DSL.and(condition, DSL.field(col(it.name)).eq(value))
                else DSL.field(it.name).eq(value)
        }

        return { SqlPredicate(condition!!) }
    }

    /**
     * Get the table name for the controller
     * @return The table name in Jooq
     */
    private fun tableName(): Table<Record> {
        return iron.settings.driver?.literal(table.name)?.let { DSL.table(it) }
            ?: DSL.table(table.name)
    }

    /**
     * Get the literal column name for the specified column
     * @param name The column name
     * @return The column name in Jooq
     */
    private fun col(name: String): String {
        return iron.settings.driver?.literal(name) ?: name
    }

    /**
     * Get all rows from the table
     * @param filter The filter to apply to the query
     * @return A list of all entities in the table
     */
    suspend fun all(filter: SqlFilter<T>? = null): List<T> {
        return useJooq {
            val resultSet = select().from(table.name)
                .where(this@TableController, filter)
                .fetchResultSet()

            IronResultSet(resultSet, iron).all(clazz)
        }
    }

    /**
     * Insert an entity into the table
     * @param entity The entity to insert
     * @param fetch Whether to fetch the entity after inserting it
     * @return The entity that was inserted, if `fetch` is true then this will be the
     * complete entity, otherwise it will be exact same entity that was passed in
     */
    suspend fun insert(entity: T, fetch: Boolean = false): T {
        return useJooq {
            val bindings = Bindings.of(entity, iron)
            val columns = table.columns
                .filter { !bindings.isNull(it.variable) }
                .map { DSL.field(col(it.name)) }

            val insert = insertInto(tableName())
                .columns(columns)
                .values(bindings.map.values.filterNotNull())

            if (fetch) {
                val resultSet = insert
                    .returning()
                    .fetchResultSet()

                IronResultSet(resultSet, iron).single(clazz)
            } else {
                insert.execute()
                entity
            }
        }
    }

    /**
     * Insert multiple entities into the table
     * @param entities The list of entities to insert
     * @param fetch If true, the database values will be fetched and returned, otherwise the exact same list will be returned
     * @return The entities that were inserted, if `fetch` is true then this will reflect the
     * database values, otherwise it will be exact same list that was passed in
     */
    suspend fun insertMany(entities: List<T>, fetch: Boolean = false): List<T> {
        if (entities.isEmpty()) return emptyList()

        return useJooq {
            val bindings = entities.map { Bindings.of(it, iron) }
            val columns = table.columns
                .filter { !bindings.first().isNull(it.variable) }
                .map { DSL.field(col(it.name)) }

            var insert = insertInto(tableName())
                .columns(columns)
                .values(bindings.first().map.values.filterNotNull())

            bindings.drop(1).forEach {
                insert = insert.values(it.map.values)
            }

            return@useJooq if (fetch) {
                val resultSet = insert.returning().fetchResultSet()
                IronResultSet(resultSet, iron).all(clazz)
            } else {
                insert.execute()
                entities
            }
        }
    }

    /**
     * Get the size of the table
     * @param filter The filter to apply to the query
     * @return The amount of entities in the table
     */
    suspend fun count(filter: SqlFilter<T>? = null): Int {
        return useJooq {
            if (filter != null) fetchCount(tableName(), this@TableController, filter)
            else fetchCount(tableName())
        }
    }

    /**
     * Drop the table
     * @apiNote This is a destructive operation, it cannot be undone
     */
    suspend fun drop() {
        useJooq {
            dropTable(tableName())
                .execute()
        }
    }

    /**
     * Get the first entity from the table that matches the filter
     * @param filter The filter to apply to the query
     */
    suspend fun first(filter: SqlFilter<T>? = null): T? {
        return useJooq {
            val fetch = if (filter != null) {
                 select().from(tableName()).where(this@TableController, filter).limit(1)
            } else {
                select().from(tableName()).limit(1)
            }

            val resultSet = fetch.fetchResultSet()
            IronResultSet(resultSet, iron).singleNullable(clazz)
        }
    }

    /**
     * Delete all entities from the table
     */
    suspend fun clear() {
        useJooq {
            deleteFrom(tableName()).execute()
        }
    }

    /**
     * Delete multiple entities from the table that match the filter
     * @param filter The filter to apply to the query
     */
    suspend fun delete(filter: SqlFilter<T>) {
        useJooq {
            delete(tableName()).where(this@TableController, filter).execute()
        }
    }

    /**
     * Delete a single entity from the table
     * @param entity The entity to delete
     */
    suspend fun delete(entity: T) {
        delete(selector(entity))
    }

    /**
     * Update a single entity in the table
     * @param entity The entity to update
     * @param fetch Whether to fetch the entity after inserting it
     */
    suspend fun update(entity: T, fetch: Boolean = false): T {
        return useJooq {
            val bindings = Bindings.of(entity, iron)

            val update = update(tableName())
                .set(bindings.map.mapKeys {
                    table.columns.first { column -> column.variable == it.key }.name
                })

            if (fetch) {
                val resultSet = update.returning().fetchResultSet()
                IronResultSet(resultSet, iron).single(clazz)
            } else {
                update.execute()
                entity
            }
        }
    }

    /**
     * Upsert an entity into the table, if it is already in the table it will be updated, otherwise it will be inserted
     * @param entity The entity to upsert
     * @param fetch Whether to fetch the entity after inserting or updating it
     */
    suspend fun upsert(entity: T, fetch: Boolean = false): T {
        return useJooq {
            val bindings = Bindings.of(entity, iron)
            val columns = table.columns
                .filter { !bindings.isNull(it.variable) }
                .map { DSL.field(col(it.name)) }

            val upsert = insertInto(tableName())
                .columns(columns)
                .values(*bindings.map.values.toTypedArray())
                .onDuplicateKeyUpdate()
                .set(bindings.map.mapKeys {
                    table.columns.first { column -> column.variable == it.key }.name
                })

            if (fetch) {
                val resultSet = upsert.returning().fetchResultSet()
                IronResultSet(resultSet, iron).single(clazz)
            } else {
                upsert.execute()
                entity
            }
        }
    }

    companion object {
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