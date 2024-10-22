package gg.ingot.iron.sql.scopes.select

import gg.ingot.iron.sql.expressions.filter.Filter
import gg.ingot.iron.sql.scopes.Scope
import java.util.function.Supplier

/**
 * The scope for the `SELECT FROM JOIN` clause.
 * @author santio
 * @since 2.0
 */
interface JoinSelectScope: Scope {

    infix fun alias(alias: String): JoinSelectScope
    infix fun on(filter: Supplier<Filter>): FromSelectScope

}