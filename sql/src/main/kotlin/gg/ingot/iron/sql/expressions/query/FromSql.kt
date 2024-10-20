package gg.ingot.iron.sql.expressions.query

import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.expressions.filter.Filter

class FromSql(private val sql: Sql): Sql(sql.driver, sql.builder) {

    fun where(expression: String): Sql {
        return modify(FromSql(this)) {
            append("WHERE $expression")
        }
    }

    fun where(filter: Filter): Sql {
        return modify(FromSql(this)) {
            append("WHERE ${filter.asString(sql)}")
        }
    }

}