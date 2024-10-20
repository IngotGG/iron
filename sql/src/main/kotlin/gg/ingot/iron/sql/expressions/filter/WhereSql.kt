package gg.ingot.iron.sql.expressions.filter

import gg.ingot.iron.sql.Sql

class WhereSql(private val sql: Sql): Sql(sql.driver, sql.builder) {
}