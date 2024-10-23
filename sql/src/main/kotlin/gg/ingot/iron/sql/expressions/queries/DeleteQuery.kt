package gg.ingot.iron.sql.expressions.queries

import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.expressions.filter.Filter
import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.scopes.delete.DeleteScope
import gg.ingot.iron.sql.scopes.delete.FromDeleteScope
import gg.ingot.iron.sql.scopes.delete.ReturningDeleteScope
import gg.ingot.iron.sql.scopes.delete.WhereDeleteScope
import gg.ingot.iron.sql.types.Expression
import gg.ingot.iron.sql.types.column
import java.util.function.Supplier

internal class DeleteQuery(private val sql: Sql): Sql(sql.driver, sql.builder),
    DeleteScope, FromDeleteScope, ReturningDeleteScope, WhereDeleteScope {
    override fun from(table: String): FromDeleteScope {
        return modify(this) {
            append("FROM", sql.driver.literal(table))
        }
    }

    override fun from(table: SqlTable): FromDeleteScope {
        return from(table.name)
    }

    override fun where(expression: String): WhereDeleteScope {
        return modify(this) {
            append("WHERE", expression)
        }
    }

    override fun where(filter: Supplier<Filter>): WhereDeleteScope {
        return where(filter.get())
    }

    override fun where(filter: Filter): WhereDeleteScope {
        return modify(this) {
            append("WHERE", filter.asString(sql))
        }
    }

    override fun returning(vararg columns: String): Scope {
        return returning(*columns.map { column(it) }.toTypedArray())
    }

    override fun returning(vararg columns: Expression): Scope {
        return modify(this) {
            append("RETURNING", columns.joinToString(", ") { it.asString(sql) })
        }
    }

    override fun returning(): Scope {
        return returning("*")
    }


}