package gg.ingot.iron.sql.scopes.select

import gg.ingot.iron.sql.expressions.filter.Filter
import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.scopes.select.partial.LimitPartial
import gg.ingot.iron.sql.scopes.select.partial.OrderByPartial
import java.util.function.Supplier

/**
 * The scope for the `SELECT FROM GROUP BY` clause.
 * @author santio
 * @since 2.0
 */
interface GroupBySelectScope: Scope, OrderByPartial, LimitPartial {

    fun having(expression: String): HavingSelectScope
    fun having(filter: Filter): HavingSelectScope
    fun having(filter: Supplier<Filter>): HavingSelectScope

}
