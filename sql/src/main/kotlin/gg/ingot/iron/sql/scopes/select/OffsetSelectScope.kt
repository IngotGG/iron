package gg.ingot.iron.sql.scopes.select

import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.scopes.select.partial.OrderByPartial

/**
 * The scope for the `SELECT FROM WHERE (LIMIT) OFFSET` clause.
 * @author santio
 * @since 2.0
 */
interface OffsetSelectScope: Scope, OrderByPartial