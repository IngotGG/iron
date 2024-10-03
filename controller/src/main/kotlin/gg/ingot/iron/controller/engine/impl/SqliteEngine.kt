package gg.ingot.iron.controller.engine.impl

import gg.ingot.iron.Iron
import gg.ingot.iron.controller.controller.TableController
import gg.ingot.iron.controller.engine.DBMSEngine
import gg.ingot.iron.controller.query.SQL
import gg.ingot.iron.controller.query.SqlFilter
import gg.ingot.iron.sql.binding.Bindings

@Suppress("DuplicatedCode")
class SqliteEngine<T: Any>(
    iron: Iron,
    controller: TableController<T>
): DBMSEngine<T>(iron, controller) {
    override fun column(name: String): String {
        return "`$name`"
    }

    override suspend fun all(filter: SqlFilter<T>?): List<T> {
        val scope = SQL<T>(iron, controller.model, this)
        val predicate = filter?.invoke(scope)

        return if (predicate == null) {
            iron.prepare("SELECT * FROM ${controller.tableName}")
                .all(controller.clazz)
        } else {
            iron.prepare("SELECT * FROM ${controller.tableName} WHERE $predicate", predicate.bindings())
                .all(controller.clazz)
        }
    }

    override suspend fun count(): Int {
        return iron.prepare("SELECT COUNT(*) FROM ${controller.tableName}")
            .single<Int>()
    }

    override suspend fun drop() {
        iron.prepare("DROP TABLE ${controller.tableName}")
    }

    override suspend fun insert(entity: T, fetch: Boolean): T {
        return insertMany(listOf(entity), fetch).first()
    }

    override suspend fun insertMany(entities: List<T>, fetch: Boolean): List<T> {
        return iron.transaction {
            return@transaction entities.map { entity ->
                val columns = controller.model.fields.joinToString(",") { column(it.field.column) }
                val variables = controller.model.fields.joinToString(",") { ":${it.field.variable}" }

                prepare(
                    "INSERT INTO ${controller.tableName} ($columns) VALUES ($variables)",
                    Bindings.get(entity, iron)
                )

                if (fetch) {
                    val selector = controller.uniqueSelector(entity)
                    prepare("SELECT * FROM ${controller.tableName} WHERE $selector", selector.bindings())
                        .single(controller.clazz)
                } else {
                    entity
                }
            }
        }
    }

    override suspend fun first(filter: SqlFilter<T>?): T? {
        val scope = SQL<T>(iron, controller.model, this)
        val predicate = filter?.invoke(scope)

        return if (predicate == null) {
            iron.prepare("SELECT * FROM ${controller.tableName} LIMIT 1")
                .singleNullable(controller.clazz)
        } else {
            iron.prepare("SELECT * FROM ${controller.tableName} WHERE $predicate LIMIT 1", predicate.bindings())
                .singleNullable(controller.clazz)
        }
    }

    @Suppress("SqlWithoutWhere")
    override suspend fun clear() {
        iron.prepare("DELETE FROM ${controller.tableName}")
    }

    override suspend fun delete(filter: SqlFilter<T>) {
        val scope = SQL<T>(iron, controller.model, this)
        val predicate = filter.invoke(scope)

        iron.prepare("DELETE FROM ${controller.tableName} WHERE $predicate", predicate.bindings())
    }

    override suspend fun delete(entity: T) {
        val selector = controller.uniqueSelector(entity)
        iron.prepare("DELETE FROM ${controller.tableName} WHERE $selector", selector.bindings())
    }

    override suspend fun update(entity: T, fetch: Boolean): T {
        val selector = controller.uniqueSelector(entity)
        val columns = controller.model.fields.joinToString(", ") { "${column(it.field.column)} = :${it.field.variable}" }

        return iron.transaction {
            prepare(
                "UPDATE ${controller.tableName} SET $columns WHERE $selector",
                selector.bindings(), Bindings.get(entity, iron)
            )

            if (fetch) {
                prepare("SELECT * FROM ${controller.tableName} WHERE $selector", selector.bindings(), Bindings.get(entity, iron))
                    .single(controller.clazz)
            } else {
                entity
            }
        }
    }

    override suspend fun upsert(entity: T, fetch: Boolean): T {
        val selector = controller.uniqueSelector(entity)
        val primaryKeys = controller.model.fields.filter { it.field.isPrimaryKey() }.joinToString(", ") { column(it.field.column) }
        val columns = controller.model.fields.joinToString(", ") { column(it.field.column) }
        val variables = controller.model.fields.joinToString(", ") { ":${it.field.variable}" }
        val updates = controller.model.fields.joinToString(", ") { "${column(it.field.column)} = :${it.field.variable}" }

        return iron.transaction {
            prepare(
                "INSERT INTO ${controller.tableName} ($columns) VALUES ($variables) ON CONFLICT($primaryKeys) DO UPDATE SET $updates",
                Bindings.get(entity, iron)
            )

            if (fetch) {
                prepare("SELECT * FROM ${controller.tableName} WHERE $selector", selector.bindings(), Bindings.get(entity, iron))
                    .single(controller.clazz)
            } else {
                entity
            }
        }
    }
}