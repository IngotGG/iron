package gg.ingot.iron.sql.scopes.select.partial

import gg.ingot.iron.sql.scopes.select.LimitSelectScope

/**
 * A partial containing the methods for the `LIMIT` clause.
 * @author santio
 * @since 2.0
 */
interface LimitPartial {

    infix fun limit(limit: Int): LimitSelectScope

}