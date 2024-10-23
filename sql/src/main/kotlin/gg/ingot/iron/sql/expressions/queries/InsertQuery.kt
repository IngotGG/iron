package gg.ingot.iron.sql.expressions.queries

import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.scopes.insert.*
import gg.ingot.iron.sql.types.ExpValue
import gg.ingot.iron.sql.types.Expression
import gg.ingot.iron.sql.types.column

internal class InsertQuery(private val sql: Sql): Sql(sql.driver, sql.builder),
    InsertScope, IntoInsertScope, ReturningInsertScope, ValuesInsertScope,
    ColumnsInsertScope, ConditionedInsertScope, DefaultValuesInsertScope {
    override fun into(table: String): IntoInsertScope {
        return modify(this) {
            append("INTO", sql.driver.literal(table))
        }
    }

    override fun into(table: String, alias: String): IntoInsertScope {
        return modify(this) {
            append("INTO", sql.driver.literal(table), "AS", sql.driver.literal(alias))
        }
    }

    override fun into(table: SqlTable): IntoInsertScope {
        return into(table.name)
    }

    override fun orIgnore(): ConditionedInsertScope {
        return modify(this) {
            append("OR IGNORE")
        }
    }

    override fun orReplace(): ConditionedInsertScope {
        return modify(this) {
            append("OR REPLACE")
        }
    }

    override fun orRollback(): ConditionedInsertScope {
        return modify(this) {
            append("OR ROLLBACK")
        }
    }

    override fun orAbort(): ConditionedInsertScope {
        return modify(this) {
            append("OR ABORT")
        }
    }

    override fun orFail(): ConditionedInsertScope {
        return modify(this) {
            append("OR FAIL")
        }
    }

    override fun columns(vararg columns: String): ColumnsInsertScope {
        return columns(*columns.map { column(it) }.toTypedArray())
    }

    override fun columns(vararg columns: Expression): ColumnsInsertScope {
        return modify(this) {
            append("(${columns.joinToString(", ") { it.asString(sql) }})")
        }
    }

    override fun defaultValues(): DefaultValuesInsertScope {
        return modify(this) {
            append("DEFAULT VALUES")
        }
    }

    override fun values(size: Int): ValuesInsertScope {
        return values(*Array(size) { ExpValue.placeholder() })
    }

    override fun values(vararg values: Any?): ValuesInsertScope {
        return values(*values.map { ExpValue.of(it) }.toTypedArray())
    }

    override fun values(vararg values: Expression): ValuesInsertScope {
        return modify(this) {
            val previous = get(-1) ?: error("values() called too early")
            val isValues = get(-2) == "VALUES"
                || (contains("VALUES") && previous.startsWith("(") && previous.endsWith(")"))

            if (isValues) {
                replace(-1, "$previous,")
                append("(${
                    values.joinToString(", ") { it.asString(sql) }
                })")
            } else {
                append("VALUES", "(${
                    values.joinToString(", ") { it.asString(sql) }
                })")
            }
        }
    }

    override fun returning(vararg columns: String): ReturningInsertScope {
        return returning(*columns.map { column(it) }.toTypedArray())
    }

    override fun returning(vararg columns: Expression): ReturningInsertScope {
        return modify(this) {
            append("RETURNING", columns.joinToString(", ") { it.asString(sql) })
        }
    }

    override fun returning(): ReturningInsertScope {
        return returning("*")
    }
}