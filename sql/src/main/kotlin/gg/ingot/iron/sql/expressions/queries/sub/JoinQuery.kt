package gg.ingot.iron.sql.expressions.queries.sub

import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.expressions.filter.Filter
import gg.ingot.iron.sql.expressions.queries.SelectQuery
import gg.ingot.iron.sql.scopes.select.JoinSelectScope
import java.util.function.Supplier

internal class JoinQuery(
    private val sql: SelectQuery,
): Sql(sql.driver, sql.builder), JoinSelectScope {
    override infix fun alias(alias: String): JoinQuery {
        return modify(this) {
            val hasAlias = builder.get(-2) == "AS"
            if (hasAlias) return@modify

            append("AS", sql.driver.literal(alias))
        }
    }

    override infix fun on(filter: Supplier<Filter>): SelectQuery {
        return modify(sql) {
            append("ON", filter.get().asString(sql))
        }
    }
}