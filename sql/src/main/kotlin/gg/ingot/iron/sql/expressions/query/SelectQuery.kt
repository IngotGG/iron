package gg.ingot.iron.sql.expressions.query

import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.builder.SqlBuilder
import gg.ingot.iron.sql.expressions.Entrypoint
import gg.ingot.iron.sql.scopes.select.DistinctSelectScope
import gg.ingot.iron.sql.scopes.select.FromSelectScope
import gg.ingot.iron.sql.scopes.select.SelectScope
import java.util.function.Consumer

internal class SelectQuery(private val sql: Sql): Sql(sql.driver, sql.builder), SelectScope, DistinctSelectScope {
    override fun distinct(): DistinctSelectScope {
        return modify(this) {
            if (!builder.contains("DISTINCT")) {
                val index = builder.lastIndexOf("SELECT")
                builder.append("DISTINCT", index + 1)
            }
        }
    }

    override fun from(table: String): FromSelectScope {
        return modify(FromQuery(this)) {
            append("FROM", sql.driver.literal(table))
        }
    }

    override fun from(table: SqlTable): FromSelectScope {
        return from(table.name)
    }

    override fun from(subquery: Consumer<Entrypoint>): FromSelectScope {
        val sql = Entrypoint(Sql(sql.driver, SqlBuilder()))
        subquery.accept(sql)

        return modify(FromQuery(this)) {
            append("FROM")
            append(sql.builder)
        }
    }

    override fun from(subquery: Entrypoint.() -> Unit): FromSelectScope {
        return from(Consumer {
            subquery(it)
        })
    }
}