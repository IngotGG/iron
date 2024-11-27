package gg.ingot.iron.sql.expressions.queries

import gg.ingot.iron.DBMS
import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.expressions.filter.Filter
import gg.ingot.iron.sql.expressions.filter.eq
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
            when (sql.driver) {
                DBMS.H2 -> replace(0, "MERGE")
                DBMS.SQLITE, DBMS.MYSQL -> replace(0, "REPLACE")
                DBMS.POSTGRESQL -> error("Postgres does not support OR REPLACE without specifying the primary key")
                else -> append("OR REPLACE")
            }
        }
    }

    override fun orReplace(vararg primaryKey: String): ConditionedInsertScope {
        return orReplace(*primaryKey.map { column(it) }.toTypedArray())
    }

    override fun orReplace(vararg primaryKey: Expression): ConditionedInsertScope {
        return when(sql.driver) {
            DBMS.POSTGRESQL -> modify(this) {
                preBuild {
                    statements().last().modify {
                        append("ON CONFLICT (${primaryKey.joinToString(", ") { it.asString(sql) }}) DO UPDATE", "SET")

                        val changes = context.changes().firstOrNull() ?: error("Failed to get changes from query, make sure you are using the correct syntax for your database")
                        var index = 0

                        changes.forEach { (column, value) ->
                            if (index > 0) replace(-1, get(-1) + ",")
                            append(column, "=", ExpValue.placeholder().asString(sql))
                            addValue(value)

                            index++
                        }
                    }
                }
            }
            else -> orReplace()
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

    override fun values(vararg values: Any?): ValuesInsertScope {
        return modify(this) {
            val previous = get(-1) ?: error("values() called too early")
            val isValues = get(-2) == "VALUES"
                || (contains("VALUES") && previous.startsWith("(") && previous.endsWith(")"))

            if (isValues) {
                replace(-1, "$previous,")
            } else append("VALUES")

            append("(${
                values.joinToString(", ") { "?" }
            })")

            addValue(*values)
        }
    }

    override fun returning(vararg columns: String): ReturningInsertScope {
        return returning(*columns.map { column(it) }.toTypedArray())
    }

    override fun returning(vararg columns: Expression): ReturningInsertScope {
        return modify(this) {
            when (sql.driver) {
                DBMS.MYSQL, DBMS.H2, DBMS.ORACLE -> {
                    // Add another statement to get the requested columns
                    val table = context.table()
                        ?: error("Failed to get table name from query, make sure you are using the correct syntax for your database")

                    val insertedColumns = context.columns()
                    if (insertedColumns.isEmpty()) error("You need to specify the columns with columns(...) when using RETURNING to allow Iron to generate a SELECT statement for you (limitation by ${sql.driver.name})")

                    val values = this.values()

                    var filter: Filter? = null
                    val chunked = values.chunked(insertedColumns.size)

                    chunked.forEach {
                        var current: Filter? = null

                        it.forEachIndexed { index, value ->
                            val column = insertedColumns[index]
                            val columnFilter = column(column) eq value
                            current = current?.and(columnFilter) ?: columnFilter
                        }

                        if (current != null) filter = filter?.or(current!!)
                            ?: current
                    }

                    this.next()
                    append(
                        Sql(sql.driver)
                            .select(*columns)
                            .from(table)
                            .where(filter!!)
                            .toString()
                    )
                }
                DBMS.DB2 -> {
                    wrap(
                        "SELECT ${
                            columns.joinToString(", ") { it.asString(sql) }
                        } FROM (",
                        ") t"
                    )
                }
                DBMS.SQLSERVER -> {
                    val values = lastIndexOf("VALUES")
                    append(values, "OUTPUT", columns.joinToString(", ") { it.asString(sql) })
                }
                else -> {
                    preBuild {
                        statements().last().modify {
                            append("RETURNING", columns.joinToString(", ") { it.asString(sql) })
                        }
                    }
                }
            }
        }
    }

    override fun returning(): ReturningInsertScope {
        return returning("*")
    }
}