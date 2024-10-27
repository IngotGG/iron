package gg.ingot.iron.sql.scopes.update

import gg.ingot.iron.sql.expressions.filter.Filter
import gg.ingot.iron.sql.scopes.Scope
import java.util.function.Supplier

/**
 * The scope for the `UPDATE SET` clause.
 * @author santio
 * @since 2.0
 */
interface SetUpdateScope: Scope {

    fun where(expression: String): WhereUpdateScope
    fun where(filter: Supplier<Filter>): WhereUpdateScope
    fun where(filter: Filter?): WhereUpdateScope

}
