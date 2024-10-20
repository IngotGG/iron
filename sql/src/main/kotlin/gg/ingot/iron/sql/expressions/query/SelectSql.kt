package gg.ingot.iron.sql.expressions.query

import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.Sql

class SelectSql(private val sql: Sql): Sql(sql.driver, sql.builder) {
    fun from(table: String): FromSql {
        return modify(FromSql(this)) {
            append("FROM ${sql.driver.literal(table)}")
        }
    }

    fun from(table: SqlTable): FromSql {
        return from(table.name)
    }
}