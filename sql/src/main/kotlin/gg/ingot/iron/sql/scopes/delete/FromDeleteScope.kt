package gg.ingot.iron.sql.scopes.delete

import gg.ingot.iron.sql.expressions.filter.Filter
import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.scopes.partial.ReturningPartial
import java.util.function.Supplier

/**
 * The scope for the `DELETE FROM` clause.
 * @author santio
 * @since 2.0
 */
interface FromDeleteScope: Scope, ReturningPartial {

    infix fun where(expression: String): WhereDeleteScope
    infix fun where(filter: Supplier<Filter>): WhereDeleteScope
    infix fun where(filter: Filter): WhereDeleteScope

}
