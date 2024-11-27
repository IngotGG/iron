package gg.ingot.iron.sql.expressions.queries.sub

import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.expressions.SQL
import gg.ingot.iron.sql.expressions.filter.Filter
import gg.ingot.iron.sql.expressions.ordering.Order
import gg.ingot.iron.sql.expressions.queries.SelectQuery
import gg.ingot.iron.sql.scopes.select.GroupBySelectScope
import gg.ingot.iron.sql.scopes.select.JoinSelectScope
import gg.ingot.iron.sql.scopes.select.LimitSelectScope
import gg.ingot.iron.sql.scopes.select.OrderBySelectScope
import gg.ingot.iron.sql.types.ExpColumn
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

    override fun join(subquery: SQL): JoinSelectScope {
        return sql.join(subquery)
    }

    override fun join(alias: String, subquery: SQL): JoinSelectScope {
        return sql.join(alias, subquery)
    }

    override fun orderBy(vararg order: Order): OrderBySelectScope {
        return sql.orderBy(*order)
    }

    override fun orderBy(vararg columns: String): OrderBySelectScope {
        return sql.orderBy(*columns)
    }

    override fun orderBy(vararg columns: ExpColumn): OrderBySelectScope {
        return sql.orderBy(*columns)
    }

    override fun orderBy(order: List<Order>): OrderBySelectScope {
        return sql.orderBy(order)
    }

    override fun orderBy(column: ExpColumn): OrderBySelectScope {
        return sql.orderBy(column)
    }

    override fun orderBy(column: String): OrderBySelectScope {
        return sql.orderBy(column)
    }

    override fun orderBy(order: Order): OrderBySelectScope {
        return sql.orderBy(order)
    }

    override fun groupBy(vararg columns: String): GroupBySelectScope {
        return sql.groupBy(*columns)
    }

    override fun groupBy(vararg columns: ExpColumn): GroupBySelectScope {
        return sql.groupBy(*columns)
    }

    override fun groupBy(columns: List<ExpColumn>): GroupBySelectScope {
        return sql.groupBy(columns)
    }

    override fun groupBy(columns: String): GroupBySelectScope {
        return sql.groupBy(columns)
    }

    override fun groupBy(column: ExpColumn): GroupBySelectScope {
        return sql.groupBy(column)
    }

    override fun limit(limit: Int): LimitSelectScope {
        return sql.limit(limit)
    }
}