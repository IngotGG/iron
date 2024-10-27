package gg.ingot.iron.controller.controller

import gg.ingot.iron.Iron
import gg.ingot.iron.bindings.Bindings
import gg.ingot.iron.controller.query.SQL
import gg.ingot.iron.controller.query.SqlFilter
import gg.ingot.iron.controller.query.SqlPredicate
import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.expressions.filter.Filter
import gg.ingot.iron.sql.expressions.filter.eq
import gg.ingot.iron.sql.scopes.insert.ValuesInsertScope
import gg.ingot.iron.sql.types.column
import gg.ingot.iron.sql.types.count as sqlCount

/**
 * A controller for working with a model in the database. This provides an ORM-like interface for
 * working with entities.
 * @param clazz The class to use for the model
 * @author santio
 * @since 2.0
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class TableController<T: Any>(val iron: Iron, internal val clazz: Class<T>) {

    val table = SqlTable.get(clazz)
        ?: error("Class ${clazz.simpleName} is not a model, please make sure you annotate your model with @Model")

    /**
     * Creates a selector for the entity that only selects the primary keys
     * @param entity The entity to create a unique selector for
     * @return A unique selector for the entity
     */
    fun selector(entity: T): SqlFilter<T> {
        val primaryKeys = table.columns.filter { it.primaryKey }
        if (primaryKeys.isEmpty()) {
            error("No primary keys found for ${clazz.simpleName}, mark one or more with @Column(primaryKey = true)")
        }

        var condition: Filter? = null
        primaryKeys.forEach {
            val value = iron.resultMapper.serialize(it, it.value(entity))
            condition = condition?.and(column(it.name) eq value)
                ?: (column(it.name) eq value)
        }

        return condition?.let { { SqlPredicate(it) } }
            ?: error("No primary keys found for ${clazz.simpleName}, mark one or more with @Column(primaryKey = true)")
    }

    /**
     * Get all rows from the table
     * @param filter The filter to apply to the query
     * @return A list of all entities in the table
     */
    suspend fun all(filter: SqlFilter<T>? = null): List<T> {
        return iron.run {
            select().from(table.name).where(filter?.invoke(SQL(iron, table))?.condition)
        }.all(clazz)
    }

    /**
     * Insert an entity into the table
     * @param entity The entity to insert
     * @param fetch Whether to fetch the entity after inserting it
     * @return The entity that was inserted, if `fetch` is true then this will be the
     * complete entity, otherwise it will be exact same entity that was passed in
     */
    suspend fun insert(entity: T, fetch: Boolean = false): T {
        val bindings = Bindings.of(entity, iron)
        val values = bindings.map.values.filterNotNull()
        val columns = table.columns
            .filter { !bindings.isNull(it.variable) }

        val sql = Sql(iron.settings.driver!!)
            .insert()
            .into(table.name)
            .columns(*columns.map { it.name }.toTypedArray())
            .values(*values.toTypedArray())

        return if (fetch) {
            iron.run(sql.returning() as Sql).single(clazz)
        } else {
            iron.run(sql as Sql)
            entity
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

        val bindings = entities.map { Bindings.of(it, iron) }
        val columns = table.columns
            .filter { !bindings.first().isNull(it.variable) }

        var sql: ValuesInsertScope = Sql(iron.settings.driver!!)
            .insert()
            .into(table.name)
            .columns(*columns.map { it.name }.toTypedArray()) as ValuesInsertScope

        for (i in 1 until entities.size) {
            sql = sql.values(bindings[i].map.values.filterNotNull())
        }

        return if (fetch) {
            iron.run(sql.returning() as Sql).all(clazz)
        } else {
            iron.run(sql as Sql)
            entities
        }
    }

    /**
     * Get the size of the table
     * @param filter The filter to apply to the query
     * @return The amount of entities in the table
     */
    suspend fun count(filter: SqlFilter<T>? = null): Int {
        return iron.run {
            select(sqlCount("*"))
                .from(table.name)
                .where(filter?.invoke(SQL(iron, table))?.condition)
        }.single<Int>()
    }

    /**
     * Drop the table
     * @apiNote This is a destructive operation, it cannot be undone
     */
    suspend fun drop() {
        iron.prepare("DROP TABLE ${table.name}")
    }

    /**
     * Get the first entity from the table that matches the filter
     * @param filter The filter to apply to the query
     */
    suspend fun first(filter: SqlFilter<T>? = null): T? {
        return iron.run {
            select().from(table.name).where(filter?.invoke(SQL(iron, table))?.condition).limit(1)
        }.singleNullable(clazz)
    }

    /**
     * Delete all entities from the table
     */
    @Suppress("SqlWithoutWhere")
    suspend fun clear() {
        iron.prepare("DELETE FROM ${table.name}")
    }

    /**
     * Delete multiple entities from the table that match the filter
     * @param filter The filter to apply to the query
     */
    suspend fun delete(filter: SqlFilter<T>) {
        iron.run {
            this.delete()
                .from(table.name)
                .where(filter.invoke(SQL(iron, table)).condition)
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
     * @param filter The filter to apply to the query
     */
    suspend fun update(entity: T, filter: SqlFilter<T>): T {
        iron.run {
            val bindings = Bindings.of(entity, iron)

            update(table.name)
                .set(bindings.map.mapKeys {
                    table.columns.first { column -> column.variable == it.key }.name
                })
                .where(filter.invoke(SQL(iron, table)).condition)
        }

        return entity
    }

    /**
     * Upsert an entity into the table, if it is already in the table it will be updated, otherwise it will be inserted
     * @param entity The entity to upsert
     * @param fetch Whether to fetch the entity after inserting or updating it
     */
    suspend fun upsert(entity: T, fetch: Boolean = false): T {
        val bindings = Bindings.of(entity, iron)
        val columns = table.columns
            .filter { !bindings.isNull(it.variable) }

        val sql = Sql(iron.settings.driver!!)
            .insert()
            .orReplace()
            .into(table.name)
            .columns(*columns.map { it.name }.toTypedArray())
            .values(bindings.map.values.filterNotNull())

        return if (fetch) {
            iron.run(sql.returning() as Sql).single(clazz)
        } else {
            iron.run(sql as Sql)
            entity
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