package gg.ingot.iron.sql.scopes.select

import gg.ingot.iron.sql.expressions.Entrypoint
import gg.ingot.iron.sql.expressions.filter.Filter
import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.scopes.select.partial.GroupByPartial
import gg.ingot.iron.sql.scopes.select.partial.LimitPartial
import gg.ingot.iron.sql.scopes.select.partial.OrderByPartial
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * The scope for the `SELECT FROM AS` clause.
 * @author santio
 * @since 2.0
 */
interface AliasFromSelectScope: Scope, OrderByPartial, GroupByPartial, LimitPartial {

    infix fun where(expression: String): WhereSelectScope
    infix fun where(filter: Supplier<Filter>): WhereSelectScope
    infix fun where(filter: Filter): WhereSelectScope

    infix fun join(subquery: Consumer<Entrypoint>): JoinSelectScope
    fun join(alias: String, subquery: Consumer<Entrypoint>): JoinSelectScope

}
