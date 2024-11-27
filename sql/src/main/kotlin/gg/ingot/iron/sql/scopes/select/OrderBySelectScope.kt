package gg.ingot.iron.sql.scopes.select

import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.scopes.select.partial.LimitPartial

/**
 * The scope for the `SELECT FROM ORDER BY` clause.
 * @author santio
 * @since 2.0
 */
interface OrderBySelectScope: Scope, LimitPartial