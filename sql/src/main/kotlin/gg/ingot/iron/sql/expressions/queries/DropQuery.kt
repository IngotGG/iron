package gg.ingot.iron.sql.expressions.queries

import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.scopes.drop.DropScope
import gg.ingot.iron.sql.scopes.drop.TableDropScope

internal class DropQuery(private val sql: Sql): Sql(sql.driver, sql.builder),
    DropScope, TableDropScope {
    override fun table(table: String): TableDropScope {
        return modify(this) {
            append("TABLE", sql.driver.literal(table))
        }
    }

    override fun table(table: SqlTable): TableDropScope {
        return table(table.name)
    }
}