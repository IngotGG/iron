package gg.ingot.iron.controller.engine.impl

import gg.ingot.iron.Iron
import gg.ingot.iron.controller.engine.DBMSEngine
import gg.ingot.iron.controller.query.SQL
import gg.ingot.iron.controller.query.SqlPredicate
import gg.ingot.iron.controller.tables.TableController

@Suppress("DuplicatedCode")
class SqliteEngine<T: Any>(
    iron: Iron,
    controller: TableController<T>
): DBMSEngine<T>(iron, controller) {
    override suspend fun all(filter: (SQL<T>.() -> SqlPredicate)?): List<T> {
        val scope = SQL<T>(iron, controller.model)
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

    override suspend fun insert(entity: T, pull: Boolean): T {
        val columns = controller.model.fields.joinToString(",") { it.columnName }
        val variables = controller.model.fields.joinToString(",") { ":${it.variableName}" }

        if (pull) {
            return iron.transaction {
                prepare(
                    "INSERT INTO ${controller.tableName} ($columns) VALUES ($variables) RETURNING *",
                    entity
                ).single(controller.clazz.kotlin)
            }
        } else {
            iron.prepare(
                "INSERT INTO ${controller.tableName} ($columns) VALUES ($variables)",
                entity
            )

            return entity
        }
    }

    override suspend fun first(filter: (SQL<T>.() -> SqlPredicate)?): T? {
        val scope = SQL<T>(iron, controller.model)
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

    override suspend fun delete(filter: (SQL<T>.() -> SqlPredicate)) {
        val scope = SQL<T>(iron, controller.model)
        val predicate = filter.invoke(scope)

        iron.prepare("DELETE FROM ${controller.tableName} WHERE $predicate", predicate.params())
    }

    override suspend fun delete(entity: T) {
        val selector = controller.uniqueSelector(entity)
        iron.prepare("DELETE FROM ${controller.tableName} WHERE $selector", selector.params())
    }

    override suspend fun update(entity: T) {
        val selector = controller.uniqueSelector(entity)
        val columns = controller.model.fields.joinToString(", ") { "${it.columnName} = :${it.variableName}" }

        iron.prepare("UPDATE ${controller.tableName} SET $columns WHERE $selector", selector.params() + entity)
    }
}