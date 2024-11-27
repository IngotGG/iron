package gg.ingot.iron.sql.scopes.select

import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.scopes.select.partial.GroupByPartial
import gg.ingot.iron.sql.scopes.select.partial.LimitPartial
import gg.ingot.iron.sql.scopes.select.partial.OrderByPartial

/**
 * The scope for the `SELECT FROM WHERE` clause.
 * @author santio
 * @since 2.0
 */
interface WhereSelectScope: Scope, GroupByPartial, OrderByPartial, LimitPartial