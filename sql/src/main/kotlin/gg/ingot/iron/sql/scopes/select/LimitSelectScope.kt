package gg.ingot.iron.sql.scopes.select

import gg.ingot.iron.sql.scopes.Scope

/**
 * The scope for the `SELECT FROM WHERE LIMIT` clause.
 * @author santio
 * @since 2.0
 */
interface LimitSelectScope: Scope {

    infix fun offset(offset: Int): OffsetSelectScope

}