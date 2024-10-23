package gg.ingot.iron.sql.expressions.queries

import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.builder.SqlBuilder
import gg.ingot.iron.sql.expressions.Entrypoint
import gg.ingot.iron.sql.expressions.filter.Filter
import gg.ingot.iron.sql.expressions.ordering.Order
import gg.ingot.iron.sql.expressions.queries.sub.JoinQuery
import gg.ingot.iron.sql.scopes.select.*
import gg.ingot.iron.sql.types.ExpColumn
import gg.ingot.iron.sql.types.column
import java.util.function.Consumer
import java.util.function.Supplier

internal class SelectQuery(private val sql: Sql): Sql(sql.driver, sql.builder),
    FromSelectScope, WhereSelectScope, LimitSelectScope, OffsetSelectScope,
    AliasFromSelectScope, HavingSelectScope, OrderBySelectScope, GroupBySelectScope,
    SelectScope, DistinctSelectScope {

    override fun distinct(): DistinctSelectScope {
        return modify(this) {
            if (!builder.contains("DISTINCT")) {
                val index = builder.lastIndexOf("SELECT")
                builder.append("DISTINCT", index + 1)
            }
        }
    }

    override fun from(table: String): FromSelectScope {
        return modify(SelectQuery(this)) {
            append("FROM", sql.driver.literal(table))
        }
    }

    override fun from(table: SqlTable): FromSelectScope {
        return from(table.name)
    }

    override fun from(subquery: Consumer<Entrypoint>): FromSelectScope {
        val sql = Entrypoint(Sql(sql.driver, SqlBuilder()))
        subquery.accept(sql)

        return modify(SelectQuery(this)) {
            append("FROM")
            append(sql.builder)
        }
    }

    override fun from(subquery: Entrypoint.() -> Unit): FromSelectScope {
        return from(Consumer {
            subquery(it)
        })
    }

    override fun where(expression: String): WhereSelectScope {
        return modify(SelectQuery(this)) {
            append("WHERE", expression)
        }
    }

    override fun where(filter: Filter): WhereSelectScope {
        return modify(SelectQuery(this)) {
            append("WHERE", filter.asString(sql))
        }
    }

    override fun where(filter: Supplier<Filter>): WhereSelectScope {
        return where(filter.get())
    }

    override fun limit(limit: Int): LimitSelectScope {
        return modify(SelectQuery(this)) {
            append("LIMIT", limit.toString())
        }
    }

    override fun offset(offset: Int): OffsetSelectScope {
        return modify(SelectQuery(this)) {
            append("OFFSET", offset.toString())
        }
    }

    override fun groupBy(vararg columns: String): GroupBySelectScope {
        return groupBy(columns.map { column(it) })
    }

    override fun groupBy(vararg columns: ExpColumn): GroupBySelectScope {
        return groupBy(columns.toList())
    }

    override fun groupBy(columns: String): GroupBySelectScope {
        return groupBy(column(columns))
    }

    override fun groupBy(column: ExpColumn): GroupBySelectScope {
        return groupBy(listOf(column))
    }

    override fun groupBy(columns: List<ExpColumn>): GroupBySelectScope {
        return modify(this) {
            append("GROUP BY", columns.joinToString(", ") { it.qualified(sql) })
        }
    }

    override fun orderBy(vararg columns: String): OrderBySelectScope {
        return orderBy(columns.map { Order(it) })
    }

    override fun orderBy(vararg order: Order): OrderBySelectScope {
        return orderBy(order.toList())
    }

    override fun orderBy(vararg columns: ExpColumn): OrderBySelectScope {
        return orderBy(columns.map { Order(it.qualified(sql)) })
    }

    override fun orderBy(order: List<Order>): OrderBySelectScope {
        return modify(this) {
            append("ORDER BY", order.joinToString(", ") { it.toString() })
        }
    }

    override fun orderBy(column: ExpColumn): OrderBySelectScope {
        return orderBy(listOf(Order(column.qualified(sql))))
    }

    override fun orderBy(column: String): OrderBySelectScope {
        return orderBy(column(column))
    }

    override fun orderBy(order: Order): OrderBySelectScope {
        return orderBy(listOf(order))
    }

    override fun join(subquery: Consumer<Entrypoint>): JoinSelectScope {
        val sql = Entrypoint(Sql(sql.driver, SqlBuilder()))
        subquery.accept(sql)

        return modify(JoinQuery(this)) {
            append("INNER JOIN")
            append(sql.builder)
        }
    }

    override fun join(subquery: Entrypoint.() -> Unit): JoinSelectScope {
        return join(Consumer {
            subquery(it)
        })
    }

    override fun join(alias: String, subquery: Consumer<Entrypoint>): JoinSelectScope {
        return join(subquery).alias(alias)
    }

    override fun join(alias: String, subquery: Entrypoint.() -> Unit): JoinSelectScope {
        return join(subquery).alias(alias)
    }

    override fun alias(alias: String): AliasFromSelectScope {
        return modify(this) {
            val hasAlias = builder.get(-2) == "AS"
            if (hasAlias) return@modify

            append("AS", sql.driver.literal(alias))
        }
    }

    override fun having(expression: String): HavingSelectScope {
        return modify(SelectQuery(this)) {
            append("HAVING", expression)
        }
    }

    override fun having(filter: Filter): HavingSelectScope {
        return modify(SelectQuery(this)) {
            append("HAVING", filter.asString(sql))
        }
    }

    override fun having(filter: Supplier<Filter>): HavingSelectScope {
        return having(filter.get())
    }

}