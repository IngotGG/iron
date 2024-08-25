package gg.ingot.iron.controller.engine.impl

import gg.ingot.iron.Iron
import gg.ingot.iron.controller.controller.TableController
import gg.ingot.iron.controller.engine.DBMSEngine
import gg.ingot.iron.controller.query.SQL
import gg.ingot.iron.controller.query.SqlFilter

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
                .all(controller.clazz.kotlin)
        } else {
            iron.prepare("SELECT * FROM ${controller.tableName} WHERE $predicate", predicate.params())
                .all(controller.clazz.kotlin)
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
                val columns = controller.model.fields.joinToString(",") { column(it.columnName) }
                val variables = controller.model.fields.joinToString(",") { ":${it.variableName}" }

                prepare(
                    "INSERT INTO ${controller.tableName} ($columns) VALUES ($variables)",
                    entity
                )

                if (fetch) {
                    val selector = controller.uniqueSelector(entity)
                    prepare("SELECT * FROM ${controller.tableName} WHERE $selector", selector.params())
                        .single(controller.clazz.kotlin)
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
                .singleNullable(controller.clazz.kotlin)
        } else {
            iron.prepare("SELECT * FROM ${controller.tableName} WHERE $predicate", predicate.params())
                .singleNullable(controller.clazz.kotlin)
        }
    }

    @Suppress("SqlWithoutWhere")
    override suspend fun clear() {
        iron.prepare("DELETE FROM ${controller.tableName}")
    }

    override suspend fun delete(filter: SqlFilter<T>) {
        val scope = SQL<T>(iron, controller.model, this)
        val predicate = filter.invoke(scope)

        iron.prepare("DELETE FROM ${controller.tableName} WHERE $predicate", predicate.params())
    }

    override suspend fun delete(entity: T) {
        val selector = controller.uniqueSelector(entity)
        iron.prepare("DELETE FROM ${controller.tableName} WHERE $selector", selector.params())
    }

    override suspend fun update(entity: T, fetch: Boolean): T {
        val selector = controller.uniqueSelector(entity)
        val columns = controller.model.fields.joinToString(", ") { "${column(it.columnName)} = :${it.variableName}" }

        return iron.transaction {
            prepare(
                "UPDATE ${controller.tableName} SET $columns WHERE $selector",
                selector.params(), entity
            )

            if (fetch) {
                prepare("SELECT * FROM ${controller.tableName} WHERE $selector", selector.params(), entity)
                    .single(controller.clazz.kotlin)
            } else {
                entity
            }
        }
    }
}