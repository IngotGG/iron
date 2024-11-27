package gg.ingot.iron.sql.scopes.select

import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.scopes.select.partial.LimitPartial
import gg.ingot.iron.sql.scopes.select.partial.OrderByPartial

/**
 * The scope for the `SELECT FROM GROUP BY HAVING` clause.
 * @author santio
 * @since 2.0
 */
interface HavingSelectScope: Scope, OrderByPartial, LimitPartial
