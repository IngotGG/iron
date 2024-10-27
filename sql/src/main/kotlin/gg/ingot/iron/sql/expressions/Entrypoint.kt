package gg.ingot.iron.sql.expressions

import gg.ingot.iron.DBMS
import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.expressions.queries.*
import gg.ingot.iron.sql.scopes.alter.AlterScope
import gg.ingot.iron.sql.scopes.delete.DeleteScope
import gg.ingot.iron.sql.scopes.drop.DropScope
import gg.ingot.iron.sql.scopes.insert.InsertScope
import gg.ingot.iron.sql.scopes.select.SelectScope
import gg.ingot.iron.sql.scopes.update.UpdateScope
import gg.ingot.iron.sql.types.Expression
import gg.ingot.iron.sql.types.column

/**
 * The entrypoint for the SQL DSL, requires the driver to be set to generate coherent SQL.
 * @author santio
 * @since 2.0
 */
@Suppress("MemberVisibilityCanBePrivate")
open class Entrypoint internal constructor(
    val sql: Sql,
): Sql(sql.driver, sql.builder) {
    constructor(dbms: DBMS): this(Sql(dbms))

    /**
     * Selects all columns from the database.
     * @return A type-safe API for chaining operations
     */
    fun select(): SelectScope {
        return select("*")
    }

    /**
     * Selects the specified columns from the database.
     * @param columns The columns to select
     * @return A type-safe API for chaining operations
     */
    fun select(vararg columns: String): SelectScope {
        return select(*columns.map { column(it) }.toTypedArray())
    }

    /**
     * Selects the specified columns from the database.
     * @param columns The columns to select
     * @return A type-safe API for chaining operations
     * @see column
     */
    fun select(vararg columns: Expression): SelectScope {
        return modify(SelectQuery(this)) {
            append("SELECT", columns.joinToString(", ") { it.asString(sql) })
        }
    }

    /**
     * Insert data into a specified table.
     * @return A type-safe API for chaining operations
     */
    fun insert(): InsertScope {
        return modify(InsertQuery(this)) {
            append("INSERT")
        }
    }

    /**
     * Delete data from a specified table.
     * @return A type-safe API for chaining operations
     */
    fun delete(): DeleteScope {
        return modify(DeleteQuery(this)) {
            append("DELETE")
        }
    }

    /**
     * Drop a table.
     * @return A type-safe API for chaining operations
     */
    fun drop(): DropScope {
        return modify(DropQuery(this)) {
            append("DROP")
        }
    }

    /**
     * Alter a table
     * @return A type-safe API for chaining operations
     */
    fun alter(): AlterScope {
        return modify(AlterQuery(this)) {
            append("ALTER TABLE")
        }
    }

    /**
     * Update data in a specified table.
     * @return A type-safe API for chaining operations
     */
    fun update(table: String): UpdateScope {
        return modify(UpdateQuery(this)) {
            append("UPDATE", table)
        }
    }

    /**
     * Update data in a specified table.
     * @return A type-safe API for chaining operations
     */
    fun update(table: SqlTable): UpdateScope {
        return update(table.name)
    }
}

typealias SQL = Entrypoint.() -> Unit