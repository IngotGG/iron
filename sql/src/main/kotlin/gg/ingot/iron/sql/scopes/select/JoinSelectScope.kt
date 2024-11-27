package gg.ingot.iron.sql.scopes.select

import gg.ingot.iron.sql.expressions.SQL
import gg.ingot.iron.sql.expressions.filter.Filter
import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.scopes.select.partial.GroupByPartial
import gg.ingot.iron.sql.scopes.select.partial.LimitPartial
import gg.ingot.iron.sql.scopes.select.partial.OrderByPartial
import java.util.function.Supplier

/**
 * The scope for the `SELECT FROM JOIN` clause.
 * @author santio
 * @since 2.0
 */
interface JoinSelectScope: Scope, OrderByPartial, GroupByPartial, LimitPartial {

    infix fun join(subquery: SQL): JoinSelectScope
    fun join(alias: String, subquery: SQL): JoinSelectScope

    infix fun alias(alias: String): JoinSelectScope
    infix fun on(filter: Supplier<Filter>): FromSelectScope

}